package com.elveum.store.internal.stores

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.simple.SimpleQueryStore
import com.elveum.store.stores.simple.SimpleStore
import com.elveum.store.stores.base.BaseStore
import kotlinx.coroutines.flow.Flow

internal class SimpleStoreImpl<T : Any>(
    private val simpleQueryStore: SimpleQueryStore<Unit, T>
) : SimpleStore<T>, BaseStore<T> by simpleQueryStore {

    override fun whenActive(block: suspend SimpleStore<T>.() -> Unit): SimpleStore<T> = apply {
        simpleQueryStore.whenActive {
            block(this@SimpleStoreImpl)
        }
    }

    override fun observe(request: LoadRequest): Flow<StoreResult<T>> {
        return simpleQueryStore.observe(request)
    }

}
