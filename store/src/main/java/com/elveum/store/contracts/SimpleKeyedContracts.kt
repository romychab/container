package com.elveum.store.contracts

import kotlinx.coroutines.flow.Flow

/**
 * Contract for a keyed store that fetches each key's value from a remote source only,
 * with no local persistence.
 *
 * @param Key the type of the keys managed by the store.
 * @param T the type of value fetched by this contract.
 */
public interface SimpleKeyedContract<Key : Any, T : Any> {

    /**
     * Fetches the value associated with the given [key] from a remote source.
     *
     * @param key the key whose value should be fetched.
     * @return the fetched value of type [T].
     */
    public suspend fun fetch(key: Key): T
}

/**
 * Contract for a query-driven keyed store that fetches each key's value from a remote
 * source only, with no local persistence.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of value fetched by this contract.
 */
public interface SimpleKeyedQueryContract<Key : Any, Q : Any, T : Any> {

    /**
     * Fetches the value associated with the given [key] and [query] from a remote source.
     *
     * @param key the key whose value should be fetched.
     * @param query the query that parameterizes the fetch request.
     * @return the fetched value of type [T].
     */
    public suspend fun fetch(key: Key, query: Q): T
}

/**
 * Contract for a keyed store that fetches each key's value from a remote source and
 * observes it reactively via a [Flow]-based local storage layer.
 *
 * @param Key the type of the keys managed by the store.
 * @param T the type of value fetched and observed by this contract.
 */
public interface SimpleKeyedReactiveContract<Key : Any, T : Any> {

    /**
     * Fetches the value associated with the given [key] from a remote source.
     *
     * @param key the key whose value should be fetched.
     * @return the fetched value of type [T].
     */
    public suspend fun fetch(key: Key): T

    /**
     * Returns a [Flow] that emits the locally cached value for the given [key] whenever
     * it changes.
     *
     * @param key the key whose cached value should be observed.
     * @return a [Flow] emitting the current and future cached values, or `null` when no
     *   cached data is available for [key].
     */
    public fun observeLocalStorage(key: Key): Flow<T?>

    /**
     * Persists the given [data] to local storage for the given [key].
     *
     * @param key the key associated with the data being saved.
     * @param data the value to persist locally.
     */
    public suspend fun saveToLocalStorage(key: Key, data: T)
}

/**
 * Contract for a keyed store that fetches each key's value from a remote source and
 * caches it using suspending (one-shot) local storage operations.
 *
 * @param Key the type of the keys managed by the store.
 * @param T the type of value fetched and stored by this contract.
 */
public interface SimpleKeyedSuspendingContract<Key : Any, T : Any> {

    /**
     * Fetches the value associated with the given [key] from a remote source.
     *
     * @param key the key whose value should be fetched.
     * @return the fetched value of type [T].
     */
    public suspend fun fetch(key: Key): T

    /**
     * Loads the cached value for the given [key] from local storage.
     *
     * @param key the key whose cached value should be loaded.
     * @return the locally cached value, or `null` if no cached data exists for [key].
     */
    public suspend fun loadFromLocalStorage(key: Key): T?

    /**
     * Persists the given [data] to local storage for the given [key].
     *
     * @param key the key associated with the data being saved.
     * @param data the value to persist locally.
     */
    public suspend fun saveToLocalStorage(key: Key, data: T)
}

/**
 * Contract for a keyed store that has no remote fetcher and manages only local data
 * observed reactively via a [Flow] (for example, Room or DataStore returning a `Flow`
 * of items per key).
 *
 * @param Key the type of the keys managed by the store.
 * @param T the type of value observed by this contract.
 */
public interface SimpleKeyedReactiveNoFetcherContract<Key : Any, T : Any> {

    /**
     * Returns a [Flow] that emits the local value for the given [key] whenever it changes.
     *
     * @param key the key whose local value should be observed.
     * @return a [Flow] emitting the current and future local values.
     */
    public fun observe(key: Key): Flow<T>
}

/**
 * Contract for a query-driven keyed store that has no remote fetcher and manages only
 * local data observed reactively via a [Flow] (for example, Room or DataStore returning
 * a `Flow` of items per key and query).
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of value observed by this contract.
 */
public interface SimpleKeyedQueryReactiveNoFetcherContract<Key : Any, Q : Any, T : Any> {

    /**
     * Returns a [Flow] that emits the local value for the given [key] and [query]
     * whenever it changes.
     *
     * @param key the key whose local value should be observed.
     * @param query the query that parameterizes the observation.
     * @return a [Flow] emitting the current and future local values.
     */
    public fun observe(key: Key, query: Q): Flow<T>
}

/**
 * Contract for a query-driven keyed store that fetches each key's value from a remote
 * source and observes it reactively via a [Flow]-based local storage layer.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of value fetched and observed by this contract.
 */
public interface SimpleKeyedQueryReactiveContract<Key : Any, Q : Any, T : Any> {

    /**
     * Fetches the value associated with the given [key] and [query] from a remote source.
     *
     * @param key the key whose value should be fetched.
     * @param query the query that parameterizes the fetch request.
     * @return the fetched value of type [T].
     */
    public suspend fun fetch(key: Key, query: Q): T

    /**
     * Persists the given [data] to local storage for the given [key] and [query].
     *
     * @param key the key associated with the data being saved.
     * @param query the query associated with the data being saved.
     * @param data the value to persist locally.
     */
    public suspend fun saveToLocalStorage(key: Key, query: Q, data: T)

    /**
     * Returns a [Flow] that emits the locally cached value for the given [key] and
     * [query] whenever it changes.
     *
     * @param key the key whose cached value should be observed.
     * @param query the query whose cached value should be observed.
     * @return a [Flow] emitting the current and future cached values, or `null` when no
     *   cached data is available.
     */
    public fun observeLocalStorage(key: Key, query: Q): Flow<T?>
}

/**
 * Contract for a query-driven keyed store that fetches each key's value from a remote
 * source and caches it using suspending (one-shot) local storage operations.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of value fetched and stored by this contract.
 */
public interface SimpleKeyedQuerySuspendingContract<Key : Any, Q : Any, T : Any> {

    /**
     * Fetches the value associated with the given [key] and [query] from a remote source.
     *
     * @param key the key whose value should be fetched.
     * @param query the query that parameterizes the fetch request.
     * @return the fetched value of type [T].
     */
    public suspend fun fetch(key: Key, query: Q): T

    /**
     * Persists the given [data] to local storage for the given [key] and [query].
     *
     * @param key the key associated with the data being saved.
     * @param query the query associated with the data being saved.
     * @param data the value to persist locally.
     */
    public suspend fun saveToLocalStorage(key: Key, query: Q, data: T)

    /**
     * Loads the cached value for the given [key] and [query] from local storage.
     *
     * @param key the key whose cached value should be loaded.
     * @param query the query whose cached value should be loaded.
     * @return the locally cached value, or `null` if no cached data exists.
     */
    public suspend fun loadFromLocalStorage(key: Key, query: Q): T?
}
