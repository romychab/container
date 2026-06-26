package com.elveum.store.stores.base

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult

/**
 * Base interface for stores providing in-memory invalidation and update operations.
 */
public interface BaseStore<T : Any> {

    /**
     * The most actual result in the store.
     */
    public fun get(): StoreResult<T>

    /**
     * Force reload data managed by the store. If the data is already loaded,
     * it will be reloaded according to policy defined by [request].
     */
    public suspend fun invalidate(request: LoadRequest = LoadRequest.Default)

    /**
     * The same as [invalidate], but without waiting for invalidation results.
     */
    public fun invalidateAsync(request: LoadRequest = LoadRequest.Default)

    /**
     * Set a new store result manually to the in-memory cache.
     */
    public fun updateWith(storeResult: StoreResult<T>)

    /**
     * Update the in-memory cached data. It will be emitted to observers immediately.
     * Optimistic update can be used for smoother UI/UX experience while performing
     * long-running real updates.
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
 * Simple update the in-memory cached data. Can be used after real update in the data source
 * to reflect changes to all consumers subscribed to the store.
 */
public suspend fun <T : Any> BaseStore<T>.update(updater: suspend (T) -> T) {
    optimisticUpdate {
        emit(updater(it))
    }
}
