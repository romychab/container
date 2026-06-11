package com.elveum.store.internal.builders.paged

import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedBuilder
import com.elveum.store.builders.PagedQueryBuilder
import com.elveum.store.builders.PagedSuspendingBuilder
import com.elveum.store.contracts.PagedContract
import com.elveum.store.internal.stores.PagedQueryStoreImpl
import com.elveum.store.internal.stores.asPagedStore
import com.elveum.store.stores.paged.PagedList
import com.elveum.store.stores.paged.PagedStore

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

    override fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long): PagedQueryBuilder<Q, P, T> {
        return PagedQueryBuilderImpl(initialQuery, debounceMillis, sharedBuilder.pageConfig)
    }

    override fun addSuspendingLocalStorage(): PagedSuspendingBuilder<P, T> {
        return PagedSuspendingBuilderImpl(sharedBuilder.pageConfig)
    }

    override fun build(contract: PagedContract<P, T>): PagedStore<T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (P) -> PagedList<P, T>): PagedStore<T> {
        return PagedQueryStoreImpl(
            initialQuery = Unit,
            queryDebounceMillis = 0,
            config = sharedBuilder.pageConfig,
            fetcher = { _, pageKey -> onFetch(pageKey) },
        ).asPagedStore()
    }
}
