package com.elveum.store.stores.base

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow

public interface BasePagedStore<T : Any> : BaseSimpleStore<List<T>> {

    /**
     * Notify the store that the item under [index] has been rendered to the user.
     *
     * The store can trigger the load of the next page when the rendered item is
     * located close enough to the next page. The fetch distance is determined by store
     * builder.
     */
    public fun onItemRendered(index: Int)

    /**
     * Observe data managed by the paged store. Optionally, you can specify an additional
     * load [request] to specify how data must be loaded.
     */
    public override fun observe(
        request: LoadRequest,
    ): Flow<StoreResult<List<T>>>

}
