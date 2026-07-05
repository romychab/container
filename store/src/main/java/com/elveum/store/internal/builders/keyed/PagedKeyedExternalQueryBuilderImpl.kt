package com.elveum.store.internal.builders.keyed

import com.elveum.container.subject.paging.PageEmitter
import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedKeyedExternalQueryBuilder
import com.elveum.store.builders.PagedKeyedExternalQuerySuspendingBuilder
import com.elveum.store.contracts.PagedKeyedQueryContract
import com.elveum.store.internal.builders.paged.BasePageBuilderImpl
import com.elveum.store.internal.builders.paged.SharedPageConfig
import com.elveum.store.internal.stores.PagedKeyedQueryStoreImpl
import com.elveum.store.internal.stores.common.CorePageFetcher
import com.elveum.store.stores.keyed.PagedKeyedStore
import com.elveum.store.stores.paged.PagedList
import kotlinx.coroutines.flow.Flow

internal class PagedKeyedExternalQueryBuilderImpl<Key : Any, Q : Any, PageKey : Any, T : Any>(
    private val initialQueryProvider: (Key) -> Q,
    private val queryDebounceMillis: Long,
    private val queryFlow: (Key) -> Flow<Q>,
    private val config: SharedPageConfig<PageKey, T>,
    baseBuilder: BasePageBuilderImpl<PageKey, T, PagedKeyedExternalQueryBuilder<Key, Q, PageKey, T>> =
        BasePageBuilderImpl(config),
) : PagedKeyedExternalQueryBuilder<Key, Q, PageKey, T>,
    BasePagedBuilder<PagedKeyedExternalQueryBuilder<Key, Q, PageKey, T>> by baseBuilder {

    init {
        baseBuilder.setReference(this)
    }

    override fun addSuspendingLocalStorage(): PagedKeyedExternalQuerySuspendingBuilder<Key, Q, PageKey, T> {
        return PagedKeyedExternalQuerySuspendingBuilderImpl(
            initialQueryProvider, queryDebounceMillis, queryFlow, config,
        )
    }

    override fun build(contract: PagedKeyedQueryContract<Key, Q, PageKey, T>): PagedKeyedStore<Key, T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>): PagedKeyedStore<Key, T> {
        return PagedKeyedQueryStoreImpl(
            config = config,
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            fetcher = onFetch,
            externalQueryProvider = queryFlow,
        )
    }

    override fun buildCustom(
        loader: suspend PageEmitter<PageKey, T>.(Key, Q, PageKey) -> Unit,
    ): PagedKeyedStore<Key, T> {
        return PagedKeyedQueryStoreImpl(
            config = config,
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            fetcher = CorePageFetcher.Custom { key, query, pageKey -> loader(key, query, pageKey) },
            externalQueryProvider = queryFlow,
        )
    }
}
