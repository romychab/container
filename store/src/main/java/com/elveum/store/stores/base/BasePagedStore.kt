package com.elveum.store.stores.base

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow

/**
 * Base interface for non-keyed stores whose value is a paginated [List] loaded page
 * by page. Adds [onItemRendered] on top of [BaseSimpleStore].
 *
 * @param T the type of the individual items in the paged list.
 */
public interface BasePagedStore<T : Any> : BaseSimpleStore<List<T>> {

    /**
     * Notify the store that the item under [index] has been rendered to the user.
     *
     * The store can trigger the load of the next page when the rendered item is
     * located close enough to the next page. The fetch distance is determined by store
     * builder.
     *
     * @param index the index of the rendered item within the list.
     */
    public fun onItemRendered(index: Int)

    /**
     * Observe the data managed by the paged store.
     *
     * @param request the [LoadRequest] controlling how this observer loads and shows
     *   data. When `null`, the store's default request configured via
     *   [com.elveum.store.builders.base.BaseBuilder.setLoadRequest] is used.
     * @return a [Flow] emitting [StoreResult]s wrapping the merged list of loaded pages.
     */
    public override fun observe(
        request: LoadRequest?,
    ): Flow<StoreResult<List<T>>>

}
