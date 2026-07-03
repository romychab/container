package com.elveum.store.stores.simple

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.BaseSimpleStore
import com.elveum.store.stores.base.BaseStore
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.base.WithStoreLifecycleOwner
import kotlinx.coroutines.flow.Flow

/**
 * A simple store can load data without any additional request parameters. Examples:
 *
 * - User settings
 * - Profile
 * - Dashboard
 * - Non-paged lists
 *
 * This is a base class
 */
public interface SimpleStore<T : Any> :
    BaseSimpleStore<T>,
    WithStoreLifecycleOwner<SimpleStore<T>> {

    /**
     * Observe data managed by the store. Optionally, you can specify an additional
     * load [request] to specify how data must be loaded. If [request] is `null`,
     * the store's configured default load request is used instead
     * (see `BaseBuilder.setLoadRequest`).
     */
    override fun observe(request: LoadRequest?): Flow<StoreResult<T>>

    /**
     * Force a reload of the data managed by the store.
     *
     * Invalidation intentionally does not accept a [LoadRequest]: the store may have
     * several observers, each subscribed with its own request via [observe] (or the
     * store's configured default). Every observer keeps receiving data according to the
     * request it subscribed with. Use [invalidateAsync] for a fire-and-forget variant.
     */
    override suspend fun invalidate()

    /**
     * The same as [invalidate], but without waiting for invalidation results.
     */
    override fun invalidateAsync()

    /**
     * Update the in-memory cached data. It will be emitted to observers immediately.
     * Optimistic update can be used for smoother UI/UX experience while performing
     * long-running real updates.
     *
     * See [BaseStore.optimisticUpdate] for more details.
     *
     * See also: [updateIfSuccess][com.elveum.store.stores.base.updateIfSuccess] extension.
     */
    override suspend fun optimisticUpdate(updater: suspend OptimisticUpdateScope<T>.(T) -> Unit)

    /**
     * Register a [block] of code which is executed when the store becomes active (while at
     * least one observer start listening the store, and until cache is released).
     *
     * See [WithStoreLifecycleOwner.whenActive] for more details.
     */
    override fun whenActive(block: suspend SimpleStore<T>.() -> Unit): SimpleStore<T>

}
