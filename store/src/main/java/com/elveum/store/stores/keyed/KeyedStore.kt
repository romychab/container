package com.elveum.store.stores.keyed

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.base.WithStoreLifecycleOwner
import kotlinx.coroutines.flow.Flow

/**
 * A keyed store that manages multiple values (like a Map collection) and allows
 * to observe each of keyed value separately.
 *
 * Unlike traditional map, the lifetime of each key and the corresponding loaded cached value
 * is determined by the configured timeout. The entry is deleted from the in-memory cache
 * when the timeout expires after the last observer unsubscribes from the flow.
 */
public interface KeyedStore<Key : Any, T : Any> : WithStoreLifecycleOwner<KeyedStore<Key, T>> {

    /**
     * Force reload data managed by the store. If the data is already loaded,
     * it will be reloaded according to policy defined by [request].
     */
    public suspend fun invalidate(key: Key, request: LoadRequest = LoadRequest.Default)

    /**
     * The same as [invalidate], but without waiting for invalidation results.
     */
    public fun invalidateAsync(key: Key, request: LoadRequest = LoadRequest.Default)

    /**
     * Update the in-memory cached data. It will be emitted to observers immediately.
     * Optimistic update can be used for smoother UI/UX experience while performing
     * long-running real updates.
     *
     * Usage example:
     *
     * ```
     * suspend fun updateTitle(productId: Long, title: String) {
     *     store.optimisticUpdate(productId) { oldProduct ->
     *         // 1. emit updated product immediately, so the user will see changes instantly
     *         emit(oldProduct.copy(title = newTitle))
     *         // 2. start real update:
     *         dataSource.updateProductTitle(productId, title)
     *         // 3. in case of failure, the updated product will be auto-reverted back in the cache.
     *     }
     * }
     * ```
     */
    public suspend fun optimisticUpdate(key: Key, updater: suspend OptimisticUpdateScope<T>.(T) -> Unit)

    /**
     * Fetch and observe data by the specified [key].
     */
    public fun observe(
        key: Key,
        request: LoadRequest = LoadRequest.Default,
    ): Flow<StoreResult<T>>

    /**
     * Register a [block] of code which is executed when the store becomes active (while at
     * least one observer start listening the store, and until cache is released).
     *
     * See [WithStoreLifecycleOwner.whenActive] for more details.
     */
    public override fun whenActive(block: suspend KeyedStore<Key, T>.() -> Unit): KeyedStore<Key, T>

}

/**
 * Simple update the in-memory cached data. Can be used after real update in the data source
 * to reflect changes to all consumers subscribed to the store.
 */
public suspend fun <Key : Any, T : Any> KeyedStore<Key, T>.update(key: Key, updater: suspend (T) -> T) {
    optimisticUpdate(key) {
        emit(updater(it))
    }
}
