package com.elveum.store.internal.stores

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.simple.SimpleStore
import kotlinx.coroutines.flow.Flow

internal class SimpleStoreImpl<T : Any>(
    private val origin: KeyedQueryStoreImpl<Unit, Unit, T>,
) : SimpleStore<T> {
    override fun observe(request: LoadRequest?): Flow<StoreResult<T>> {
        return origin.observe(Unit, request)
    }

    override suspend fun invalidate() {
        origin.invalidate(Unit)
    }

    override fun invalidateAsync() {
        origin.invalidateAsync(Unit)
    }

    override suspend fun optimisticUpdate(updater: suspend OptimisticUpdateScope<T>.(T) -> Unit) {
        origin.optimisticUpdate(Unit, updater)
    }

    override fun whenActive(block: suspend SimpleStore<T>.() -> Unit): SimpleStore<T> {
        origin.whenActive { this@SimpleStoreImpl.block() }
        return this
    }

    override fun get(): StoreResult<T> {
        return origin.get(Unit)
    }

    override fun updateWith(storeResult: StoreResult<T>) {
        origin.updateWith(Unit, storeResult)
    }
}

internal fun <T : Any> KeyedQueryStoreImpl<Unit, Unit, T>.asSimpleStore() =
    SimpleStoreImpl(this)
