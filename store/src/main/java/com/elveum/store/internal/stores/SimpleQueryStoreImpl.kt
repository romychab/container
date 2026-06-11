@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.elveum.store.internal.stores

import com.elveum.container.Emitter
import com.elveum.container.factory.SubjectFactory
import com.elveum.container.getContainerValueOrNull
import com.elveum.container.subject.ScopedLazyFlowSubject
import com.elveum.container.subject.listenReloadable
import com.elveum.container.subject.reloadAsync
import com.elveum.container.subject.updateIfSuccess
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.load.LoadRequestSourceMetadata
import com.elveum.store.internal.load.toStoreResult
import com.elveum.store.internal.stores.common.MutexOwner
import com.elveum.store.internal.stores.common.QueryHandler
import com.elveum.store.internal.stores.common.processDataLoad
import com.elveum.store.internal.stores.common.processOptimisticUpdate
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.simple.SimpleQueryStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

internal class SimpleQueryStoreImpl<Q : Any, T : Any>(
    initialQuery: Q,
    private val fetcher: suspend (Q) -> T,
    private val saver: suspend (Q, T) -> Unit = { _, _ -> },
    private val loader: suspend (Q) -> T? = { null },
    private val observer: (Q) -> Flow<T?> = { emptyFlow() },
    queryDebounceMillis: Long = 0,
    private val config: SharedConfig,
) : SimpleQueryStore<Q, T>, MutexOwner {

    private val queryHandler = QueryHandler(initialQuery, queryDebounceMillis, this)

    override val queryFlow = queryHandler.queryFlow
    override val mutex = Mutex()

    private val cache = SubjectFactory
        .createSubject(
            cacheTimeoutMillis = config.inMemoryCacheTimeout.inWholeMilliseconds,
            coroutineScopeFactory = config.buildCoroutineScopeFactory(),
            valueLoader = { loadValue() }
        )
        .whenActive {
            launch { observeReactiveStorageChanges() }
            launch { queryHandler.observeAsyncQueryRequests() }
        }

    override fun observe(request: LoadRequest): Flow<StoreResult<T>> {
        queryHandler.handleObserveRequest(request)
        return cache
            .listenReloadable()
            .map { it.toStoreResult() }
    }

    override suspend fun invalidate(request: LoadRequest) {
        cache.reload(
            config = request.config,
            metadata = LoadRequestSourceMetadata(request.requestSource),
        ).collect()
    }

    override fun invalidateAsync(request: LoadRequest) {
        cache.reloadAsync(
            config = request.config,
            metadata = LoadRequestSourceMetadata(request.requestSource),
        )
    }

    override suspend fun submitQuery(query: Q, loadRequest: LoadRequest) {
        queryHandler.submitQuery(query, loadRequest)
    }

    override fun submitQueryAsync(query: Q, loadRequest: LoadRequest) {
        queryHandler.submitQueryAsync(query, loadRequest)
    }

    override suspend fun optimisticUpdate(updater: suspend OptimisticUpdateScope<T>.(T) -> Unit) {
        processOptimisticUpdate(
            getter = { cache.currentValue().getContainerValueOrNull() },
            setToCache = cache::updateWith,
            updater = updater,
        )
    }

    override fun whenActive(block: suspend SimpleQueryStore<Q, T>.() -> Unit): SimpleQueryStore<Q, T> = apply {
        cache.whenActive {
            block(this@SimpleQueryStoreImpl)
        }
    }

    private suspend fun Emitter<T>.loadValue() = processDataLoad(
        query = queryFlow.value,
        fetcher = fetcher,
        loader = loader,
        saver = saver,
        observer = observer,
    )

    private fun ScopedLazyFlowSubject<T>.observeReactiveStorageChanges() {
        queryFlow
            .flatMapLatest { observer(it).drop(1) }
            .onEach { newValue ->
                newValue?.let {
                    updateIfSuccess { newValue }
                }
            }
            .launchIn(this)
    }

}

internal fun <T : Any> SimpleQueryStore<Unit, T>.asSimpleStore() =
    SimpleStoreImpl(this)
