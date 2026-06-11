package com.elveum.store.contracts

import com.elveum.store.stores.paged.PagedList

/**
 * Contract for a paged store that fetches pages from a remote source only,
 * with no local persistence.
 *
 * @param PageKey the type used as a pagination cursor or page identifier.
 * @param T the type of individual items in each page.
 */
public interface PagedContract<PageKey : Any, T : Any> {

    /**
     * Fetches a page of data from a remote source for the given page key.
     *
     * @param pageKey the cursor or identifier of the page to fetch.
     * @return a [PagedList] containing the fetched items and the next page key.
     */
    public suspend fun fetch(pageKey: PageKey): PagedList<PageKey, T>
}

/**
 * Contract for a query-driven paged store that fetches pages from a remote source only,
 * with no local persistence.
 *
 * @param Q the type representing the query.
 * @param PageKey the type used as a pagination cursor or page identifier.
 * @param T the type of individual items in each page.
 */
public interface PagedQueryContract<Q : Any, PageKey : Any, T : Any> {

    /**
     * Fetches a page of data from a remote source for the given query and page key.
     *
     * @param query the query that parameterizes the fetch request.
     * @param pageKey the cursor or identifier of the page to fetch.
     * @return a [PagedList] containing the fetched items and the next page key.
     */
    public suspend fun fetch(query: Q, pageKey: PageKey): PagedList<PageKey, T>
}

/**
 * Contract for a paged store that fetches pages from a remote source and caches them
 * using suspending (one-shot) local storage operations.
 *
 * @param PageKey the type used as a pagination cursor or page identifier.
 * @param T the type of individual items in each page.
 */
public interface PagedSuspendingContract<PageKey : Any, T : Any> {

    /**
     * Fetches a page of data from a remote source for the given page key.
     *
     * @param pageKey the cursor or identifier of the page to fetch.
     * @return a [PagedList] containing the fetched items and the next page key.
     */
    public suspend fun fetch(pageKey: PageKey): PagedList<PageKey, T>

    /**
     * Persists the given page to local storage for the given page key.
     *
     * @param pageKey the cursor or identifier of the page being saved.
     * @param pagedList the page data to persist locally.
     */
    public suspend fun saveToLocalStorage(pageKey: PageKey, pagedList: PagedList<PageKey, T>)

    /**
     * Loads a cached page from local storage for the given page key.
     *
     * @param pageKey the cursor or identifier of the page to load.
     * @return the locally cached [PagedList], or `null` if no cached data exists for [pageKey].
     */
    public suspend fun loadFromLocalStorage(pageKey: PageKey): PagedList<PageKey, T>?
}

/**
 * Contract for a query-driven paged store that fetches pages from a remote source and
 * caches them using suspending (one-shot) local storage operations.
 *
 * @param Q the type representing the query.
 * @param PageKey the type used as a pagination cursor or page identifier.
 * @param T the type of individual items in each page.
 */
public interface PagedQuerySuspendingContract<Q : Any, PageKey : Any, T : Any> {

    /**
     * Fetches a page of data from a remote source for the given query and page key.
     *
     * @param query the query that parameterizes the fetch request.
     * @param pageKey the cursor or identifier of the page to fetch.
     * @return a [PagedList] containing the fetched items and the next page key.
     */
    public suspend fun fetch(query: Q, pageKey: PageKey): PagedList<PageKey, T>

    /**
     * Persists the given page to local storage for the given query and page key.
     *
     * @param query the query associated with the page being saved.
     * @param pageKey the cursor or identifier of the page being saved.
     * @param pagedList the page data to persist locally.
     */
    public suspend fun saveToLocalStorage(query: Q, pageKey: PageKey, pagedList: PagedList<PageKey, T>)

    /**
     * Loads a cached page from local storage for the given query and page key.
     *
     * @param query the query whose cached page should be loaded.
     * @param pageKey the cursor or identifier of the page to load.
     * @return the locally cached [PagedList], or `null` if no cached data exists for
     *   the given [query] and [pageKey].
     */
    public suspend fun loadFromLocalStorage(query: Q, pageKey: PageKey): PagedList<PageKey, T>?
}
