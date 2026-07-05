package com.elveum.store.internal.builders.paged

import com.elveum.container.subject.paging.PageEmitter
import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedExternalQueryBuilder
import com.elveum.store.builders.PagedExternalQuerySuspendingBuilder
import com.elveum.store.builders.PagedKeyedExternalQueryBuilder
import com.elveum.store.contracts.PagedQueryContract
import com.elveum.store.internal.builders.keyed.PagedKeyedExternalQueryBuilderImpl
import com.elveum.store.internal.stores.PagedKeyedQueryStoreImpl
import com.elveum.store.internal.stores.asPagedStore
import com.elveum.store.internal.stores.common.CorePageFetcher
import com.elveum.store.stores.paged.PagedList
import com.elveum.store.stores.paged.PagedStore
import kotlinx.coroutines.flow.Flow

internal class PagedExternalQueryBuilderImpl<Q : Any, P : Any, T : Any>(
    private val initialQueryProvider: (Unit) -> Q,
    private val queryDebounceMillis: Long,
    private val queryFlow: () -> Flow<Q>,
    private val config: SharedPageConfig<P, T>,
    sharedBuilder: BasePageBuilderImpl<P, T, PagedExternalQueryBuilder<Q, P, T>> = BasePageBuilderImpl(config),
) : PagedExternalQueryBuilder<Q, P, T>, BasePagedBuilder<PagedExternalQueryBuilder<Q, P, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    private val externalQueryProvider: (Unit) -> Flow<Q> = { queryFlow() }

    override fun <Key : Any> withKeys(): PagedKeyedExternalQueryBuilder<Key, Q, P, T> {
        return PagedKeyedExternalQueryBuilderImpl(
            initialQueryProvider = { initialQueryProvider(Unit) },
            queryDebounceMillis = queryDebounceMillis,
            queryFlow = { queryFlow() },
            config = config,
        )
    }

    override fun addSuspendingLocalStorage(): PagedExternalQuerySuspendingBuilder<Q, P, T> {
        return PagedExternalQuerySuspendingBuilderImpl(initialQueryProvider, queryDebounceMillis, queryFlow, config)
    }

    override fun build(contract: PagedQueryContract<Q, P, T>): PagedStore<T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (Q, P) -> PagedList<P, T>): PagedStore<T> {
        return PagedKeyedQueryStoreImpl<Unit, Q, P, T>(
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = { _, query, pageKey -> onFetch(query, pageKey) },
            externalQueryProvider = externalQueryProvider,
        ).asPagedStore()
    }

    override fun buildCustom(loader: suspend PageEmitter<P, T>.(Q, P) -> Unit): PagedStore<T> {
        return PagedKeyedQueryStoreImpl<Unit, Q, P, T>(
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = CorePageFetcher.Custom { _, query, pageKey -> loader(query, pageKey) },
            externalQueryProvider = externalQueryProvider,
        ).asPagedStore()
    }
}
