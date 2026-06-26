package com.elveum.store.load

import com.elveum.store.stores.base.BaseStore
import com.elveum.store.stores.keyed.KeyedStore

/**
 * Get the current value held by this store, or `null` if the latest result is not
 * a loaded one (i.e. it is loading or failed).
 *
 * @return the current value, or `null` when no value is available.
 */
public fun <T : Any> BaseStore<T>.getOrNull(): T? {
    return get().getOrNull()
}

/**
 * Get the current value held by this store for the given [key], or `null` if the latest
 * result for that key is not a loaded one (i.e. it is loading or failed).
 *
 * @param Key the type of the key identifying each item.
 * @param T the type of data managed by the store.
 * @param key the key whose current value should be returned.
 * @return the current value for [key], or `null` when no value is available.
 */
public fun <Key : Any, T : Any> KeyedStore<Key, T>.getOrNull(key: Key): T? {
    return get(key).getOrNull()
}

/**
 * Get the exception held by this store, or `null` if the latest result is not a failed one.
 *
 * @return the current failure, or `null` when the latest result is not failed.
 */
public fun <T : Any> BaseStore<T>.failureOrNull(): Exception? {
    return get().failureOrNull()
}

/**
 * Get the exception held by this store for the given [key], or `null` if the latest
 * result for that key is not a failed one.
 *
 * @param Key the type of the key identifying each item.
 * @param T the type of data managed by the store.
 * @param key the key whose current failure should be returned.
 * @return the current failure for [key], or `null` when the latest result is not failed.
 */
public fun <Key : Any, T : Any> KeyedStore<Key, T>.failureOrNull(key: Key): Exception? {
    return get(key).failureOrNull()
}
