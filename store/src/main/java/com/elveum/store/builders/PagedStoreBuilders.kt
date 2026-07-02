package com.elveum.store.builders

import com.elveum.container.subject.paging.PageEmitter
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.PagedContract
import com.elveum.store.contracts.PagedQueryContract
import com.elveum.store.contracts.PagedQuerySuspendingContract
import com.elveum.store.contracts.PagedSuspendingContract
import com.elveum.store.stores.paged.PagedList
import com.elveum.store.stores.paged.PagedQueryStore
import com.elveum.store.stores.paged.PagedStore

/**
 * Base interface for paged store builders, extending [BaseBuilder] with pagination-specific
 * configuration.
 *
 * @param OutBuilder the concrete builder type returned by each configuration method,
 *   enabling fluent method chaining.
 */
public interface BasePagedBuilder<OutBuilder> : BaseBuilder<OutBuilder> {

    /**
     * Sets the number of items remaining in the visible list that triggers loading
     * the next page.
     *
     * @param itemCount the prefetch distance in items; when fewer than this many items
     *   are left, the store proactively fetches the next page.
     * @return this builder instance for fluent chaining.
     */
    public fun setFetchDistance(itemCount: Int): OutBuilder
}

/**
 * Builder for creating a [PagedStore] without local storage (remote-only).
 *
 * Use [withQuery] to enable query-driven reloading or [addSuspendingLocalStorage] for
 * local persistence, or call [build] directly for a network-only paged store.
 *
 * @param PageKey the type used as a pagination cursor or page identifier.
 * @param T the type of individual items in the paged list.
 */
public interface PagedBuilder<PageKey : Any, T : Any> : BasePagedBuilder<PagedBuilder<PageKey, T>> {

    /**
     * Transitions the builder into a keyed variant: instead of a single paged list, the
     * resulting store manages one independently-paged list per key. See [PagedKeyedBuilder].
     *
     * @param Key the type of the keys managed by the resulting store.
     * @return a [PagedKeyedBuilder] preserving the configuration applied so far.
     */
    public fun <Key : Any> withKeys(): PagedKeyedBuilder<Key, PageKey, T>

    /**
     * Transitions the builder to a query-aware variant that reloads the paged data
     * whenever the active query changes.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query value to use when the store is first created.
     * @param debounceMillis how long (in milliseconds) to wait after a query change
     *   before triggering a reload; defaults to `0` (no debounce).
     * @return a [PagedQueryBuilder] configured with the given query parameters.
     */
    public fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long = 0): PagedQueryBuilder<Q, PageKey, T>

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot)
     * local storage layer.
     *
     * @return a [PagedSuspendingBuilder] for configuring suspending storage callbacks.
     */
    public fun addSuspendingLocalStorage(): PagedSuspendingBuilder<PageKey, T>

    /**
     * Builds a [PagedStore] using the provided [PagedContract] implementation.
     *
     * @param contract the contract defining how each page is fetched for a given [PageKey].
     * @return the configured [PagedStore].
     */
    public fun build(contract: PagedContract<PageKey, T>): PagedStore<T>

    /**
     * Builds a [PagedStore] using a single lambda for remote page fetching.
     *
     * @param onFetch suspending function that fetches a [PagedList] for the given [PageKey].
     * @return the configured [PagedStore].
     */
    public fun build(
        onFetch: suspend (PageKey) -> PagedList<PageKey, T>,
    ): PagedStore<T>

    /**
     * Builds a [PagedStore] with a custom loader that emits pages manually through a
     * [PageEmitter], giving full control over how each page is produced.
     *
     * @param loader suspending lambda, receiving the page key, that emits pages via [PageEmitter].
     * @return the configured [PagedStore].
     */
    public fun buildCustom(loader: suspend PageEmitter<PageKey, T>.(PageKey) -> Unit): PagedStore<T>
}

/**
 * Builder for creating a [PagedQueryStore] without local storage (remote-only).
 *
 * @param Q the type representing the query.
 * @param PageKey the type used as a pagination cursor or page identifier.
 * @param T the type of individual items in the paged list.
 */
public interface PagedQueryBuilder<Q : Any, PageKey : Any, T : Any> :
        BasePagedBuilder<PagedQueryBuilder<Q, PageKey, T>> {

    /**
     * Transitions the builder into a keyed variant that manages one independently-paged,
     * query-driven list per key. See [PagedKeyedQueryBuilder].
     *
     * @param Key the type of the keys managed by the resulting store.
     * @return a [PagedKeyedQueryBuilder] preserving the configuration applied so far.
     */
    public fun <Key : Any> withKeys(): PagedKeyedQueryBuilder<Key, Q, PageKey, T>

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot)
     * local storage layer.
     *
     * @return a [PagedQuerySuspendingBuilder] for configuring suspending storage callbacks.
     */
    public fun addSuspendingLocalStorage(): PagedQuerySuspendingBuilder<Q, PageKey, T>

    /**
     * Builds a [PagedQueryStore] using the provided [PagedQueryContract] implementation.
     *
     * @param contract the contract defining how each page is fetched for the given query [Q]
     *   and [PageKey].
     * @return the configured [PagedQueryStore].
     */
    public fun build(contract: PagedQueryContract<Q, PageKey, T>): PagedQueryStore<Q, T>

    /**
     * Builds a [PagedQueryStore] using a single lambda for remote page fetching.
     *
     * @param onFetch suspending function that fetches a [PagedList] for the given query [Q]
     *   and [PageKey].
     * @return the configured [PagedQueryStore].
     */
    public fun build(
        onFetch: suspend (Q, PageKey) -> PagedList<PageKey, T>,
    ): PagedQueryStore<Q, T>

    /**
     * Builds a [PagedQueryStore] with a custom loader that emits pages manually through a
     * [PageEmitter].
     *
     * @param loader suspending lambda, receiving the current query and page key, that emits
     *   pages via [PageEmitter].
     * @return the configured [PagedQueryStore].
     */
    public fun buildCustom(loader: suspend PageEmitter<PageKey, T>.(Q, PageKey) -> Unit): PagedQueryStore<Q, T>

}

/**
 * Builder for creating a [PagedStore] backed by suspending (one-shot) local storage.
 *
 * @param PageKey the type used as a pagination cursor or page identifier.
 * @param T the type of individual items in the paged list.
 */
public interface PagedSuspendingBuilder<PageKey : Any, T : Any> :
        BasePagedBuilder<PagedSuspendingBuilder<PageKey, T>> {

    /**
     * Transitions the builder into a keyed variant with suspending local storage that
     * manages one independently-paged list per key. See [PagedKeyedSuspendingBuilder].
     *
     * @param Key the type of the keys managed by the resulting store.
     * @return a [PagedKeyedSuspendingBuilder] preserving the configuration applied so far.
     */
    public fun <Key : Any> withKeys(): PagedKeyedSuspendingBuilder<Key, PageKey, T>

    /**
     * Transitions the builder to a query-aware variant with suspending local storage.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query value to use when the store is first created.
     * @param debounceMillis how long (in milliseconds) to wait after a query change
     *   before triggering a reload; defaults to `0` (no debounce).
     * @return a [PagedQuerySuspendingBuilder] configured with the given query parameters.
     */
    public fun <Q : Any> withQuery(
        initialQuery: Q, debounceMillis: Long = 0,
    ): PagedQuerySuspendingBuilder<Q, PageKey, T>

    /**
     * Builds a [PagedStore] using the provided [PagedSuspendingContract] implementation.
     *
     * @param contract the contract defining remote fetch and suspending local storage operations.
     * @return the configured [PagedStore].
     */
    public fun build(contract: PagedSuspendingContract<PageKey, T>): PagedStore<T>

    /**
     * Builds a [PagedStore] using individual lambdas for remote fetch and local storage.
     *
     * @param onFetch suspending function that fetches a [PagedList] for the given [PageKey].
     * @param onSaveToStorage suspending function that persists a [PagedList] locally for
     *   the given [PageKey].
     * @param onLoadFromStorage suspending function that loads a cached [PagedList] from local
     *   storage for the given [PageKey]; returns `null` if no cached data is available.
     * @return the configured [PagedStore].
     */
    public fun build(
        onFetch: suspend (PageKey) -> PagedList<PageKey, T>,
        onSaveToStorage: suspend (PageKey, PagedList<PageKey, T>) -> Unit,
        onLoadFromStorage: suspend (PageKey) -> PagedList<PageKey, T>?,
    ): PagedStore<T>

}

/**
 * Builder for creating a [PagedQueryStore] backed by suspending (one-shot) local storage.
 *
 * @param Q the type representing the query.
 * @param PageKey the type used as a pagination cursor or page identifier.
 * @param T the type of individual items in the paged list.
 */
public interface PagedQuerySuspendingBuilder<Q : Any, PageKey: Any, T : Any> :
        BasePagedBuilder<PagedQuerySuspendingBuilder<Q, PageKey, T>> {

    /**
     * Transitions the builder into a keyed variant that manages one independently-paged,
     * query-driven list per key, backed by suspending local storage. See
     * [PagedKeyedQuerySuspendingBuilder].
     *
     * @param Key the type of the keys managed by the resulting store.
     * @return a [PagedKeyedQuerySuspendingBuilder] preserving the configuration applied so far.
     */
    public fun <Key : Any> withKeys(): PagedKeyedQuerySuspendingBuilder<Key, Q, PageKey, T>

    /**
     * Builds a [PagedQueryStore] using the provided [PagedQuerySuspendingContract] implementation.
     *
     * @param contract the contract defining remote fetch and suspending local storage operations
     *   for the given query and page key.
     * @return the configured [PagedQueryStore].
     */
    public fun build(contract: PagedQuerySuspendingContract<Q, PageKey, T>): PagedQueryStore<Q, T>

    /**
     * Builds a [PagedQueryStore] using individual lambdas for remote fetch and local storage.
     *
     * @param onFetch suspending function that fetches a [PagedList] for the given query [Q]
     *   and [PageKey].
     * @param onSaveToStorage suspending function that persists a [PagedList] locally for
     *   the given query [Q] and [PageKey].
     * @param onLoadFromStorage suspending function that loads a cached [PagedList] from local
     *   storage for the given query [Q] and [PageKey]; returns `null` if no cached data is available.
     * @return the configured [PagedQueryStore].
     */
    public fun build(
        onFetch: suspend (Q, PageKey) -> PagedList<PageKey, T>,
        onSaveToStorage: suspend (Q, PageKey, PagedList<PageKey, T>) -> Unit,
        onLoadFromStorage: suspend (Q, PageKey) -> PagedList<PageKey, T>?,
    ): PagedQueryStore<Q, T>

}
