package com.elveum.store.stores.base

/**
 * See [BaseStore.optimisticUpdate] for more details.
 *
 * Using this scope you can update values in a cache ahead of time before
 * actual update finishes. All updates made by [emit] call are automatically
 * reverted if the actual update fails.
 */
public fun interface OptimisticUpdateScope<T> {
    public suspend fun emit(optimisticValue: T)
}
