package com.elveum.store.internal.builders.paged

import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedQueryBuilder
import com.elveum.store.builders.PagedQuerySuspendingBuilder
import com.elveum.store.contracts.PagedQueryContract
import com.elveum.store.internal.stores.PagedQueryStoreImpl
import com.elveum.store.stores.paged.PagedList
import com.elveum.store.stores.paged.PagedQueryStore

internal class PagedQueryBuilderImpl<Q : Any, P : Any, T : Any>(
    private val initialQuery: Q,
    private val queryDebounceMillis: Long,
    private val config: SharedPageConfig<P, T>,
    sharedBuilder: BasePageBuilderImpl<P, T, PagedQueryBuilder<Q, P, T>> = BasePageBuilderImpl(config),
) : PagedQueryBuilder<Q, P, T>, BasePagedBuilder<PagedQueryBuilder<Q, P, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun addSuspendingLocalStorage(): PagedQuerySuspendingBuilder<Q, P, T> {
        return PagedQuerySuspendingBuilderImpl(initialQuery, queryDebounceMillis, config)
    }

    override fun build(contract: PagedQueryContract<Q, P, T>): PagedQueryStore<Q, T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (Q, P) -> PagedList<P, T>): PagedQueryStore<Q, T> {
        return PagedQueryStoreImpl(
            initialQuery = initialQuery,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = { query, pageKey -> onFetch(query, pageKey) },
        )
    }
}
