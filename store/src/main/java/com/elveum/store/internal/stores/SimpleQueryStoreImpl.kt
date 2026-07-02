package com.elveum.store.internal.stores

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.simple.SimpleQueryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class SimpleQueryStoreImpl<Q : Any, T : Any>(
    private val origin: KeyedQueryStoreImpl<Unit, Q, T>,
) : SimpleQueryStore<Q, T> {

    override val queryFlow: StateFlow<Q> get() = origin.observeQueryFlow(Unit)

    override suspend fun submitQuery(query: Q) {
        origin.submitQuery(Unit, query)
    }

    override fun submitQueryAsync(query: Q) {
        origin.submitQueryAsync(Unit, query)
    }

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

    override fun whenActive(block: suspend SimpleQueryStore<Q, T>.() -> Unit): SimpleQueryStore<Q, T> {
        origin.whenActive { this@SimpleQueryStoreImpl.block() }
        return this
    }

    override fun get(): StoreResult<T> {
        return origin.get(Unit)
    }

    override fun updateWith(storeResult: StoreResult<T>) {
        origin.updateWith(Unit, storeResult)
    }

}

internal fun <Q : Any, T : Any> KeyedQueryStoreImpl<Unit, Q, T>.asSimpleQueryStore() =
    SimpleQueryStoreImpl(this)
