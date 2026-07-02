package com.elveum.store.internal.stores.common

import com.elveum.container.subject.paging.PageEmitter
import com.elveum.store.stores.paged.PagedList

internal sealed class CorePageFetcher<Key, Q, PageKey, T> {

    class Custom<Key, PageKey, Q, T>(
        val fetcher: suspend PageEmitter<PageKey, T>.(Key, Q, PageKey) -> Unit,
    ) : CorePageFetcher<Key, Q, PageKey, T>()

    class Default<Key, Q, PageKey : Any, T : Any>(
        val fetcher: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>,
    ) : CorePageFetcher<Key, Q, PageKey, T>()

}
