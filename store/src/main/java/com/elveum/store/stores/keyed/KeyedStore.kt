package com.elveum.store.stores.keyed

import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.base.WithStoreLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

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
     * A [StateFlow] emitting the set of keys that are currently active, i.e. keys
     * that have at least one observer (plus keys whose in-memory cache has not yet
     * expired after their last observer unsubscribed).
     *
     * This can be used to react to the store's lifecycle, e.g. to synchronize the
     * set of loaded entries with an external data source.
     */
    public val activeKeys: StateFlow<Set<Key>>

    /**
     * The most actual result cached in the store for the given [key].
     *
     * @param key the key whose cached result should be returned.
     * @return the latest [StoreResult] for [key]; [StoreResult.Loading] if nothing
     *   has been loaded for [key] yet.
     */
    public fun get(key: Key): StoreResult<T>

    /**
     * Force a reload of the value cached for the given [key].
     *
     * Invalidation intentionally does not accept a [LoadRequest]: a single key may be
     * observed by several observers, each with its own request (fresh, offline, etc.).
     * Invalidation simply triggers a reload, and every observer keeps receiving data
     * according to the request it subscribed with.
     *
     * This is a suspending call that returns once the reload has completed. Use
     * [invalidateAsync] for a fire-and-forget variant.
     *
     * @param key the key whose cached value should be reloaded.
     * @param metadata custom metadata values merged into the emitted result.
     */
    public suspend fun invalidate(key: Key, metadata: ContainerMetadata = EmptyMetadata)

    /**
     * The same as [invalidate], but returns immediately without waiting for the
     * reload to complete.
     *
     * @param key the key whose cached value should be reloaded.
     * @param metadata custom metadata values merged into the emitted result.
     */
    public fun invalidateAsync(key: Key, metadata: ContainerMetadata = EmptyMetadata)

    /**
     * Set a new store result manually to the in-memory cache.
     */
    public fun updateWith(key: Key, storeResult: StoreResult<T>)

    /**
     * Update the in-memory cached data. It will be emitted to observers immediately.
     * Optimistic update can be used for smoother UI/UX experience while performing
     * long-running real updates.
     *
     * **Requires an active observer for [key].** The operation targets the in-memory cache
     * of [key], which exists only while [key] has at least one active observer (plus the
     * configured cache timeout after the last one unsubscribes). If [key] is not currently
     * observed there is no cached value to update and this call is a no-op: [updater] is not
     * invoked. Trigger [observe] for [key] (and let the value load) before updating.
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
     * Fetch and observe the value associated with the specified [key].
     *
     * Loading starts lazily when the first observer subscribes; subsequent observers
     * of the same key share the same in-memory cache.
     *
     * @param key the key whose value should be observed.
     * @param request the [LoadRequest] controlling how this observer loads and shows
     *   data (fresh, offline, keeping content on reload, etc.). When `null`, the
     *   store's default request configured via
     *   [com.elveum.store.builders.base.BaseBuilder.setLoadRequest] is used.
     * @return a [Flow] emitting [StoreResult]s for [key].
     */
    public fun observe(
        key: Key,
        request: LoadRequest? = null,
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
 * Read-modify-write helper for a single [key]: applies [updater] to the value currently cached
 * for [key] and writes the result back to the in-memory cache, emitting it to observers of that
 * key immediately.
 *
 * **The [updater] runs only while [key] currently holds a [StoreResult.Loaded] value.** If [key]
 * is still loading or is in a failed state there is nothing to transform, so the call is a no-op
 * and [updater] is not invoked. This is the key difference from [KeyedStore.updateWith], which
 * always overwrites the cached result regardless of the current state - hence the explicit
 * `IfSuccess` suffix.
 *
 * The transform is applied through the same machinery as [KeyedStore.optimisticUpdate], so
 * concurrent updates for the same key are serialized. Typically used after the real data source
 * has already been changed, to reflect the change to every consumer of [key]:
 *
 * ```
 * suspend fun renameProduct(productId: Long, newTitle: String) {
 *     dataSource.renameProduct(productId, newTitle)
 *     store.updateIfSuccess(productId) { it.copy(title = newTitle) }
 * }
 * ```
 *
 * @param Key the type of the keys managed by the store.
 * @param T the type of value cached per key.
 * @param key the key whose cached value should be transformed.
 * @param updater transform applied to the currently loaded value; its result becomes the new
 *   cached value for [key].
 */
public suspend fun <Key : Any, T : Any> KeyedStore<Key, T>.updateIfSuccess(
    key: Key,
    updater: suspend (T) -> T,
) {
    optimisticUpdate(key) {
        emit(updater(it))
    }
}
