@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.store.internal.stores

import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.paging.pageLoader
import com.elveum.store.internal.builders.paged.SharedPageConfig
import com.elveum.store.internal.stores.common.CoreEmitter
import com.elveum.store.internal.stores.common.CoreLoaderDelegate
import com.elveum.store.internal.stores.common.CorePageFetcher
import com.elveum.store.internal.stores.common.CoreStore
import com.elveum.store.internal.stores.common.CoreValueLoaderProvider
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.LoadRequestSource
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.keyed.KeyedStore
import com.elveum.store.stores.keyed.PagedKeyedQueryStore
import com.elveum.store.stores.paged.PagedList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

@Suppress("TooManyFunctions")
internal class PagedKeyedQueryStoreImpl<Key : Any, Q : Any, PageKey : Any, T : Any>(
    private val fetcher: CorePageFetcher<Key, Q, PageKey, T>,
    private val loader: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>? = { _, _, _ -> null },
    private val saver: suspend (Key, Q, PageKey, PagedList<PageKey, T>) -> Unit = { _, _, _, _ -> },
    private val config: SharedPageConfig<PageKey, T>,
    private val initialQuery: Q,
    private val queryDebounceMillis: Long = 0,
) : PagedKeyedQueryStore<Key, Q, T> {

    private val coreStore = CoreStore<Key, Q, List<T>, PagedList<PageKey, T>>(
        initialQuery = initialQuery,
        queryDebounceMillis = queryDebounceMillis,
        config = config,
        observer = { _, _ -> flowOf(null) },
        valueLoaderProvider = object : CoreValueLoaderProvider<Key, Q, List<T>, PagedList<PageKey, T>> {
            override fun provideValueLoader(
                key: Key,
                querySource: () -> Q,
                requestSource: LoadRequestSource,
                delegate: CoreLoaderDelegate<PagedList<PageKey, T>>
            ): ValueLoader<List<T>> {
                return pageLoader(
                    initialKey = config.initialKey,
                    itemId = config.itemId,
                    fetchDistance = config.fetchDistance,
                    block = { pageKey ->
                        val query = querySource()
                        when (fetcher) {
                            is CorePageFetcher.Default -> {
                                delegate.processDataLoad(
                                    emitter = CoreEmitter.fromPageEmitter(this),
                                    requestSource = requestSource,
                                    fetcher = { fetcher.fetcher(key, query, pageKey) },
                                    loader = { loader(key, query, pageKey) },
                                    saver = { pageList -> saver(key, query, pageKey, pageList) },
                                    observer = { flowOf(null) },
                                )
                            }
                            is CorePageFetcher.Custom -> fetcher.fetcher(this, key, query, pageKey)
                        }
                    }
                )
            }
        }
    )

    override val activeKeys: StateFlow<Set<Key>> = coreStore.activeKeys

    constructor(
        fetcher: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>,
        loader: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>? = { _, _, _ -> null },
        saver: suspend (Key, Q, PageKey, PagedList<PageKey, T>) -> Unit = { _, _, _, _ -> },
        config: SharedPageConfig<PageKey, T>,
        initialQuery: Q,
        queryDebounceMillis: Long = 0,
    ) : this(
        fetcher = CorePageFetcher.Default(fetcher),
        loader, saver, config, initialQuery, queryDebounceMillis,
    )

    override fun observeQueryFlow(key: Key) = coreStore.observeQueryFlow(key)

    override suspend fun submitQuery(key: Key, query: Q) = coreStore.submitQuery(key, query)

    override fun submitQueryAsync(key: Key, query: Q) = coreStore.submitQueryAsync(key, query)

    override fun get(key: Key): StoreResult<List<T>> = coreStore.get(key)

    override suspend fun invalidate(key: Key) = coreStore.invalidate(key)

    override fun invalidateAsync(key: Key) = coreStore.invalidateAsync(key)

    override fun updateWith(key: Key, storeResult: StoreResult<List<T>>) =
        coreStore.updateWith(key, storeResult)

    override suspend fun optimisticUpdate(
        key: Key,
        updater: suspend OptimisticUpdateScope<List<T>>.(List<T>) -> Unit
    ) = coreStore.optimisticUpdate(key, updater)

    override fun observe(
        key: Key,
        request: LoadRequest?
    ): Flow<StoreResult<List<T>>> = coreStore.observe(key, request)

    override fun whenActive(block: suspend KeyedStore<Key, List<T>>.() -> Unit): KeyedStore<Key, List<T>> {
        coreStore.whenActive { block(this) }
        return this
    }

    override fun onItemRendered(key: Key, index: Int) {
        coreStore.onItemRendered(key, index)
    }
}
