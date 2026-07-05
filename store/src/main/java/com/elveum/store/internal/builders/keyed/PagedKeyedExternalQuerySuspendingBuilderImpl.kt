package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedKeyedExternalQuerySuspendingBuilder
import com.elveum.store.contracts.PagedKeyedQuerySuspendingContract
import com.elveum.store.internal.builders.paged.BasePageBuilderImpl
import com.elveum.store.internal.builders.paged.SharedPageConfig
import com.elveum.store.internal.stores.PagedKeyedQueryStoreImpl
import com.elveum.store.stores.keyed.PagedKeyedStore
import com.elveum.store.stores.paged.PagedList
import kotlinx.coroutines.flow.Flow

internal class PagedKeyedExternalQuerySuspendingBuilderImpl<Key : Any, Q : Any, PageKey : Any, T : Any>(
    private val initialQueryProvider: (Key) -> Q,
    private val queryDebounceMillis: Long,
    private val queryFlow: (Key) -> Flow<Q>,
    private val config: SharedPageConfig<PageKey, T>,
    baseBuilder: BasePageBuilderImpl<PageKey, T, PagedKeyedExternalQuerySuspendingBuilder<Key, Q, PageKey, T>> =
        BasePageBuilderImpl(config),
) : PagedKeyedExternalQuerySuspendingBuilder<Key, Q, PageKey, T>,
    BasePagedBuilder<PagedKeyedExternalQuerySuspendingBuilder<Key, Q, PageKey, T>> by baseBuilder {

    init {
        baseBuilder.setReference(this)
    }

    override fun build(
        contract: PagedKeyedQuerySuspendingContract<Key, Q, PageKey, T>,
    ): PagedKeyedStore<Key, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>,
        onSaveToStorage: suspend (Key, Q, PageKey, PagedList<PageKey, T>) -> Unit,
        onLoadFromStorage: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>?,
    ): PagedKeyedStore<Key, T> {
        return PagedKeyedQueryStoreImpl(
            fetcher = onFetch,
            loader = onLoadFromStorage,
            saver = onSaveToStorage,
            config = config,
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            externalQueryProvider = queryFlow,
        )
    }
}
