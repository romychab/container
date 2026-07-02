package com.elveum.store.builders

import com.elveum.container.Emitter
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedContract
import com.elveum.store.contracts.SimpleKeyedQueryContract
import com.elveum.store.contracts.SimpleKeyedQueryReactiveContract
import com.elveum.store.contracts.SimpleKeyedQueryReactiveNoFetcherContract
import com.elveum.store.contracts.SimpleKeyedQuerySuspendingContract
import com.elveum.store.contracts.SimpleKeyedReactiveContract
import com.elveum.store.contracts.SimpleKeyedReactiveNoFetcherContract
import com.elveum.store.contracts.SimpleKeyedSuspendingContract
import com.elveum.store.stores.keyed.KeyedQueryStore
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.flow.Flow

/**
 * Builder for creating a keyed [KeyedStore] without local storage (remote-only).
 *
 * Obtained by calling `withKeys()` on a [SimpleBuilder]. Use [withQuery] to enable
 * query-driven reloading per key, [addSuspendingLocalStorage] or [addReactiveLocalStorage]
 * to add local persistence, [disableFetcher] for a local-only store, or one of the [build]
 * overloads for a network-only store.
 *
 * @param Key the type of the keys managed by the store.
 * @param T the type of value cached per key.
 */
public interface SimpleKeyedBuilder<Key : Any, T : Any> : BaseBuilder<SimpleKeyedBuilder<Key, T>> {

    /**
     * Transitions the builder to a query-aware variant that reloads a key's value
     * whenever that key's active query changes.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query value each key uses when first observed.
     * @param debounceMillis how long (in milliseconds) to wait after a query change
     *   before triggering a reload; defaults to `0` (no debounce).
     * @return a [SimpleKeyedQueryBuilder] configured with the given query parameters.
     */
    public fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long = 0): SimpleKeyedQueryBuilder<Key, Q, T>

    /**
     * Configure a keyed store without a fetcher. In this case, each key manages only
     * local data via a reactive flow (for example, Room or DataStore returning a `Flow`
     * of items per key).
     *
     * @return a [SimpleKeyedReactiveNoFetcherBuilder] for configuring the local-only store.
     */
    public fun disableFetcher(): SimpleKeyedReactiveNoFetcherBuilder<Key, T>

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot)
     * local storage layer.
     *
     * @return a [SimpleKeyedSuspendingBuilder] for configuring suspending storage callbacks.
     */
    public fun addSuspendingLocalStorage(): SimpleKeyedSuspendingBuilder<Key, T>

    /**
     * Transitions the builder to a variant that supports a reactive (Flow-based)
     * local storage layer.
     *
     * @return a [SimpleKeyedReactiveBuilder] for configuring reactive storage callbacks.
     */
    public fun addReactiveLocalStorage(): SimpleKeyedReactiveBuilder<Key, T>

    /**
     * Builds a [KeyedStore] using a single lambda for remote fetching per key.
     *
     * @param onFetch suspending function that fetches the remote value for a key.
     * @return the configured [KeyedStore].
     */
    public fun build(onFetch: suspend (Key) -> T): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] using the provided [SimpleKeyedContract] implementation.
     *
     * @param contract the contract defining how each key's remote value is fetched.
     * @return the configured [KeyedStore].
     */
    public fun build(contract: SimpleKeyedContract<Key, T>): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] with a custom loader that emits values manually through an
     * [Emitter]. This allows emitting more than one value per load (e.g. a cached value
     * followed by a fresh one) without attaching a full local storage layer.
     *
     * @param loader suspending lambda, receiving the key, that emits values via [Emitter].
     * @return the configured [KeyedStore].
     */
    public fun buildCustom(loader: suspend Emitter<T>.(Key) -> Unit): KeyedStore<Key, T>
}

/**
 * Builder for creating a query-driven keyed [KeyedQueryStore] without local storage
 * (remote-only).
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of value cached per key.
 */
public interface SimpleKeyedQueryBuilder<Key : Any, Q : Any, T : Any> :
    BaseBuilder<SimpleKeyedQueryBuilder<Key, Q, T>> {

    /**
     * Configure a query-driven keyed store without a fetcher; each key manages only
     * local data via a reactive flow.
     *
     * @return a [SimpleKeyedQueryReactiveNoFetcherBuilder] for the local-only store.
     */
    public fun disableFetcher(): SimpleKeyedQueryReactiveNoFetcherBuilder<Key, Q, T>

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot)
     * local storage layer.
     *
     * @return a [SimpleKeyedQuerySuspendingBuilder] for configuring suspending storage callbacks.
     */
    public fun addSuspendingLocalStorage(): SimpleKeyedQuerySuspendingBuilder<Key, Q, T>

    /**
     * Transitions the builder to a variant that supports a reactive (Flow-based)
     * local storage layer.
     *
     * @return a [SimpleKeyedQueryReactiveBuilder] for configuring reactive storage callbacks.
     */
    public fun addReactiveLocalStorage(): SimpleKeyedQueryReactiveBuilder<Key, Q, T>

    /**
     * Builds a [KeyedQueryStore] using a single lambda for remote fetching per key and query.
     *
     * @param onFetch suspending function that fetches the remote value for a key and query.
     * @return the configured [KeyedQueryStore].
     */
    public fun build(onFetch: suspend (Key, Q) -> T): KeyedQueryStore<Key, Q, T>

    /**
     * Builds a [KeyedQueryStore] using the provided [SimpleKeyedQueryContract] implementation.
     *
     * @param contract the contract defining how each key's remote value is fetched for a query.
     * @return the configured [KeyedQueryStore].
     */
    public fun build(contract: SimpleKeyedQueryContract<Key, Q, T>): KeyedQueryStore<Key, Q, T>

    /**
     * Builds a [KeyedQueryStore] with a custom loader that emits values manually through
     * an [Emitter].
     *
     * @param loader suspending lambda, receiving the key and query, that emits values via [Emitter].
     * @return the configured [KeyedQueryStore].
     */
    public fun buildCustom(loader: suspend Emitter<T>.(Key, Q) -> Unit): KeyedQueryStore<Key, Q, T>
}

/**
 * Builder for creating a keyed [KeyedStore] backed by reactive (Flow-based) local storage.
 *
 * @param Key the type of the keys managed by the store.
 * @param T the type of value cached per key.
 */
public interface SimpleKeyedReactiveBuilder<Key : Any, T : Any> :
    BaseBuilder<SimpleKeyedReactiveBuilder<Key, T>> {

    /**
     * Transitions the builder to a query-aware variant with reactive local storage.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query value each key uses when first observed.
     * @param debounceMillis how long (in milliseconds) to wait after a query change
     *   before triggering a reload; defaults to `0` (no debounce).
     * @return a [SimpleKeyedQueryReactiveBuilder] configured with the given query parameters.
     */
    public fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long = 0,
    ): SimpleKeyedQueryReactiveBuilder<Key, Q, T>

    /**
     * Configure a keyed store without a fetcher; each key manages only local data via a
     * reactive flow.
     *
     * @return a [SimpleKeyedReactiveNoFetcherBuilder] for the local-only store.
     */
    public fun disableFetcher(): SimpleKeyedReactiveNoFetcherBuilder<Key, T>

    /**
     * Builds a [KeyedStore] using individual lambdas for remote fetch and reactive local storage.
     *
     * @param onFetch suspending function that fetches the remote value for a key.
     * @param onSaveToStorage suspending function that persists a fetched value locally for a key.
     * @param onObserveStorage function that returns a [Flow] emitting a key's cached value;
     *   emits `null` when no cached data is available.
     * @return the configured [KeyedStore].
     */
    public fun build(
        onFetch: suspend (Key) -> T,
        onSaveToStorage: suspend (Key, T) -> Unit,
        onObserveStorage: (Key) -> Flow<T?>,
    ): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] using the provided [SimpleKeyedReactiveContract] implementation.
     *
     * @param contract the contract defining remote fetch and reactive local storage operations.
     * @return the configured [KeyedStore].
     */
    public fun build(contract: SimpleKeyedReactiveContract<Key, T>): KeyedStore<Key, T>
}

/**
 * Builder for a keyed store that has no remote fetcher and manages only local data
 * observed reactively via a [Flow] per key.
 *
 * @param Key the type of the keys managed by the store.
 * @param T the type of value cached per key.
 */
public interface SimpleKeyedReactiveNoFetcherBuilder<Key : Any, T : Any> :
    BaseBuilder<SimpleKeyedReactiveNoFetcherBuilder<Key, T>> {

    /**
     * Transitions the builder to a query-aware variant.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query value each key uses when first observed.
     * @param debounceMillis how long (in milliseconds) to wait after a query change
     *   before re-observing; defaults to `0` (no debounce).
     * @return a [SimpleKeyedQueryReactiveNoFetcherBuilder] configured with the given query parameters.
     */
    public fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long = 0,
    ): SimpleKeyedQueryReactiveNoFetcherBuilder<Key, Q, T>

    /**
     * Builds a [KeyedStore] using only a lambda for observing local data per key (no remote fetches).
     *
     * @param onObserve function that returns a [Flow] emitting a key's locally stored values.
     * @return the configured [KeyedStore].
     */
    public fun build(onObserve: (Key) -> Flow<T>): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] using the provided [SimpleKeyedReactiveNoFetcherContract] implementation.
     *
     * @param contract the contract defining reactive local storage observations per key.
     * @return the configured [KeyedStore].
     */
    public fun build(contract: SimpleKeyedReactiveNoFetcherContract<Key, T>): KeyedStore<Key, T>
}

/**
 * Builder for creating a keyed [KeyedStore] backed by suspending (one-shot) local storage.
 *
 * @param Key the type of the keys managed by the store.
 * @param T the type of value cached per key.
 */
public interface SimpleKeyedSuspendingBuilder<Key : Any, T : Any> :
    BaseBuilder<SimpleKeyedSuspendingBuilder<Key, T>> {

    /**
     * Transitions the builder to a query-aware variant with suspending local storage.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query value each key uses when first observed.
     * @param debounceMillis how long (in milliseconds) to wait after a query change
     *   before triggering a reload; defaults to `0` (no debounce).
     * @return a [SimpleKeyedQuerySuspendingBuilder] configured with the given query parameters.
     */
    public fun <Q : Any> withQuery(
        initialQuery: Q,
        debounceMillis: Long = 0,
    ): SimpleKeyedQuerySuspendingBuilder<Key, Q, T>

    /**
     * Builds a [KeyedStore] using individual lambdas for remote fetch and suspending local storage.
     *
     * @param onFetch suspending function that fetches the remote value for a key.
     * @param onSaveToStorage suspending function that persists a fetched value locally for a key.
     * @param onLoadFromStorage suspending function that loads a key's cached value from local
     *   storage; returns `null` if no cached data is available.
     * @return the configured [KeyedStore].
     */
    public fun build(
        onFetch: suspend (Key) -> T,
        onSaveToStorage: suspend (Key, T) -> Unit,
        onLoadFromStorage: suspend (Key) -> T?,
    ): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] using the provided [SimpleKeyedSuspendingContract] implementation.
     *
     * @param contract the contract defining remote fetch and suspending local storage operations.
     * @return the configured [KeyedStore].
     */
    public fun build(contract: SimpleKeyedSuspendingContract<Key, T>): KeyedStore<Key, T>
}

/**
 * Builder for a query-driven keyed store that has no remote fetcher and manages only
 * local data observed reactively via a [Flow] per key and query.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of value cached per key.
 */
public interface SimpleKeyedQueryReactiveNoFetcherBuilder<Key : Any, Q : Any, T : Any> :
    BaseBuilder<SimpleKeyedQueryReactiveNoFetcherBuilder<Key, Q, T>> {

    /**
     * Builds a [KeyedQueryStore] using only a lambda for observing local data per key and
     * query (no remote fetches).
     *
     * @param onObserve function, receiving the key and query, that returns a [Flow] emitting
     *   locally stored values.
     * @return the configured [KeyedQueryStore].
     */
    public fun build(onObserve: (Key, Q) -> Flow<T>): KeyedQueryStore<Key, Q, T>

    /**
     * Builds a [KeyedQueryStore] using the provided
     * [SimpleKeyedQueryReactiveNoFetcherContract] implementation.
     *
     * @param contract the contract defining reactive local storage observations per key and query.
     * @return the configured [KeyedQueryStore].
     */
    public fun build(contract: SimpleKeyedQueryReactiveNoFetcherContract<Key, Q, T>): KeyedQueryStore<Key, Q, T>
}

/**
 * Builder for creating a query-driven keyed [KeyedQueryStore] backed by suspending
 * (one-shot) local storage.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of value cached per key.
 */
public interface SimpleKeyedQuerySuspendingBuilder<Key : Any, Q : Any, T : Any> :
    BaseBuilder<SimpleKeyedQuerySuspendingBuilder<Key, Q, T>> {

    /**
     * Builds a [KeyedQueryStore] using individual lambdas for remote fetch and suspending
     * local storage.
     *
     * @param onFetch suspending function that fetches the remote value for a key and query.
     * @param onSaveToStorage suspending function that persists a fetched value locally for a
     *   key and query.
     * @param onLoadFromStorage suspending function that loads a cached value from local storage
     *   for a key and query; returns `null` if no cached data is available.
     * @return the configured [KeyedQueryStore].
     */
    public fun build(
        onFetch: suspend (Key, Q) -> T,
        onSaveToStorage: suspend (Key, Q, T) -> Unit,
        onLoadFromStorage: suspend (Key, Q) -> T?,
    ): KeyedQueryStore<Key, Q, T>

    /**
     * Builds a [KeyedQueryStore] using the provided [SimpleKeyedQuerySuspendingContract]
     * implementation.
     *
     * @param contract the contract defining remote fetch and suspending local storage operations.
     * @return the configured [KeyedQueryStore].
     */
    public fun build(contract: SimpleKeyedQuerySuspendingContract<Key, Q, T>): KeyedQueryStore<Key, Q, T>
}

/**
 * Builder for creating a query-driven keyed [KeyedQueryStore] backed by reactive
 * (Flow-based) local storage.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of value cached per key.
 */
public interface SimpleKeyedQueryReactiveBuilder<Key : Any, Q : Any, T : Any>
    : BaseBuilder<SimpleKeyedQueryReactiveBuilder<Key, Q, T>> {

    /**
     * Configure a query-driven keyed store without a fetcher; each key manages only
     * local data via a reactive flow.
     *
     * @return a [SimpleKeyedQueryReactiveNoFetcherBuilder] for the local-only store.
     */
    public fun disableFetcher(): SimpleKeyedQueryReactiveNoFetcherBuilder<Key, Q, T>

    /**
     * Builds a [KeyedQueryStore] using individual lambdas for remote fetch and reactive
     * local storage.
     *
     * @param onFetch suspending function that fetches the remote value for a key and query.
     * @param onSaveToStorage suspending function that persists a fetched value locally for a
     *   key and query.
     * @param onObserveStorage function that returns a [Flow] emitting a key/query cached value;
     *   emits `null` when no cached data is available.
     * @return the configured [KeyedQueryStore].
     */
    public fun build(
        onFetch: suspend (Key, Q) -> T,
        onSaveToStorage: suspend (Key, Q, T) -> Unit,
        onObserveStorage: (Key, Q) -> Flow<T?>,
    ): KeyedQueryStore<Key, Q, T>

    /**
     * Builds a [KeyedQueryStore] using the provided [SimpleKeyedQueryReactiveContract]
     * implementation.
     *
     * @param contract the contract defining remote fetch and reactive local storage operations.
     * @return the configured [KeyedQueryStore].
     */
    public fun build(contract: SimpleKeyedQueryReactiveContract<Key, Q, T>): KeyedQueryStore<Key, Q, T>
}
