package com.elveum.store.internal.builders.paged

import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedKeyedQuerySuspendingBuilder
import com.elveum.store.builders.PagedQuerySuspendingBuilder
import com.elveum.store.contracts.PagedQuerySuspendingContract
import com.elveum.store.internal.builders.keyed.PagedKeyedQuerySuspendingBuilderImpl
import com.elveum.store.internal.stores.PagedKeyedQueryStoreImpl
import com.elveum.store.internal.stores.asPagedQueryStore
import com.elveum.store.stores.paged.PagedList
import com.elveum.store.stores.paged.PagedQueryStore

internal class PagedQuerySuspendingBuilderImpl<Q : Any, P : Any, T : Any>(
    private val initialQuery: Q,
    private val queryDebounceMillis: Long,
    private val config: SharedPageConfig<P, T>,
    sharedBuilder: BasePageBuilderImpl<P, T, PagedQuerySuspendingBuilder<Q, P, T>> = BasePageBuilderImpl(config),
) : PagedQuerySuspendingBuilder<Q, P, T>, BasePagedBuilder<PagedQuerySuspendingBuilder<Q, P, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Key : Any> withKeys(): PagedKeyedQuerySuspendingBuilder<Key, Q, P, T> {
        return PagedKeyedQuerySuspendingBuilderImpl(initialQuery, queryDebounceMillis, config)
    }

    override fun build(contract: PagedQuerySuspendingContract<Q, P, T>): PagedQueryStore<Q, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Q, P) -> PagedList<P, T>,
        onSaveToStorage: suspend (Q, P, PagedList<P, T>) -> Unit,
        onLoadFromStorage: suspend (Q, P) -> PagedList<P, T>?
    ): PagedQueryStore<Q, T> {
        return PagedKeyedQueryStoreImpl<Unit, Q, P, T>(
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = { _, query, pageKey -> onFetch(query, pageKey) },
            loader = { _, query, pageKey -> onLoadFromStorage(query, pageKey) },
            saver = { _, query, pageKey, list -> onSaveToStorage(query, pageKey, list) },
        ).asPagedQueryStore()
    }

}
