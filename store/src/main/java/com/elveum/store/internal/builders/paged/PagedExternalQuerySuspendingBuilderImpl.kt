package com.elveum.store.internal.builders.paged

import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedExternalQuerySuspendingBuilder
import com.elveum.store.builders.PagedKeyedExternalQuerySuspendingBuilder
import com.elveum.store.contracts.PagedQuerySuspendingContract
import com.elveum.store.internal.builders.keyed.PagedKeyedExternalQuerySuspendingBuilderImpl
import com.elveum.store.internal.stores.PagedKeyedQueryStoreImpl
import com.elveum.store.internal.stores.asPagedStore
import com.elveum.store.stores.paged.PagedList
import com.elveum.store.stores.paged.PagedStore
import kotlinx.coroutines.flow.Flow

internal class PagedExternalQuerySuspendingBuilderImpl<Q : Any, P : Any, T : Any>(
    private val initialQueryProvider: (Unit) -> Q,
    private val queryDebounceMillis: Long,
    private val queryFlow: () -> Flow<Q>,
    private val config: SharedPageConfig<P, T>,
    sharedBuilder: BasePageBuilderImpl<P, T, PagedExternalQuerySuspendingBuilder<Q, P, T>> =
        BasePageBuilderImpl(config),
) : PagedExternalQuerySuspendingBuilder<Q, P, T>,
    BasePagedBuilder<PagedExternalQuerySuspendingBuilder<Q, P, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    private val externalQueryProvider: (Unit) -> Flow<Q> = { queryFlow() }

    override fun <Key : Any> withKeys(): PagedKeyedExternalQuerySuspendingBuilder<Key, Q, P, T> {
        return PagedKeyedExternalQuerySuspendingBuilderImpl(
            initialQueryProvider = { initialQueryProvider(Unit) },
            queryDebounceMillis = queryDebounceMillis,
            queryFlow = { queryFlow() },
            config = config,
        )
    }

    override fun build(contract: PagedQuerySuspendingContract<Q, P, T>): PagedStore<T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Q, P) -> PagedList<P, T>,
        onSaveToStorage: suspend (Q, P, PagedList<P, T>) -> Unit,
        onLoadFromStorage: suspend (Q, P) -> PagedList<P, T>?,
    ): PagedStore<T> {
        return PagedKeyedQueryStoreImpl<Unit, Q, P, T>(
            initialQueryProvider = initialQueryProvider,
            queryDebounceMillis = queryDebounceMillis,
            config = config,
            fetcher = { _, query, pageKey -> onFetch(query, pageKey) },
            loader = { _, query, pageKey -> onLoadFromStorage(query, pageKey) },
            saver = { _, query, pageKey, list -> onSaveToStorage(query, pageKey, list) },
            externalQueryProvider = externalQueryProvider,
        ).asPagedStore()
    }
}
