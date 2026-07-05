package com.elveum.store.internal.builders.keyed

import com.elveum.container.subject.paging.PageEmitter
import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedKeyedBuilder
import com.elveum.store.builders.PagedKeyedExternalQueryBuilder
import com.elveum.store.builders.PagedKeyedQueryBuilder
import com.elveum.store.builders.PagedKeyedSuspendingBuilder
import com.elveum.store.contracts.PagedKeyedContract
import com.elveum.store.internal.builders.paged.BasePageBuilderImpl
import com.elveum.store.internal.builders.paged.SharedPageConfig
import com.elveum.store.internal.stores.PagedKeyedQueryStoreImpl
import com.elveum.store.internal.stores.common.CorePageFetcher
import com.elveum.store.stores.keyed.PagedKeyedStore
import com.elveum.store.stores.paged.PagedList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class PagedKeyedBuilderImpl<Key : Any, PageKey : Any, T : Any>(
    val config: SharedPageConfig<PageKey, T>,
    baseBuilder: BasePageBuilderImpl<PageKey, T, PagedKeyedBuilder<Key, PageKey, T>> = BasePageBuilderImpl(config)
) : PagedKeyedBuilder<Key, PageKey, T>,
    BasePagedBuilder<PagedKeyedBuilder<Key, PageKey, T>> by baseBuilder {

    init {
        baseBuilder.setReference(this)
    }

    override fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long
    ): PagedKeyedQueryBuilder<Key, Q, PageKey, T> {
        return PagedKeyedQueryBuilderImpl(initialQuery, debounceMillis, config)
    }

    override fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long,
        queryFlow: (Key) -> Flow<Q>,
    ): PagedKeyedExternalQueryBuilder<Key, Q, PageKey, T> {
        return PagedKeyedExternalQueryBuilderImpl(
            initialQueryProvider = { initialQuery },
            queryDebounceMillis = debounceMillis,
            queryFlow = queryFlow,
            config = config,
        )
    }

    override fun <Q : Any> withQuery(
        debounceMillis: Long,
        queryFlow: (Key) -> StateFlow<Q>,
    ): PagedKeyedExternalQueryBuilder<Key, Q, PageKey, T> {
        return PagedKeyedExternalQueryBuilderImpl(
            initialQueryProvider = { key -> queryFlow(key).value },
            queryDebounceMillis = debounceMillis,
            queryFlow = queryFlow,
            config = config,
        )
    }

    override fun addSuspendingLocalStorage(): PagedKeyedSuspendingBuilder<Key, PageKey, T> {
        return PagedKeyedSuspendingBuilderImpl(config)
    }

    override fun build(contract: PagedKeyedContract<Key, PageKey, T>): PagedKeyedStore<Key, T> {
        return build(onFetch = contract::fetch)
    }

    override fun build(onFetch: suspend (Key, PageKey) -> PagedList<PageKey, T>): PagedKeyedStore<Key, T> {
        return PagedKeyedQueryStoreImpl(
            config = config,
            initialQueryProvider = { Unit },
            fetcher = { key, _, pageKey -> onFetch(key, pageKey) },
        )
    }

    override fun buildCustom(loader: suspend PageEmitter<PageKey, T>.(Key, PageKey) -> Unit): PagedKeyedStore<Key, T> {
        return PagedKeyedQueryStoreImpl(
            config = config,
            initialQueryProvider = { Unit },
            fetcher = CorePageFetcher.Custom { key, _, pageKey -> loader(key, pageKey) },
        )
    }
}
