package com.elveum.store.stores.paged

import com.elveum.container.ContainerMetadata
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.BasePagedStore
import com.elveum.store.stores.base.BaseStore
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.base.WithStoreLifecycleOwner
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
     * load [request] to specify how data must be loaded. If [request] is `null`,
     * the store's configured default load request is used instead
     * (see `BaseBuilder.setLoadRequest`).
     */
    override fun observe(request: LoadRequest?): Flow<StoreResult<List<T>>>

    /**
     * Force a reload of the data managed by the store.
     *
     * Invalidation intentionally does not accept a [LoadRequest]: the store may have
     * several observers, each subscribed with its own request via [observe] (or the
     * store's configured default). Every observer keeps receiving data according to the
     * request it subscribed with. Use [invalidateAsync] for a fire-and-forget variant.
     */
    override suspend fun invalidate(metadata: ContainerMetadata)

    /**
     * The same as [invalidate], but without waiting for invalidation results.
     */
    override fun invalidateAsync(metadata: ContainerMetadata)

    /**
     * Update the in-memory cached data. It will be emitted to observers immediately.
     * Optimistic update can be used for smoother UI/UX experience while performing
     * long-running real updates.
     *
     * See [BaseStore.optimisticUpdate] for more details.
     *
     * See also: [updateIfSuccess][com.elveum.store.stores.base.updateIfSuccess] extension.
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
