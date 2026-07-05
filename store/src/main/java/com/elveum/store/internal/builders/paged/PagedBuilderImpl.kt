package com.elveum.store.internal.builders.paged

import com.elveum.container.subject.paging.PageEmitter
import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedBuilder
import com.elveum.store.builders.PagedExternalQueryBuilder
import com.elveum.store.builders.PagedKeyedBuilder
import com.elveum.store.builders.PagedQueryBuilder
import com.elveum.store.builders.PagedSuspendingBuilder
import com.elveum.store.contracts.PagedContract
import com.elveum.store.internal.builders.keyed.PagedKeyedBuilderImpl
import com.elveum.store.internal.stores.PagedKeyedQueryStoreImpl
import com.elveum.store.internal.stores.asPagedStore
import com.elveum.store.internal.stores.common.CorePageFetcher
import com.elveum.store.stores.paged.PagedList
import com.elveum.store.stores.paged.PagedStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class PagedBuilderImpl<P : Any, T : Any>(
    initialKey: P,
    itemId: (T) -> Any,
    val sharedBuilder: BasePageBuilderImpl<P, T, PagedBuilder<P, T>> = BasePageBuilderImpl(
        pageConfig = SharedPageConfig(
            initialKey = initialKey,
            itemId = itemId,
        )
    ),
) : PagedBuilder<P, T>, BasePagedBuilder<PagedBuilder<P, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Key : Any> withKeys(): PagedKeyedBuilder<Key, P, T> {
        return PagedKeyedBuilderImpl(sharedBuilder.pageConfig)
    }

    override fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long): PagedQueryBuilder<Q, P, T> {
        return PagedQueryBuilderImpl(initialQuery, debounceMillis, sharedBuilder.pageConfig)
    }

    override fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long,
        queryFlow: () -> Flow<Q>,
    ): PagedExternalQueryBuilder<Q, P, T> {
        return PagedExternalQueryBuilderImpl(
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = debounceMillis,
            queryFlow = queryFlow,
            config = sharedBuilder.pageConfig,
        )
    }

    override fun <Q : Any> withQuery(
        debounceMillis: Long,
        queryFlow: () -> StateFlow<Q>,
    ): PagedExternalQueryBuilder<Q, P, T> {
        return PagedExternalQueryBuilderImpl(
            initialQueryProvider = { queryFlow().value },
            queryDebounceMillis = debounceMillis,
            queryFlow = queryFlow,
            config = sharedBuilder.pageConfig,
        )
    }

    override fun addSuspendingLocalStorage(): PagedSuspendingBuilder<P, T> {
        return PagedSuspendingBuilderImpl(sharedBuilder.pageConfig)
    }

    override fun build(contract: PagedContract<P, T>): PagedStore<T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (P) -> PagedList<P, T>): PagedStore<T> {
        return PagedKeyedQueryStoreImpl<Unit, Unit, P, T>(
            initialQueryProvider = { Unit },
            config = sharedBuilder.pageConfig,
            fetcher = { _, _, pageKey -> onFetch(pageKey) },
        ).asPagedStore()
    }

    override fun buildCustom(loader: suspend PageEmitter<P, T>.(P) -> Unit): PagedStore<T> {
        return PagedKeyedQueryStoreImpl<Unit, Unit, P, T>(
            initialQueryProvider = { Unit },
            config = sharedBuilder.pageConfig,
            fetcher = CorePageFetcher.Custom { _, _, pageKey  -> loader(pageKey) },
        ).asPagedStore()
    }
}
