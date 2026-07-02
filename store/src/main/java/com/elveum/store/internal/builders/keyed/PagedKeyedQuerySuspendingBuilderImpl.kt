package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedKeyedQuerySuspendingBuilder
import com.elveum.store.contracts.PagedKeyedQuerySuspendingContract
import com.elveum.store.internal.builders.paged.BasePageBuilderImpl
import com.elveum.store.internal.builders.paged.SharedPageConfig
import com.elveum.store.internal.stores.PagedKeyedQueryStoreImpl
import com.elveum.store.stores.keyed.PagedKeyedQueryStore
import com.elveum.store.stores.paged.PagedList

internal class PagedKeyedQuerySuspendingBuilderImpl<Key : Any, Q : Any, PageKey: Any, T : Any>(
    val initialQuery: Q,
    val queryDebounceMillis: Long,
    val config: SharedPageConfig<PageKey, T>,
    baseBuilder: BasePageBuilderImpl<PageKey, T, PagedKeyedQuerySuspendingBuilder<Key, Q, PageKey, T>> =
        BasePageBuilderImpl(config),
) : PagedKeyedQuerySuspendingBuilder<Key, Q, PageKey, T>,
    BasePagedBuilder<PagedKeyedQuerySuspendingBuilder<Key, Q, PageKey, T>> by baseBuilder {

    init {
        baseBuilder.setReference(this)
    }

    override fun build(
        contract: PagedKeyedQuerySuspendingContract<Key, Q, PageKey, T>,
    ): PagedKeyedQueryStore<Key, Q, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>,
        onSaveToStorage: suspend (Key, Q, PageKey, PagedList<PageKey, T>) -> Unit,
        onLoadFromStorage: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>?
    ): PagedKeyedQueryStore<Key, Q, T> {
        return PagedKeyedQueryStoreImpl(
            fetcher = onFetch,
            loader = onLoadFromStorage,
            saver = onSaveToStorage,
            config = config,
            initialQuery = initialQuery,
            queryDebounceMillis = queryDebounceMillis,
        )
    }

}
