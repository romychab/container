package com.elveum.store.contracts

import kotlinx.coroutines.flow.Flow

/**
 * Contract for a keyed store that fetches data from a remote source only,
 * with no local persistence.
 *
 * @param Key the type used to identify individual items.
 * @param T the type of data fetched by this contract.
 */
public interface KeyedContract<Key : Any, T : Any> {

    /**
     * Fetches data from a remote source for the given key.
     *
     * @param key the identifier of the item to fetch.
     * @return the fetched item of type [T].
     */
    public suspend fun fetch(key: Key): T
}

/**
 * Contract for a keyed store that fetches data from a remote source and caches it
 * using suspending (one-shot) local storage operations.
 *
 * @param Key the type used to identify individual items.
 * @param T the type of data fetched and stored by this contract.
 */
public interface KeyedSuspendingContract<Key : Any, T : Any> {

    /**
     * Fetches data from a remote source for the given key.
     *
     * @param key the identifier of the item to fetch.
     * @return the fetched item of type [T].
     */
    public suspend fun fetch(key: Key): T

    /**
     * Persists the given data to local storage for the given key.
     *
     * @param key the identifier of the item being saved.
     * @param data the item to persist locally.
     */
    public suspend fun saveToLocalStorage(key: Key, data: T)

    /**
     * Loads cached data from local storage for the given key.
     *
     * @param key the identifier of the item to load.
     * @return the locally cached item, or `null` if no cached data exists for [key].
     */
    public suspend fun loadFromLocalStorage(key: Key): T?
}

/**
 * Contract for a keyed store that fetches data from a remote source and observes it
 * reactively via a [Flow]-based local storage layer.
 *
 * @param Key the type used to identify individual items.
 * @param T the type of data fetched and observed by this contract.
 */
public interface KeyedReactiveContract<Key : Any, T : Any> {

    /**
     * Fetches data from a remote source for the given key.
     *
     * @param key the identifier of the item to fetch.
     * @return the fetched item of type [T].
     */
    public suspend fun fetch(key: Key): T

    /**
     * Persists the given data to local storage for the given key.
     *
     * @param key the identifier of the item being saved.
     * @param data the item to persist locally.
     */
    public suspend fun saveToLocalStorage(key: Key, data: T)

    /**
     * Returns a [Flow] that emits the locally cached value for the given key whenever
     * it changes.
     *
     * @param key the identifier of the item to observe.
     * @return a [Flow] emitting the current and future cached values, or `null` when
     *   no cached data is available for [key].
     */
    public fun observeLocalStorage(key: Key): Flow<T?>
}
