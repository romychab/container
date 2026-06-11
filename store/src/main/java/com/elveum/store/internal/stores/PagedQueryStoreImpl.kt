@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.store.internal.stores

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.Emitter
import com.elveum.container.factory.SubjectFactory
import com.elveum.container.getContainerValueOrNull
import com.elveum.container.subject.listenReloadable
import com.elveum.container.subject.paging.PageEmitter
import com.elveum.container.subject.paging.pageLoader
import com.elveum.container.subject.reloadAsync
import com.elveum.store.internal.builders.paged.SharedPageConfig
import com.elveum.store.internal.load.LoadRequestSourceMetadata
import com.elveum.store.internal.load.toStoreResult
import com.elveum.store.internal.stores.common.MutexOwner
import com.elveum.store.internal.stores.common.QueryHandler
import com.elveum.store.internal.stores.common.processDataLoad
import com.elveum.store.internal.stores.common.processOptimisticUpdate
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.paged.PagedList
import com.elveum.store.stores.paged.PagedQueryStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex

internal class PagedQueryStoreImpl<Q : Any, P : Any, T : Any>(
    private val initialQuery: Q,
    private val queryDebounceMillis: Long,
    private val fetcher: suspend (Q, P) -> PagedList<P, T>,
    private val saver: suspend (Q, P, PagedList<P, T>) -> Unit = { _, _, _ -> },
    private val loader: suspend (Q, P) -> PagedList<P, T>? = { _, _ -> null },
    config: SharedPageConfig<P, T>,
) : PagedQueryStore<Q, T>, MutexOwner {

    private val queryHandler = QueryHandler(initialQuery, queryDebounceMillis, this)

    override val queryFlow = queryHandler.queryFlow
    override val mutex = Mutex()

    private val pageLoader = pageLoader(
        initialKey = config.initialKey,
        itemId = config.itemId,
        fetchDistance = config.fetchDistance,
        block = { pageKey -> loadPage(pageKey) }
    )

    private val cache = SubjectFactory
        .createSubject<List<T>>(
            cacheTimeoutMillis = config.inMemoryCacheTimeout.inWholeMilliseconds,
            coroutineScopeFactory = config.buildCoroutineScopeFactory(),
            valueLoader = pageLoader,
        )
        .whenActive { queryHandler.observeAsyncQueryRequests() }

    override fun observe(
        request: LoadRequest
    ): Flow<StoreResult<List<T>>> {
        queryHandler.handleObserveRequest(request)
        return cache
            .listenReloadable()
            .map { it.toStoreResult() }
    }

    override suspend fun invalidate(
        request: LoadRequest
    ) {
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

    override suspend fun optimisticUpdate(
        updater: suspend OptimisticUpdateScope<List<T>>.(List<T>) -> Unit
    ) {
        processOptimisticUpdate(
            getter = { cache.currentValue().getContainerValueOrNull() },
            setToCache = cache::updateWith,
            updater = updater,
        )
    }

    override suspend fun submitQuery(query: Q, loadRequest: LoadRequest) {
        queryHandler.submitQuery(query, loadRequest)
    }

    override fun submitQueryAsync(query: Q, loadRequest: LoadRequest) {
        queryHandler.submitQueryAsync(query, loadRequest)

    }

    override fun onItemRendered(index: Int) {
        pageLoader.onItemRendered(index)
    }

    override fun whenActive(
        block: suspend PagedQueryStore<Q, T>.() -> Unit,
    ): PagedQueryStore<Q, T> = apply {
        cache.whenActive {
            block(this@PagedQueryStoreImpl)
        }
    }

    private suspend fun PageEmitter<P, T>.loadPage(pageKey: P) {
        val adapter = PageEmitterAdapter(this)
        val args = queryFlow.value to pageKey
        adapter.processDataLoad(
            query = args,
            fetcher = { (query, pageKey) -> fetcher(query, pageKey) },
            loader = { (query, pageKey) -> loader(query, pageKey) },
            saver = { (query, pageKey), pagedList -> saver(query, pageKey, pagedList) },
            observer = { emptyFlow() },
        )
    }

    private inner class PageEmitterAdapter(
        val pageEmitter: PageEmitter<P, T>
    ) : Emitter<PagedList<P, T>> {

        override val metadata: ContainerMetadata = pageEmitter.metadata

        override suspend fun emit(
            value: PagedList<P, T>,
            metadata: ContainerMetadata,
            isLastValue: Boolean
        ) {
            pageEmitter.emitPage(value.items)
            if (value.nextKey != null) pageEmitter.emitNextKey(value.nextKey)
        }

        override suspend fun <R> dependsOnFlow(
            key: Any,
            vararg keys: Any,
            flow: () -> Flow<R>
        ): R = pageEmitter.dependsOnFlow(key, *keys, flow = flow)

        override suspend fun <R> dependsOnContainerFlow(
            key: Any,
            vararg keys: Any,
            flow: () -> Flow<Container<R>>
        ): R = pageEmitter.dependsOnContainerFlow(key, *keys, flow = flow)
    }
}

internal fun <T : Any> PagedQueryStore<Unit, T>.asPagedStore() =
    PagedStoreImpl(this)
