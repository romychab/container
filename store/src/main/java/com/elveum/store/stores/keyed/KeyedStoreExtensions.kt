package com.elveum.store.stores.keyed

/**
 * Invalidate all keys in the [KeyedStore].
 */
public fun <Key : Any, T : Any> KeyedStore<Key, T>.invalidateAllAsync() {
    activeKeys.value.forEach {
        invalidateAsync(it)
    }
}
