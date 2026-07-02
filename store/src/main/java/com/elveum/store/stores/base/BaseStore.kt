package com.elveum.store.stores.base

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult

/**
 * Base interface for stores providing in-memory invalidation and update operations.
 */
public interface BaseStore<T : Any> {

    /**
     * The most actual result cached in the store.
     *
     * @return the latest [StoreResult]; [StoreResult.Loading] if nothing has been
     *   loaded yet.
     */
    public fun get(): StoreResult<T>

    /**
     * Force a reload of the data managed by the store.
     *
     * Invalidation intentionally does not accept a [LoadRequest]: the store may be
     * observed by several observers, each with its own request (fresh, offline, etc.).
     * Invalidation simply triggers a reload, and every observer keeps receiving data
     * according to the request it subscribed with (see
     * [com.elveum.store.builders.base.BaseBuilder.setLoadRequest] and
     * [BaseSimpleStore.observe]).
     *
     * This is a suspending call that returns once the reload has completed. Use
     * [invalidateAsync] for a fire-and-forget variant.
     */
    public suspend fun invalidate()

    /**
     * The same as [invalidate], but returns immediately without waiting for the
     * reload to complete.
     */
    public fun invalidateAsync()

    /**
     * Set a new store result manually into the in-memory cache. The value is emitted
     * to all current observers immediately.
     *
     * @param storeResult the result to place into the cache.
     */
    public fun updateWith(storeResult: StoreResult<T>)

    /**
     * Update the in-memory cached data. It will be emitted to observers immediately.
     * Optimistic update can be used for smoother UI/UX experience while performing
     * long-running real updates.
     *
     * **Requires an active observer.** The operation targets the in-memory cache, which
     * exists only while the store has at least one active observer (plus the configured
     * cache timeout after the last one unsubscribes). If there is currently no cached
     * value - i.e. the store is not being observed - this call is a no-op: [updater] is
     * not invoked. Trigger `observe` (and let the value load) before updating.
     *
     * Usage example:
     *
     * ```
     * suspend fun updateTitle(title: String) {
     *     store.optimisticUpdate { oldValue ->
     *         // 1. emit updated value immediately, so the user will see changes instantly
     *         emit(oldValue.copy(title = newTitle))
     *         // 2. start real update:
     *         dataSource.updateTitle(title)
     *         // 3. in case of failure, the updated value will be auto-reverted back in the cache.
     *     }
     * }
     * ```
     */
    public suspend fun optimisticUpdate(updater: suspend OptimisticUpdateScope<T>.(T) -> Unit)

}

/**
 * Read-modify-write helper: applies [updater] to the value currently cached in the store and
 * writes the result back to the in-memory cache, emitting it to all observers immediately.
 *
 * **The [updater] runs only while the store currently holds a [StoreResult.Loaded] value.** If
 * the store is still loading or is in a failed state there is nothing to transform, so the call
 * is a no-op and [updater] is not invoked. This is the key difference from [BaseStore.updateWith],
 * which always overwrites the cached result regardless of the current state - hence the explicit
 * `IfSuccess` suffix.
 *
 * The transform is applied through the same machinery as [BaseStore.optimisticUpdate], so
 * concurrent updates for the store are serialized. Typically used after the real data source has
 * already been changed, to reflect the change to every consumer:
 *
 * ```
 * suspend fun renameProfile(newName: String) {
 *     dataSource.renameProfile(newName)
 *     store.updateIfSuccess { it.copy(name = newName) }
 * }
 * ```
 *
 * @param T the type of data held by the store.
 * @param updater transform applied to the currently loaded value; its result becomes the new
 *   cached value.
 */
public suspend fun <T : Any> BaseStore<T>.updateIfSuccess(updater: suspend (T) -> T) {
    optimisticUpdate {
        emit(updater(it))
    }
}
