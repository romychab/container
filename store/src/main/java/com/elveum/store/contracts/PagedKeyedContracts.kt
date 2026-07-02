package com.elveum.store.contracts

import com.elveum.store.stores.paged.PagedList

/**
 * Contract for a keyed paged store that fetches each page of a key's list from a remote
 * source only, with no local persistence.
 *
 * @param Key the type of the keys managed by the store.
 * @param PageKey the type of the page key used to request the next page.
 * @param T the type of the items contained in each page.
 */
public interface PagedKeyedContract<Key : Any, PageKey : Any, T : Any> {

    /**
     * Fetches a single page of the list associated with the given [key].
     *
     * @param key the key whose list is being paged.
     * @param pageKey the key identifying the page to load.
     * @return a [PagedList] with the loaded items and the key of the next page
     *   (or `null` if there are no more pages).
     */
    public suspend fun fetch(key: Key, pageKey: PageKey): PagedList<PageKey, T>
}

/**
 * Contract for a query-driven keyed paged store that fetches each page of a key's list
 * from a remote source only, with no local persistence.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param PageKey the type of the page key used to request the next page.
 * @param T the type of the items contained in each page.
 */
public interface PagedKeyedQueryContract<Key : Any, Q : Any, PageKey : Any, T : Any> {

    /**
     * Fetches a single page of the list associated with the given [key] and [query].
     *
     * @param key the key whose list is being paged.
     * @param query the query that parameterizes the fetch request.
     * @param pageKey the key identifying the page to load.
     * @return a [PagedList] with the loaded items and the key of the next page
     *   (or `null` if there are no more pages).
     */
    public suspend fun fetch(key: Key, query: Q, pageKey: PageKey): PagedList<PageKey, T>
}

/**
 * Contract for a keyed paged store that fetches each page of a key's list from a remote
 * source and caches it using suspending (one-shot) local storage operations.
 *
 * @param Key the type of the keys managed by the store.
 * @param PageKey the type of the page key used to request the next page.
 * @param T the type of the items contained in each page.
 */
public interface PagedKeyedSuspendingContract<Key : Any, PageKey : Any, T : Any> {

    /**
     * Fetches a single page of the list associated with the given [key].
     *
     * @param key the key whose list is being paged.
     * @param pageKey the key identifying the page to load.
     * @return a [PagedList] with the loaded items and the key of the next page
     *   (or `null` if there are no more pages).
     */
    public suspend fun fetch(key: Key, pageKey: PageKey): PagedList<PageKey, T>

    /**
     * Persists the given [pagedList] page to local storage for the given [key].
     *
     * @param key the key whose page is being saved.
     * @param pageKey the key identifying the page being saved.
     * @param pagedList the page of items to persist locally.
     */
    public suspend fun saveToLocalStorage(key: Key, pageKey: PageKey, pagedList: PagedList<PageKey, T>)

    /**
     * Loads a cached page from local storage for the given [key].
     *
     * @param key the key whose page should be loaded.
     * @param pageKey the key identifying the page to load.
     * @return the locally cached page, or `null` if no cached data exists.
     */
    public suspend fun loadFromLocalStorage(key: Key, pageKey: PageKey): PagedList<PageKey, T>?
}

/**
 * Contract for a query-driven keyed paged store that fetches each page of a key's list
 * from a remote source and caches it using suspending (one-shot) local storage operations.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param PageKey the type of the page key used to request the next page.
 * @param T the type of the items contained in each page.
 */
public interface PagedKeyedQuerySuspendingContract<Key : Any, Q : Any, PageKey : Any, T : Any> {

    /**
     * Fetches a single page of the list associated with the given [key] and [query].
     *
     * @param key the key whose list is being paged.
     * @param query the query that parameterizes the fetch request.
     * @param pageKey the key identifying the page to load.
     * @return a [PagedList] with the loaded items and the key of the next page
     *   (or `null` if there are no more pages).
     */
    public suspend fun fetch(key: Key, query: Q, pageKey: PageKey): PagedList<PageKey, T>

    /**
     * Persists the given [pagedList] page to local storage for the given [key] and [query].
     *
     * @param key the key whose page is being saved.
     * @param query the query associated with the page being saved.
     * @param pageKey the key identifying the page being saved.
     * @param pagedList the page of items to persist locally.
     */
    public suspend fun saveToLocalStorage(key: Key, query: Q, pageKey: PageKey, pagedList: PagedList<PageKey, T>)

    /**
     * Loads a cached page from local storage for the given [key] and [query].
     *
     * @param key the key whose page should be loaded.
     * @param query the query whose page should be loaded.
     * @param pageKey the key identifying the page to load.
     * @return the locally cached page, or `null` if no cached data exists.
     */
    public suspend fun loadFromLocalStorage(key: Key, query: Q, pageKey: PageKey): PagedList<PageKey, T>?
}
