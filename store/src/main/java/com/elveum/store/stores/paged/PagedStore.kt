package com.elveum.store.stores.paged

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.BasePagedStore
import com.elveum.store.stores.base.WithQuery
import com.elveum.store.stores.base.BaseStore
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.base.WithStoreLifecycleOwner
import com.elveum.store.stores.base.update
import kotlinx.coroutines.flow.Flow

/**
 * A store loading chunks of data and merging them into the one final list to
 * be displayed in Lazy-containers with endless scrolling.
 */
public interface PagedStore<T : Any> :
    BasePagedStore<T>,
    WithStoreLifecycleOwner<PagedStore<T>> {

    /**
     * Notify the store that the item under [index] has been rendered to the user.
     *
     * The store can trigger the load of the next page when the rendered item is
     * located close enough to the next page. The fetch distance is determined by store
     * builder.
     */
    override fun onItemRendered(index: Int)

    /**
     * Observe data managed by the paged store. Optionally, you can specify an additional
     * load [request] to specify how data must be loaded.
     */
    override fun observe(request: LoadRequest): Flow<StoreResult<List<T>>>

    /**
     * Force reload data managed by the store. If the data is already loaded,
     * it will be reloaded according to policy defined by [request].
     */
    override suspend fun invalidate(request: LoadRequest)

    /**
     * The same as [invalidate], but without waiting for invalidation results.
     */
    override fun invalidateAsync(request: LoadRequest)

    /**
     * Update the in-memory cached data. It will be emitted to observers immediately.
     * Optimistic update can be used for smoother UI/UX experience while performing
     * long-running real updates.
     *
     * See [BaseStore.optimisticUpdate] for more details.
     *
     * See also: [update] extension.
     */
    override suspend fun optimisticUpdate(updater: suspend OptimisticUpdateScope<List<T>>.(List<T>) -> Unit)

    /**
     * Register a [block] of code which is executed when the store becomes active (while at
     * least one observer start listening the store, and until cache is released).
     *
     * See [WithStoreLifecycleOwner.whenActive] for more details.
     */
    override fun whenActive(block: suspend PagedStore<T>.() -> Unit): PagedStore<T>
}
