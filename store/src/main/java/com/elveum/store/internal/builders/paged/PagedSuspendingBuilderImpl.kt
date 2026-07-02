package com.elveum.store.internal.builders.paged

import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedKeyedSuspendingBuilder
import com.elveum.store.builders.PagedQuerySuspendingBuilder
import com.elveum.store.builders.PagedSuspendingBuilder
import com.elveum.store.contracts.PagedSuspendingContract
import com.elveum.store.internal.builders.keyed.PagedKeyedSuspendingBuilderImpl
import com.elveum.store.internal.stores.PagedKeyedQueryStoreImpl
import com.elveum.store.internal.stores.asPagedStore
import com.elveum.store.stores.paged.PagedList
import com.elveum.store.stores.paged.PagedStore

internal class PagedSuspendingBuilderImpl<P : Any, T : Any>(
    private val config: SharedPageConfig<P, T>,
    sharedBuilder: BasePageBuilderImpl<P, T, PagedSuspendingBuilder<P, T>> = BasePageBuilderImpl(config)
) : PagedSuspendingBuilder<P, T>, BasePagedBuilder<PagedSuspendingBuilder<P, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long): PagedQuerySuspendingBuilder<Q, P, T> {
        return PagedQuerySuspendingBuilderImpl(initialQuery, debounceMillis, config)
    }

    override fun <Key : Any> withKeys(): PagedKeyedSuspendingBuilder<Key, P, T> {
        return PagedKeyedSuspendingBuilderImpl(config)
    }

    override fun build(contract: PagedSuspendingContract<P, T>): PagedStore<T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (P) -> PagedList<P, T>,
        onSaveToStorage: suspend (P, PagedList<P, T>) -> Unit,
        onLoadFromStorage: suspend (P) -> PagedList<P, T>?
    ): PagedStore<T> {
        return PagedKeyedQueryStoreImpl<Unit, Unit, P, T>(
            initialQuery = Unit,
            queryDebounceMillis = 0,
            config = config,
            fetcher = { _, _, pageKey -> onFetch(pageKey) },
            saver = { _, _, pageKey, page -> onSaveToStorage(pageKey, page) },
            loader = { _, _, pageKey -> onLoadFromStorage(pageKey) }
        ).asPagedStore()
    }
}
