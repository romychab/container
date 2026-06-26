package com.elveum.store.contracts

import kotlinx.coroutines.flow.Flow

/**
 * Contract for a simple store that fetches data from a remote source only,
 * with no local persistence.
 *
 * @param T the type of data fetched by this contract.
 */
public interface SimpleContract<T> {

    /**
     * Fetches data from a remote source.
     *
     * @return the fetched item of type [T].
     */
    public suspend fun fetch(): T
}

/**
 * Contract for a query-driven simple store that fetches data from a remote source only,
 * with no local persistence.
 *
 * @param Q the type representing the query.
 * @param T the type of data fetched by this contract.
 */
public interface SimpleQueryContract<Q, T> {

    /**
     * Fetches data from a remote source for the given query.
     *
     * @param query the query that parameterizes the fetch request.
     * @return the fetched item of type [T].
     */
    public suspend fun fetch(query: Q): T
}

/**
 * Contract for a simple store that fetches data from a remote source and caches it
 * using suspending (one-shot) local storage operations.
 *
 * @param T the type of data fetched and stored by this contract.
 */
public interface SimpleSuspendingContract<T : Any> {

    /**
     * Fetches data from a remote source.
     *
     * @return the fetched item of type [T].
     */
    public suspend fun fetch(): T

    /**
     * Persists the given data to local storage.
     *
     * @param data the item to persist locally.
     */
    public suspend fun saveToLocalStorage(data: T)

    /**
     * Loads cached data from local storage.
     *
     * @return the locally cached item, or `null` if no cached data exists.
     */
    public suspend fun loadFromLocalStorage(): T?
}

/**
 * Contract for a simple store that fetches data from a remote source and observes it
 * reactively via a [Flow]-based local storage layer.
 *
 * @param T the type of data fetched and observed by this contract.
 */
public interface SimpleReactiveContract<T : Any> {

    /**
     * Fetches data from a remote source.
     *
     * @return the fetched item of type [T].
     */
    public suspend fun fetch(): T

    /**
     * Persists the given data to local storage.
     *
     * @param data the item to persist locally.
     */
    public suspend fun saveToLocalStorage(data: T)

    /**
     * Returns a [Flow] that emits the locally cached value whenever it changes.
     *
     * @return a [Flow] emitting the current and future cached values, or `null`
     *   when no cached data is available.
     */
    public fun observeLocalStorage(): Flow<T?>
}

/**
 * Contract for a query-driven simple store that fetches data from a remote source and
 * caches it using suspending (one-shot) local storage operations.
 *
 * @param Q the type representing the query.
 * @param T the type of data fetched and stored by this contract.
 */
public interface SimpleQuerySuspendingContract<Q : Any, T : Any> {

    /**
     * Fetches data from a remote source for the given query.
     *
     * @param query the query that parameterizes the fetch request.
     * @return the fetched item of type [T].
     */
    public suspend fun fetch(query: Q): T

    /**
     * Persists the given data to local storage for the given query.
     *
     * @param query the query associated with the data being saved.
     * @param data the item to persist locally.
     */
    public suspend fun saveToLocalStorage(query: Q, data: T)

    /**
     * Loads cached data from local storage for the given query.
     *
     * @param query the query whose cached data should be loaded.
     * @return the locally cached item, or `null` if no cached data exists for [query].
     */
    public suspend fun loadFromLocalStorage(query: Q): T?
}

/**
 * Contract for a query-driven simple store that fetches data from a remote source and
 * observes it reactively via a [Flow]-based local storage layer.
 *
 * @param Q the type representing the query.
 * @param T the type of data fetched and observed by this contract.
 */
public interface SimpleQueryReactiveContract<Q : Any, T : Any> {

    /**
     * Fetches data from a remote source for the given query.
     *
     * @param query the query that parameterizes the fetch request.
     * @return the fetched item of type [T].
     */
    public suspend fun fetch(query: Q): T

    /**
     * Persists the given data to local storage for the given query.
     *
     * @param query the query associated with the data being saved.
     * @param data the item to persist locally.
     */
    public suspend fun saveToLocalStorage(query: Q, data: T)

    /**
     * Returns a [Flow] that emits the locally cached value for the given query
     * whenever it changes.
     *
     * @param query the query whose cached data should be observed.
     * @return a [Flow] emitting the current and future cached values, or `null`
     *   when no cached data is available for [query].
     */
    public fun observeLocalStorage(query: Q): Flow<T?>
}

/**
 * Contract for a simple store that has no remote fetcher and manages only local data
 * observed reactively via a [Flow] (for example, Room or DataStore returning a `Flow`
 * of items).
 *
 * @param T the type of data observed by this contract.
 */
public interface SimpleReactiveNoFetcherContract<T : Any> {

    /**
     * Returns a [Flow] that emits the local value whenever it changes.
     *
     * @return a [Flow] emitting the current and future local values.
     */
    public fun observe(): Flow<T>
}

/**
 * Contract for a query-driven simple store that has no remote fetcher and manages only
 * local data observed reactively via a [Flow] (for example, Room or DataStore returning
 * a `Flow` of items).
 *
 * @param Q the type representing the query.
 * @param T the type of data observed by this contract.
 */
public interface SimpleQueryReactiveNoFetcherContract<Q : Any, T : Any> {

    /**
     * Returns a [Flow] that emits the local value for the given query whenever it changes.
     *
     * @param query the query that parameterizes the observation.
     * @return a [Flow] emitting the current and future local values.
     */
    public fun observe(query: Q): Flow<T>
}
