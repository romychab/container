package com.elveum.store.internal.stores

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.paged.PagedStore
import kotlinx.coroutines.flow.Flow

internal class PagedStoreImpl<P : Any, T : Any>(
    private val origin: PagedKeyedQueryStoreImpl<Unit, Unit, P, T>,
) : PagedStore<T> {

    override fun onItemRendered(index: Int) = origin.onItemRendered(Unit, index)

    override fun observe(request: LoadRequest?): Flow<StoreResult<List<T>>> {
        return origin.observe(Unit, request)
    }

    override suspend fun invalidate() = origin.invalidate(Unit)

    override fun invalidateAsync() = origin.invalidateAsync(Unit)

    override suspend fun optimisticUpdate(updater: suspend OptimisticUpdateScope<List<T>>.(List<T>) -> Unit) {
        origin.optimisticUpdate(Unit, updater)
    }

    override fun whenActive(block: suspend PagedStore<T>.() -> Unit): PagedStore<T> {
        origin.whenActive { block(this@PagedStoreImpl) }
        return this
    }

    override fun get(): StoreResult<List<T>> = origin.get(Unit)

    override fun updateWith(storeResult: StoreResult<List<T>>) {
        origin.updateWith(Unit, storeResult)
    }
}

internal fun <P : Any, T : Any> PagedKeyedQueryStoreImpl<Unit, Unit, P, T>.asPagedStore() =
    PagedStoreImpl(this)
