@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.store.internal.stores

import com.elveum.container.subject.ValueLoader
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.common.CoreEmitter
import com.elveum.store.internal.stores.common.CoreFetcher
import com.elveum.store.internal.stores.common.CoreStore
import com.elveum.store.internal.stores.common.CoreLoaderDelegate
import com.elveum.store.internal.stores.common.CoreValueLoaderProvider
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.LoadRequestSource
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.keyed.KeyedQueryStore
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

internal class KeyedQueryStoreImpl<Key : Any, Q : Any, T : Any>(
    private val fetcher: CoreFetcher<Key, Q, T>,
    private val loader: suspend (Key, Q) -> T? = { _, _ -> null },
    private val saver: suspend (Key, Q, T) -> Unit = { _, _, _ -> },
    private val observer: (Key, Q) -> Flow<T?> = { _, _ -> flowOf(null) },
    private val config: SharedConfig,
    private val initialQueryProvider: (Key) -> Q,
    private val queryDebounceMillis: Long = 0,
    private val externalQueryProvider: ((Key) -> Flow<Q>)? = null,
) : KeyedQueryStore<Key, Q, T> {

    private val coreStore = CoreStore(
        initialQueryProvider = initialQueryProvider,
        queryDebounceMillis = queryDebounceMillis,
        config = config,
        observer = observer,
        externalQueryProvider = externalQueryProvider,
        valueLoaderProvider = object : CoreValueLoaderProvider<Key, Q, T, T> {
            override fun provideValueLoader(
                key: Key,
                querySource: () -> Q,
                requestSource: LoadRequestSource,
                delegate: CoreLoaderDelegate<T>
            ): ValueLoader<T> {
                return ValueLoader {
                    val query = querySource()
                    when (fetcher) {
                        is CoreFetcher.Default -> {
                            delegate.processDataLoad(
                                emitter = CoreEmitter.fromEmitter(this),
                                requestSource = requestSource,
                                fetcher = { fetcher.fetcher(key, query) },
                                loader = { loader(key, query) },
                                observer = { observer(key, query) },
                                saver = { saver(key, query, it) },
                            )
                        }
                        is CoreFetcher.Custom -> fetcher.fetcher(this, key, query)
                    }
                }
            }
        }
    )

    override val activeKeys: StateFlow<Set<Key>> = coreStore.activeKeys

    constructor(
        fetcher: suspend (Key, Q) -> T,
        loader: suspend (Key, Q) -> T? = { _, _ -> null },
        saver: suspend (Key, Q, T) -> Unit = { _, _, _ -> },
        observer: (Key, Q) -> Flow<T?> = { _, _ -> flowOf(null) },
        config: SharedConfig,
        initialQueryProvider: (Key) -> Q,
        queryDebounceMillis: Long = 0,
        externalQueryProvider: ((Key) -> Flow<Q>)? = null,
    ) : this(
        fetcher = CoreFetcher.Default(fetcher),
        loader, saver, observer, config, initialQueryProvider, queryDebounceMillis, externalQueryProvider,
    )

    override fun observeQueryFlow(key: Key) = coreStore.observeQueryFlow(key)

    override suspend fun submitQuery(key: Key, query: Q) = coreStore.submitQuery(key, query)

    override fun submitQueryAsync(key: Key, query: Q) = coreStore.submitQueryAsync(key, query)

    override fun get(key: Key): StoreResult<T> = coreStore.get(key)

    override suspend fun invalidate(key: Key) = coreStore.invalidate(key)

    override fun invalidateAsync(key: Key) = coreStore.invalidateAsync(key)

    override fun updateWith(key: Key, storeResult: StoreResult<T>) =
        coreStore.updateWith(key, storeResult)

    override suspend fun optimisticUpdate(
        key: Key,
        updater: suspend OptimisticUpdateScope<T>.(T) -> Unit
    ) = coreStore.optimisticUpdate(key, updater)

    override fun observe(
        key: Key,
        request: LoadRequest?
    ): Flow<StoreResult<T>> = coreStore.observe(key, request)

    override fun whenActive(block: suspend KeyedStore<Key, T>.() -> Unit): KeyedStore<Key, T> {
        coreStore.whenActive { block(this) }
        return this
    }

}
