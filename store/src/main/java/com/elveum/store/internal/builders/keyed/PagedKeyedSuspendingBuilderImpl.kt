package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedKeyedQuerySuspendingBuilder
import com.elveum.store.builders.PagedKeyedSuspendingBuilder
import com.elveum.store.contracts.PagedKeyedSuspendingContract
import com.elveum.store.internal.builders.paged.BasePageBuilderImpl
import com.elveum.store.internal.builders.paged.SharedPageConfig
import com.elveum.store.internal.stores.PagedKeyedQueryStoreImpl
import com.elveum.store.stores.keyed.PagedKeyedStore
import com.elveum.store.stores.paged.PagedList

internal class PagedKeyedSuspendingBuilderImpl<Key : Any, PageKey : Any, T : Any>(
    val config: SharedPageConfig<PageKey, T>,
    baseBuilder: BasePageBuilderImpl<PageKey, T, PagedKeyedSuspendingBuilder<Key, PageKey, T>> =
        BasePageBuilderImpl(config),
) : PagedKeyedSuspendingBuilder<Key, PageKey, T>,
    BasePagedBuilder<PagedKeyedSuspendingBuilder<Key, PageKey, T>> by baseBuilder {

    init {
        baseBuilder.setReference(this)
    }

    override fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long
    ): PagedKeyedQuerySuspendingBuilder<Key, Q, PageKey, T> {
        return PagedKeyedQuerySuspendingBuilderImpl(initialQuery, debounceMillis, config)
    }

    override fun build(contract: PagedKeyedSuspendingContract<Key, PageKey, T>): PagedKeyedStore<Key, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Key, PageKey) -> PagedList<PageKey, T>,
        onSaveToStorage: suspend (Key, PageKey, PagedList<PageKey, T>) -> Unit,
        onLoadFromStorage: suspend (Key, PageKey) -> PagedList<PageKey, T>?
    ): PagedKeyedStore<Key, T> {
        return PagedKeyedQueryStoreImpl(
            fetcher = { key, _, pageKey -> onFetch(key, pageKey) },
            loader = { key, _, pageKey -> onLoadFromStorage(key, pageKey) },
            saver = { key, _, pageKey, pagedList -> onSaveToStorage(key, pageKey, pagedList) },
            config = config,
            initialQuery = Unit,
        )
    }
}
