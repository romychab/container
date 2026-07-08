@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.elveum.store.internal.stores.common

import com.elveum.container.cache.LazyCache
import com.elveum.container.cache.listenReloadable
import com.elveum.container.cache.reloadAsync
import com.elveum.container.cache.updateIfSuccess
import com.elveum.container.factory.SubjectFactory
import com.elveum.container.getContainerValueOrNull
import com.elveum.container.isError
import com.elveum.container.isSuccess
import com.elveum.container.pendingContainer
import com.elveum.container.stateMap
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.load.metadata
import com.elveum.store.internal.processItems
import com.elveum.store.internal.stores.common.KeyRecord
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.LoadRequestSource
import com.elveum.store.load.StoreResult
import com.elveum.store.load.isForegroundLoading
import com.elveum.store.load.onItemRendered
import com.elveum.store.load.toContainer
import com.elveum.store.load.toStoreResult
import com.elveum.store.stores.base.OptimisticUpdateScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("TooManyFunctions")
internal class CoreStore<Key : Any, Q : Any, T : Any, R : Any>(
    private val initialQueryProvider: (Key) -> Q,
    private val queryDebounceMillis: Long,
    private val config: SharedConfig,
    private val observer: (Key, Q) -> Flow<T?>,
    private val valueLoaderProvider: CoreValueLoaderProvider<Key, Q, T, R>,
    private val externalQueryProvider: ((Key) -> Flow<Q>)? = null,
) {

    private val gates = ConcurrentHashMap<Key, AtomicBoolean>()
    private val currentQueriesFlow = MutableStateFlow(mapOf<Key, Q>())
    private val asyncRequests = ConcurrentHashMap<Key, MutableStateFlow<Q>>()

    private val cache = SubjectFactory
        .createCacheFromFactory<KeyRecord<Key>, T>(
            cacheTimeoutMillis = config.inMemoryCacheTimeout.inWholeMilliseconds,
            coroutineScopeFactory = config.buildCoroutineScopeFactory(),
            factory = { arg, coroutineScopeFactory, cacheTimeoutMillis ->
                LazyFlowSubject.create(
                    cacheTimeoutMillis = cacheTimeoutMillis,
                    coroutineScopeFactory = coroutineScopeFactory,
                    loadConfig = arg.loadRequest.config,
                    metadata = arg.loadRequest.metadata,
                    valueLoader = valueLoaderProvider.provideValueLoader(
                        key = arg.key,
                        querySource = { observeQueryFlow(arg.key).value },
                        requestSource = arg.loadRequest.requestSource,
                        delegate = createDelegate(),
                    ),
                )
            }
        )
        .whenActive { handleMutexes() }
        .whenActive { observeLocalChanges() }
        .whenActive { observeAsyncQueryRequests() }

    val activeKeys: StateFlow<Set<Key>> = cache.spyOnArgs()
        .stateMap { keys -> keys.map { it.key }.toSet() }

    suspend fun submitQuery(key: Key, query: Q) {
        updateQuery(key, query)
        delay(queryDebounceMillis)
        val queriesAfterDelay = currentQueriesFlow.value
        if (queriesAfterDelay[key] == query) {
            invalidate(key)
        }
    }

    fun get(key: Key): StoreResult<T> {
        val allRecords = getKeyRecords(key)
        val allResults = allRecords.map { cache.get(it) }
        val container = allResults
            // 1. loaded - highest priority
            .firstOrNull { it.isSuccess() }
            // 2. errors - high priority
            ?: allResults.firstOrNull { it.isError() }
            // 3. loading in progress
            ?: allResults.firstOrNull()
            ?: pendingContainer()
        return container.toStoreResult()
    }

    fun submitQueryAsync(key: Key, query: Q) {
        updateQuery(key, query)
        asyncRequests[key]?.value = query
    }

    suspend fun observeAsyncQueryRequests() {
        val keysFlow = cache.spyOnArgs()
            .map { records -> records.map { it.key }.toSet() }
            .distinctUntilChanged()
        processItems(
            flow = keysFlow,
            onAdded = { key ->
                currentQueriesFlow.update { oldMap -> oldMap + (key to initialQueryProvider(key)) }
                asyncRequests[key] = MutableStateFlow(initialQueryProvider(key))
            },
            onRemoved = { key ->
                currentQueriesFlow.update { oldMap -> oldMap - key }
                asyncRequests.remove(key)
            }
        ) { key ->
            coroutineScope {
                externalQueryProvider?.let { provider ->
                    launch {
                        // Feed each external emission into the same async-query pipeline used by
                        // submitQueryAsync. asyncRequests[key] is a StateFlow, so an emission equal
                        // to the current query is de-duplicated and does not trigger a reload -
                        // this prevents a double initial load when the flow's first value equals
                        // the seed.
                        provider(key).collect { query -> submitQueryAsync(key, query) }
                    }
                }
                asyncRequests[key]
                    // drop the StateFlow's initial replayed value, otherwise the key would be
                    // reloaded immediately on activation - right after observe() already started
                    // the initial load (mirrors observeLocalChanges below).
                    ?.drop(1)
                    ?.debounce(queryDebounceMillis)
                    ?.collect {
                        invalidateAsync(key)
                    }
            }
            awaitCancellation()
        }
    }

    suspend fun invalidate(key: Key) {
        coroutineScope {
            getKeyRecords(key).forEach {
                launch {
                    cache
                        .reload(it, it.loadRequest.config, it.loadRequest.metadata)
                        .collect()
                }
            }
        }
    }

    fun invalidateAsync(key: Key) {
        getKeyRecords(key).forEach {
            cache.reloadAsync(it, it.loadRequest.config, it.loadRequest.metadata)
        }
    }

    suspend fun optimisticUpdate(
        key: Key,
        updater: suspend OptimisticUpdateScope<T>.(T) -> Unit
    ) {
        gates[key]?.let { gate ->
            if (gate.compareAndSet(false, true)) {
                try {
                    processOptimisticUpdate(
                        getter = { get(key).toContainer().getContainerValueOrNull() },
                        setToCache = { updateWith(key, it.toStoreResult()) },
                        updater = updater,
                    )
                } finally {
                    gate.set(false)
                }
            }
        }
    }

    fun observe(
        key: Key,
        request: LoadRequest?
    ): Flow<StoreResult<T>> {
        val loadRequestFlow = request?.let(::flowOf) ?: config.loadRequestFlow
        return loadRequestFlow
            .flatMapLatest { loadRequest ->
                val record = KeyRecord(key, loadRequest)
                cache.listenReloadable(record)
                    .map { loadRequest to it.toStoreResult() }
                    .onStart {
                        // A Fresh request must ignore any cached value and re-fetch on every
                        // subscription. Without this, re-observing within the cache-timeout
                        // window would just replay the record's cached value. On the very first
                        // subscription the record does not exist yet, so this reload is a no-op
                        // and the initial load performs the single fetch.
                        if (loadRequest.requestSource == LoadRequestSource.Fresh) {
                            cache.reloadAsync(record, loadRequest.config, loadRequest.metadata)
                        }
                    }
            }
            .scan(PreparedResult<T>()) { acc, (currentLoadRequest, currentResult) ->
                if (acc.loadRequest == currentLoadRequest) {
                    acc.copy(result = currentResult)
                } else if (currentResult.isForegroundLoading()) {
                    // load request has been updated - keep previous result until error/success received:
                    acc.copy(loadRequest = currentLoadRequest)
                } else {
                    PreparedResult(currentResult, currentLoadRequest)
                }
            }
            .map { it.result }
            .distinctUntilChanged()
    }

    fun updateWith(key: Key, storeResult: StoreResult<T>) {
        getKeyRecords(key).forEach {
            cache.updateWith(it, storeResult.toContainer())
        }
    }

    fun observeQueryFlow(key: Key): StateFlow<Q> {
        return currentQueriesFlow.stateMap { it[key] ?: initialQueryProvider(key) }
    }

    fun whenActive(block: suspend () -> Unit) {
        cache.whenActive { block() }
    }

    fun onItemRendered(key: Key, index: Int) {
        get(key).onItemRendered(index)
    }

    private fun updateQuery(key: Key, query: Q) {
        val queries = currentQueriesFlow.value
        if (!queries.contains(key)) return
        currentQueriesFlow.update { oldMap ->
            if (oldMap.contains(key)) {
                oldMap + (key to query)
            } else {
                oldMap
            }
        }
    }

    private suspend fun LazyCache<KeyRecord<Key>, *>.handleMutexes() {
        processItems(
            flow = observeAllKeys(),
            onAdded = { key -> gates[key] = AtomicBoolean() },
            onRemoved = { key -> gates.remove(key) }
        )
    }

    private fun <R : Any> createDelegate(): CoreLoaderDelegate<R> {
        return DefaultCoreLoaderDelegate()
    }

    private fun getKeyRecords(key: Key): List<KeyRecord<Key>> {
        return cache.spyOnArgs().value.filter { it.key == key }
    }

    private fun LazyCache<KeyRecord<Key>, *>.observeAllKeys(): Flow<Set<Key>> {
        return spyOnArgs()
            .map { records -> records.map { it.key }.toSet() }
            .distinctUntilChanged()
    }

    private suspend fun observeLocalChanges() {
        val keyedQueries = currentQueriesFlow
            .map { it.entries.map { (key, query) -> KeyedQuery(key, query) }.toSet() }
            .distinctUntilChanged()
        processItems(
            flow = keyedQueries,
            job = { keyedQuery ->
                observer(keyedQuery.key, keyedQuery.query)
                    .drop(1)
                    .collect { item ->
                        if (item != null) {
                            // Patch the cache only when it currently holds a loaded value, so a
                            // reactive local-storage change never clobbers an in-progress Loading
                            // or Failed state (and the existing source metadata is preserved).
                            updateIfSuccess(keyedQuery.key) { item }
                        }
                    }
            }
        )
    }

    private fun updateIfSuccess(key: Key, updater: (T) -> T) {
        getKeyRecords(key).forEach {
            cache.updateIfSuccess(it, updater = updater)
        }
    }

    private data class KeyedQuery<Key, Q>(
        val key: Key,
        val query: Q,
    )

    private data class PreparedResult<T>(
        val result: StoreResult<T> = StoreResult.Loading,
        val loadRequest: LoadRequest? = null,
    )
}
