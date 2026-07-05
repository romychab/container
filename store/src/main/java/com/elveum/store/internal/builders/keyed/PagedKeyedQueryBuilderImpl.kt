package com.elveum.store.internal.builders.keyed

import com.elveum.container.subject.paging.PageEmitter
import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedKeyedQueryBuilder
import com.elveum.store.builders.PagedKeyedQuerySuspendingBuilder
import com.elveum.store.contracts.PagedKeyedQueryContract
import com.elveum.store.internal.builders.paged.BasePageBuilderImpl
import com.elveum.store.internal.builders.paged.SharedPageConfig
import com.elveum.store.internal.stores.PagedKeyedQueryStoreImpl
import com.elveum.store.internal.stores.common.CorePageFetcher
import com.elveum.store.stores.keyed.PagedKeyedQueryStore
import com.elveum.store.stores.paged.PagedList

internal class PagedKeyedQueryBuilderImpl<Key : Any, Q : Any, PageKey : Any, T : Any> internal constructor(
    val initialQuery: Q,
    val queryDebounceMillis: Long,
    val config: SharedPageConfig<PageKey, T>,
    baseBuilder: BasePageBuilderImpl<PageKey, T, PagedKeyedQueryBuilder<Key, Q, PageKey, T>> =
        BasePageBuilderImpl(config),
) : PagedKeyedQueryBuilder<Key, Q, PageKey, T>,
    BasePagedBuilder<PagedKeyedQueryBuilder<Key, Q, PageKey, T>> by baseBuilder {

    init {
        baseBuilder.setReference(this)
    }

    override fun addSuspendingLocalStorage(): PagedKeyedQuerySuspendingBuilder<Key, Q, PageKey, T> {
        return PagedKeyedQuerySuspendingBuilderImpl(initialQuery, queryDebounceMillis, config)
    }

    override fun build(contract: PagedKeyedQueryContract<Key, Q, PageKey, T>): PagedKeyedQueryStore<Key, Q, T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>): PagedKeyedQueryStore<Key, Q, T> {
        return PagedKeyedQueryStoreImpl(
            config = config,
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = queryDebounceMillis,
            fetcher = onFetch,
        )
    }

    override fun buildCustom(
        loader: suspend PageEmitter<PageKey, T>.(Key, Q, PageKey) -> Unit,
    ): PagedKeyedQueryStore<Key, Q, T> {
        return PagedKeyedQueryStoreImpl(
            config = config,
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = queryDebounceMillis,
            fetcher = CorePageFetcher.Custom { key, query, pageKey -> loader(key, query, pageKey) },
        )
    }
}
