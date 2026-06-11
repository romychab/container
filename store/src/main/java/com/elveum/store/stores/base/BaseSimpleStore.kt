package com.elveum.store.stores.base

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow

public interface BaseSimpleStore<T : Any> : BaseStore<T> {

    /**
     * Observe data managed by the store. Optionally, you can specify an additional
     * load [request] to specify how data must be loaded.
     */
    public fun observe(request: LoadRequest = LoadRequest.Default): Flow<StoreResult<T>>

}
