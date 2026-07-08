package com.elveum.store.internal.stores

import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.paging.pageLoader
import com.elveum.store.internal.builders.paged.SharedPageConfig
import com.elveum.store.internal.stores.common.CoreEmitter
import com.elveum.store.internal.stores.common.CoreLoaderDelegate
import com.elveum.store.internal.stores.common.CorePageFetcher
import com.elveum.store.internal.stores.common.CoreValueLoaderProvider
import com.elveum.store.load.LoadRequestSource
import com.elveum.store.stores.paged.PagedList
import kotlinx.coroutines.flow.flowOf

internal class PageLoaderProvider<Key : Any, Q : Any, PageKey : Any, T : Any> constructor(
    private val config: SharedPageConfig<PageKey, T>,
    private val fetcher: CorePageFetcher<Key, Q, PageKey, T>,
    private val loader: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>?,
    private val saver: suspend (Key, Q, PageKey, PagedList<PageKey, T>) -> Unit,
) : CoreValueLoaderProvider<Key, Q, List<T>, PagedList<PageKey, T>> {

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
