package com.elveum.store.internal.stores

import com.elveum.container.Emitter
import com.elveum.container.cache.ScopedLazyCache
import com.elveum.container.cache.listenReloadable
import com.elveum.container.cache.reloadAsync
import com.elveum.container.cache.updateIfSuccess
import com.elveum.container.factory.SubjectFactory
import com.elveum.container.getContainerValueOrNull
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.load.LoadRequestSourceMetadata
import com.elveum.store.internal.load.toStoreResult
import com.elveum.store.internal.stores.common.MutexOwner
import com.elveum.store.internal.stores.common.processDataLoad
import com.elveum.store.internal.stores.common.processOptimisticUpdate
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.LoadRequestSource
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex

internal class KeyedStoreImpl<Key : Any, T : Any>(
    private val fetcher: suspend (Key) -> T,
    private val saver: suspend (Key, T) -> Unit = { _, _ -> },
    private val loader: suspend (Key) -> T? = { null },
    private val observer: (Key) -> Flow<T?> = { emptyFlow() },
    config: SharedConfig,
) : KeyedStore<Key, T> {

    private val cache = SubjectFactory
        .createCache<KeyedRequest<Key>, T>(
            cacheTimeoutMillis = config.inMemoryCacheTimeout.inWholeMilliseconds,
            coroutineScopeFactory = config.buildCoroutineScopeFactory(),
            valueLoader = { keyedRequest -> loadValue(keyedRequest) }
        )
        .whenActive { observeReactiveStorageChanges() }

    override fun observe(
        key: Key,
        request: LoadRequest,
    ): Flow<StoreResult<T>> {
        val storedKeyedRequest = getStoredKeyedRequest(key)
        if (storedKeyedRequest?.loadRequest != request || request.requestSource == LoadRequestSource.Fresh) {
            invalidateAsync(key, request)
        }
        return cache
            .listenReloadable(arg = KeyedRequest(key, request))
            .map { it.toStoreResult() }
    }

    override suspend fun invalidate(
        key: Key,
        request: LoadRequest
    ) {
        updateStoredKeyedRequest(key, request)
        cache.reload(
            arg = KeyedRequest(key, request),
            config = request.config,
            metadata = LoadRequestSourceMetadata(request.requestSource),
        ).collect()
    }

    override fun invalidateAsync(key: Key, request: LoadRequest) {
        updateStoredKeyedRequest(key, request)
        cache.reloadAsync(
            arg = KeyedRequest(key, request),
            config = request.config,
            metadata = LoadRequestSourceMetadata(request.requestSource),
        )
    }

    override suspend fun optimisticUpdate(
        key: Key,
        updater: suspend OptimisticUpdateScope<T>.(T) -> Unit
    ) {
        val keyedRequest = KeyedRequest(key)
        findMutexOwner(key)?.apply {
            processOptimisticUpdate(
                getter = { cache.get(keyedRequest).getContainerValueOrNull() },
                setToCache = { cache.updateWith(keyedRequest, it) },
                updater = updater,
            )
        }
    }

    override fun whenActive(block: suspend KeyedStore<Key, T>.() -> Unit): KeyedStore<Key, T> = apply {
        cache.whenActive { block(this@KeyedStoreImpl) }
    }

    private suspend fun Emitter<T>.loadValue(keyedRequest: KeyedRequest<Key>) = processDataLoad(
        query = keyedRequest.key,
        fetcher = fetcher,
        loader = loader,
        saver = saver,
        observer = observer,
        loadRequestSource = keyedRequest.loadRequest.requestSource,
    )

    private suspend fun ScopedLazyCache<KeyedRequest<Key>, T>.observeReactiveStorageChanges() {
        val observedKeys = mutableMapOf<KeyedRequest<Key>, Job>()
        try {
            spyOnArgs().collect { keys ->
                val oldKeys = observedKeys.keys - keys
                oldKeys.forEach { observedKeys.remove(it)?.cancel() }
                val newKeys = keys - observedKeys.keys
                newKeys.forEach { keyedRequest ->
                    val job = observer(keyedRequest.key)
                        .drop(1) // skip initial state
                        .onEach { newValue ->
                            newValue?.let {
                                updateIfSuccess(keyedRequest) { newValue }
                            }
                        }
                        .launchIn(this)
                    observedKeys[keyedRequest] = job
                }
            }
        } finally {
            observedKeys.values.forEach { it.cancel() }
            observedKeys.clear()
        }
    }

    private fun findMutexOwner(key: Key): MutexOwner? {
        return cache.spyOnArgs().value.firstOrNull { it.key == key }
    }

    private fun getStoredKeyedRequest(key: Key): KeyedRequest<Key>? {
        return cache.spyOnArgs().value.firstOrNull { it.key == key }
    }

    private fun updateStoredKeyedRequest(key: Key, loadRequest: LoadRequest) {
        getStoredKeyedRequest(key)?.loadRequest = loadRequest
    }

    private data class KeyedRequest<Key>(
        val key: Key,
        @Volatile var loadRequest: LoadRequest = LoadRequest.Default,
    ) : MutexOwner {

        override val mutex: Mutex by lazy { Mutex() }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as KeyedRequest<*>
            return key == other.key
        }

        override fun hashCode(): Int {
            return key?.hashCode() ?: 0
        }
    }

}
