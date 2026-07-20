package com.elveum.store.internal.stores

import com.elveum.container.ContainerMetadata
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.paged.PagedQueryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class PagedQueryStoreImpl<Q : Any, P : Any, T : Any>(
    private val origin: PagedKeyedQueryStoreImpl<Unit, Q, P, T>,
) : PagedQueryStore<Q, T> {

    override val queryFlow: StateFlow<Q> = origin.observeQueryFlow(Unit)

    override suspend fun submitQuery(query: Q, metadata: ContainerMetadata) {
        origin.submitQuery(Unit, query, metadata)
    }

    override fun submitQueryAsync(query: Q, metadata: ContainerMetadata) {
        origin.submitQueryAsync(Unit, query, metadata)
    }

    override fun onItemRendered(index: Int) = origin.onItemRendered(Unit, index)

    override fun observe(request: LoadRequest?): Flow<StoreResult<List<T>>> {
        return origin.observe(Unit, request)
    }

    override suspend fun invalidate(metadata: ContainerMetadata) = origin.invalidate(Unit, metadata)

    override fun invalidateAsync(metadata: ContainerMetadata) = origin.invalidateAsync(Unit, metadata)

    override suspend fun optimisticUpdate(updater: suspend OptimisticUpdateScope<List<T>>.(List<T>) -> Unit) {
        origin.optimisticUpdate(Unit, updater)
    }

    override fun whenActive(block: suspend PagedQueryStore<Q, T>.() -> Unit): PagedQueryStore<Q, T> {
        origin.whenActive { block() }
        return this
    }

    override fun get(): StoreResult<List<T>> = origin.get(Unit)

    override fun updateWith(storeResult: StoreResult<List<T>>) {
        origin.updateWith(Unit, storeResult)
    }
}

internal fun <P : Any, Q : Any, T : Any> PagedKeyedQueryStoreImpl<Unit, Q, P, T>.asPagedQueryStore() =
    PagedQueryStoreImpl(this)
