package com.elveum.store.builders

import com.elveum.container.subject.paging.PageEmitter
import com.elveum.store.contracts.PagedKeyedContract
import com.elveum.store.contracts.PagedKeyedQueryContract
import com.elveum.store.contracts.PagedKeyedQuerySuspendingContract
import com.elveum.store.contracts.PagedKeyedSuspendingContract
import com.elveum.store.stores.keyed.PagedKeyedQueryStore
import com.elveum.store.stores.keyed.PagedKeyedStore
import com.elveum.store.stores.paged.PagedList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Builder for creating a keyed [PagedKeyedStore] without local storage (remote-only),
 * where each key manages its own independent pagination.
 *
 * Obtained by calling `withKeys()` on a paged builder. Use [withQuery] for per-key
 * query-driven pagination, [addSuspendingLocalStorage] to add local persistence, or one
 * of the [build]/[buildCustom] overloads for a network-only store.
 *
 * @param Key the type of the keys managed by the store.
 * @param PageKey the type of the page key used to request the next page.
 * @param T the type of the items contained in each key's paged list.
 */
public interface PagedKeyedBuilder<Key : Any, PageKey : Any, T : Any> :
    BasePagedBuilder<PagedKeyedBuilder<Key, PageKey, T>> {

    /**
     * Transitions the builder to a query-aware variant that resets a key's pagination and
     * reloads it whenever that key's active query changes.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query value each key uses when first observed.
     * @param debounceMillis how long (in milliseconds) to wait after a query change
     *   before triggering a reload; defaults to `0` (no debounce).
     * @return a [PagedKeyedQueryBuilder] configured with the given query parameters.
     */
    public fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long = 0,
    ): PagedKeyedQueryBuilder<Key, Q, PageKey, T>

    /**
     * Transitions the builder to a variant whose per-key query is driven by an external [Flow].
     * Each key performs an immediate first load using [initialQuery]; whenever the flow returned by
     * [queryFlow] for a key emits a new query, that key's pagination is reset and reloaded from the
     * first page. The resulting store exposes no query API.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query each key uses for its immediate first load.
     * @param debounceMillis debounce applied to flow emissions before reloading; defaults to `0`.
     * @param queryFlow lambda returning the external query [Flow] for a given key.
     * @return a [PagedKeyedExternalQueryBuilder] driven by the given per-key query flow.
     */
    public fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long = 0,
        queryFlow: (Key) -> Flow<Q>,
    ): PagedKeyedExternalQueryBuilder<Key, Q, PageKey, T>

    /**
     * Convenience overload for a [StateFlow] query source: each key's initial query is derived from
     * that key's [StateFlow.value], so no explicit initial query is required.
     *
     * @param Q the type representing the query.
     * @param debounceMillis debounce applied to flow emissions before reloading; defaults to `0`.
     * @param queryFlow lambda returning the external query [StateFlow] for a given key.
     * @return a [PagedKeyedExternalQueryBuilder] driven by the given per-key query state flow.
     */
    public fun <Q : Any> withQuery(
        debounceMillis: Long = 0,
        queryFlow: (Key) -> StateFlow<Q>,
    ): PagedKeyedExternalQueryBuilder<Key, Q, PageKey, T>

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot)
     * local storage layer.
     *
     * @return a [PagedKeyedSuspendingBuilder] for configuring suspending storage callbacks.
     */
    public fun addSuspendingLocalStorage(): PagedKeyedSuspendingBuilder<Key, PageKey, T>

    /**
     * Builds a [PagedKeyedStore] using the provided [PagedKeyedContract] implementation.
     *
     * @param contract the contract defining how each key's pages are fetched.
     * @return the configured [PagedKeyedStore].
     */
    public fun build(contract: PagedKeyedContract<Key, PageKey, T>): PagedKeyedStore<Key, T>

    /**
     * Builds a [PagedKeyedStore] using a single lambda that fetches one page of a key's list.
     *
     * @param onFetch suspending function that, given a key and page key, returns a [PagedList]
     *   with the loaded items and the next page key (or `null` if there are no more pages).
     * @return the configured [PagedKeyedStore].
     */
    public fun build(
        onFetch: suspend (Key, PageKey) -> PagedList<PageKey, T>,
    ): PagedKeyedStore<Key, T>

    /**
     * Builds a [PagedKeyedStore] with a custom loader that emits pages manually through a
     * [PageEmitter], allowing full control over how a key's pages are produced.
     *
     * @param loader suspending lambda, receiving the key and page key, that emits pages via
     *   [PageEmitter].
     * @return the configured [PagedKeyedStore].
     */
    public fun buildCustom(
        loader: suspend PageEmitter<PageKey, T>.(Key, PageKey) -> Unit,
    ): PagedKeyedStore<Key, T>
}

/**
 * Builder for creating a query-driven keyed [PagedKeyedQueryStore] without local storage
 * (remote-only), where each key manages its own independent pagination.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param PageKey the type of the page key used to request the next page.
 * @param T the type of the items contained in each key's paged list.
 */
public interface PagedKeyedQueryBuilder<Key : Any, Q : Any, PageKey : Any, T : Any> :
    BasePagedBuilder<PagedKeyedQueryBuilder<Key, Q, PageKey, T>> {

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot)
     * local storage layer.
     *
     * @return a [PagedKeyedQuerySuspendingBuilder] for configuring suspending storage callbacks.
     */
    public fun addSuspendingLocalStorage(): PagedKeyedQuerySuspendingBuilder<Key, Q, PageKey, T>

    /**
     * Builds a [PagedKeyedQueryStore] using the provided [PagedKeyedQueryContract]
     * implementation.
     *
     * @param contract the contract defining how each key's pages are fetched for a query.
     * @return the configured [PagedKeyedQueryStore].
     */
    public fun build(contract: PagedKeyedQueryContract<Key, Q, PageKey, T>): PagedKeyedQueryStore<Key, Q, T>

    /**
     * Builds a [PagedKeyedQueryStore] using a single lambda that fetches one page of a
     * key's list for a given query.
     *
     * @param onFetch suspending function that, given a key, query and page key, returns a
     *   [PagedList] with the loaded items and the next page key (or `null` if there are no
     *   more pages).
     * @return the configured [PagedKeyedQueryStore].
     */
    public fun build(
        onFetch: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>,
    ): PagedKeyedQueryStore<Key, Q, T>

    /**
     * Builds a [PagedKeyedQueryStore] with a custom loader that emits pages manually
     * through a [PageEmitter].
     *
     * @param loader suspending lambda, receiving the key, query and page key, that emits
     *   pages via [PageEmitter].
     * @return the configured [PagedKeyedQueryStore].
     */
    public fun buildCustom(
        loader: suspend PageEmitter<PageKey, T>.(Key, Q, PageKey) -> Unit,
    ): PagedKeyedQueryStore<Key, Q, T>
}

/**
 * Builder for creating a keyed [PagedKeyedStore] backed by suspending (one-shot) local storage.
 *
 * @param Key the type of the keys managed by the store.
 * @param PageKey the type of the page key used to request the next page.
 * @param T the type of the items contained in each key's paged list.
 */
public interface PagedKeyedSuspendingBuilder<Key : Any, PageKey : Any, T : Any> :
    BasePagedBuilder<PagedKeyedSuspendingBuilder<Key, PageKey, T>> {

    /**
     * Transitions the builder to a query-aware variant with suspending local storage.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query value each key uses when first observed.
     * @param debounceMillis how long (in milliseconds) to wait after a query change
     *   before triggering a reload; defaults to `0` (no debounce).
     * @return a [PagedKeyedQuerySuspendingBuilder] configured with the given query parameters.
     */
    public fun <Q : Any> withQuery(
        initialQuery: Q, debounceMillis: Long = 0,
    ): PagedKeyedQuerySuspendingBuilder<Key, Q, PageKey, T>

    /**
     * Transitions the builder to a variant whose per-key query is driven by an external [Flow],
     * backed by suspending local storage. Each key performs an immediate first load using
     * [initialQuery]; whenever [queryFlow] for a key emits a new query, that key's pagination is
     * reset and reloaded from the first page. The resulting store exposes no query API.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query each key uses for its immediate first load.
     * @param debounceMillis debounce applied to flow emissions before reloading; defaults to `0`.
     * @param queryFlow lambda returning the external query [Flow] for a given key.
     * @return a [PagedKeyedExternalQuerySuspendingBuilder] driven by the given per-key query flow.
     */
    public fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long = 0,
        queryFlow: (Key) -> Flow<Q>,
    ): PagedKeyedExternalQuerySuspendingBuilder<Key, Q, PageKey, T>

    /**
     * Convenience overload for a [StateFlow] query source: each key's initial query is derived from
     * that key's [StateFlow.value], so no explicit initial query is required.
     *
     * @param Q the type representing the query.
     * @param debounceMillis debounce applied to flow emissions before reloading; defaults to `0`.
     * @param queryFlow lambda returning the external query [StateFlow] for a given key.
     * @return a [PagedKeyedExternalQuerySuspendingBuilder] driven by the given per-key query state flow.
     */
    public fun <Q : Any> withQuery(
        debounceMillis: Long = 0,
        queryFlow: (Key) -> StateFlow<Q>,
    ): PagedKeyedExternalQuerySuspendingBuilder<Key, Q, PageKey, T>

    /**
     * Builds a [PagedKeyedStore] using the provided [PagedKeyedSuspendingContract]
     * implementation.
     *
     * @param contract the contract defining page fetch and suspending local storage operations.
     * @return the configured [PagedKeyedStore].
     */
    public fun build(contract: PagedKeyedSuspendingContract<Key, PageKey, T>): PagedKeyedStore<Key, T>

    /**
     * Builds a [PagedKeyedStore] using individual lambdas for page fetch and suspending
     * local storage.
     *
     * @param onFetch suspending function that fetches one page of a key's list.
     * @param onSaveToStorage suspending function that persists a fetched page locally.
     * @param onLoadFromStorage suspending function that loads a cached page from local storage;
     *   returns `null` if no cached data is available.
     * @return the configured [PagedKeyedStore].
     */
    public fun build(
        onFetch: suspend (Key, PageKey) -> PagedList<PageKey, T>,
        onSaveToStorage: suspend (Key, PageKey, PagedList<PageKey, T>) -> Unit,
        onLoadFromStorage: suspend (Key, PageKey) -> PagedList<PageKey, T>?,
    ): PagedKeyedStore<Key, T>
}

/**
 * Builder for creating a query-driven keyed [PagedKeyedQueryStore] backed by suspending
 * (one-shot) local storage.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param PageKey the type of the page key used to request the next page.
 * @param T the type of the items contained in each key's paged list.
 */
public interface PagedKeyedQuerySuspendingBuilder<Key : Any, Q : Any, PageKey: Any, T : Any> :
    BasePagedBuilder<PagedKeyedQuerySuspendingBuilder<Key, Q, PageKey, T>> {

    /**
     * Builds a [PagedKeyedQueryStore] using the provided [PagedKeyedQuerySuspendingContract]
     * implementation.
     *
     * @param contract the contract defining page fetch and suspending local storage operations.
     * @return the configured [PagedKeyedQueryStore].
     */
    public fun build(contract: PagedKeyedQuerySuspendingContract<Key, Q, PageKey, T>): PagedKeyedQueryStore<Key, Q, T>

    /**
     * Builds a [PagedKeyedQueryStore] using individual lambdas for page fetch and suspending
     * local storage.
     *
     * @param onFetch suspending function that fetches one page of a key's list for a query.
     * @param onSaveToStorage suspending function that persists a fetched page locally.
     * @param onLoadFromStorage suspending function that loads a cached page from local storage;
     *   returns `null` if no cached data is available.
     * @return the configured [PagedKeyedQueryStore].
     */
    public fun build(
        onFetch: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>,
        onSaveToStorage: suspend (Key, Q, PageKey, PagedList<PageKey, T>) -> Unit,
        onLoadFromStorage: suspend (Key, Q, PageKey) -> PagedList<PageKey, T>?,
    ): PagedKeyedQueryStore<Key, Q, T>
}
