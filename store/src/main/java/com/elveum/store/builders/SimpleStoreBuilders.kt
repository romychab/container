package com.elveum.store.builders

import com.elveum.container.Emitter
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleContract
import com.elveum.store.contracts.SimpleQueryContract
import com.elveum.store.contracts.SimpleQueryReactiveContract
import com.elveum.store.contracts.SimpleQueryReactiveNoFetcherContract
import com.elveum.store.contracts.SimpleQuerySuspendingContract
import com.elveum.store.contracts.SimpleReactiveContract
import com.elveum.store.contracts.SimpleReactiveNoFetcherContract
import com.elveum.store.contracts.SimpleSuspendingContract
import com.elveum.store.stores.simple.SimpleQueryStore
import com.elveum.store.stores.simple.SimpleStore
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

/**
 * Builder for creating a [SimpleStore] without local storage (remote-only).
 *
 * Use [withQuery] to enable query-driven reloading, [addSuspendingLocalStorage] or
 * [addReactiveLocalStorage] to add local persistence, or call [build] directly for
 * a network-only store.
 *
 * @param T the type of data held by the store.
 */
public interface SimpleBuilder<T : Any> : BaseBuilder<SimpleBuilder<T>> {

    /**
     * Transitions the builder to a query-aware variant that reloads data whenever
     * the active query changes.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query value to use when the store is first created.
     * @param debounceMillis how long (in milliseconds) to wait after a query change
     *   before triggering a reload; defaults to `0` (no debounce).
     * @return a [SimpleQueryBuilder] configured with the given query parameters.
     */
    public fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long = 0): SimpleQueryBuilder<Q, T>

    /**
     * Transitions the builder into a keyed variant: instead of a single value, the
     * resulting store manages one value per key (like a map of independently-cached
     * stores). See [SimpleKeyedBuilder].
     *
     * @param Key the type of the keys managed by the resulting store.
     * @return a [SimpleKeyedBuilder] preserving the configuration applied so far.
     */
    public fun <Key : Any> withKeys(): SimpleKeyedBuilder<Key, T>

    /**
     * Configure a simple store without fetcher. In this case, the store manages only
     * local data via reactive flow (for example, Room or DataStore returning Flow of items).
     */
    public fun disableFetcher(): SimpleReactiveNoFetcherBuilder<T>

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot)
     * local storage layer.
     *
     * @return a [SimpleSuspendingBuilder] for configuring suspending storage callbacks.
     */
    public fun addSuspendingLocalStorage(): SimpleSuspendingBuilder<T>

    /**
     * Transitions the builder to a variant that supports a reactive (Flow-based)
     * local storage layer.
     *
     * @return a [SimpleReactiveBuilder] for configuring reactive storage callbacks.
     */
    public fun addReactiveLocalStorage(): SimpleReactiveBuilder<T>

    /**
     * Builds a [SimpleStore] using the provided [SimpleContract] implementation.
     *
     * @param contract the contract defining how remote data is fetched.
     * @return the configured [SimpleStore].
     */
    public fun build(contract: SimpleContract<T>): SimpleStore<T>

    /**
     * Builds a [SimpleStore] using a single lambda for remote fetching.
     *
     * @param onFetch suspending function that fetches remote data.
     * @return the configured [SimpleStore].
     */
    public fun build(onFetch: suspend () -> T): SimpleStore<T>

    /**
     * Builds a [SimpleStore] with a custom loader that emits values manually through an
     * [Emitter]. This allows emitting more than one value per load (e.g. a cached value
     * followed by a fresh one) without attaching a full local storage layer.
     *
     * @param loader suspending lambda that emits values via [Emitter].
     * @return the configured [SimpleStore].
     */
    public fun buildCustom(loader: suspend Emitter<T>.() -> Unit): SimpleStore<T>

}

/**
 * Builder for creating a [SimpleQueryStore] without local storage (remote-only).
 *
 * @param Q the type representing the query.
 * @param T the type of data held by the store.
 */
public interface SimpleQueryBuilder<Q : Any, T : Any> : BaseBuilder<SimpleQueryBuilder<Q, T>> {

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot)
     * local storage layer.
     *
     * @return a [SimpleQuerySuspendingBuilder] for configuring suspending storage callbacks.
     */
    public fun addSuspendingLocalStorage(): SimpleQuerySuspendingBuilder<Q, T>

    /**
     * Transitions the builder to a variant that supports a reactive (Flow-based)
     * local storage layer.
     *
     * @return a [SimpleQueryReactiveBuilder] for configuring reactive storage callbacks.
     */
    public fun addReactiveLocalStorage(): SimpleQueryReactiveBuilder<Q, T>

    /**
     * Configure a simple query store without fetcher. In this case, the store manages only
     * local data via reactive flow (for example, Room or DataStore returning Flow of items).
     */
    public fun disableFetcher(): SimpleQueryReactiveNoFetcherBuilder<Q, T>

    /**
     * Transitions the builder into a keyed variant that manages one query-driven value
     * per key. See [SimpleKeyedQueryBuilder].
     *
     * @param Key the type of the keys managed by the resulting store.
     * @return a [SimpleKeyedQueryBuilder] preserving the configuration applied so far.
     */
    public fun <Key : Any> withKeys(): SimpleKeyedQueryBuilder<Key, Q, T>

    /**
     * Builds a [SimpleQueryStore] using the provided [SimpleQueryContract] implementation.
     *
     * @param contract the contract defining how remote data is fetched for a given query.
     * @return the configured [SimpleQueryStore].
     */
    public fun build(contract: SimpleQueryContract<Q, T>): SimpleQueryStore<Q, T>

    /**
     * Builds a [SimpleQueryStore] using a single lambda for remote fetching.
     *
     * @param onFetch suspending function that fetches remote data for the given query [Q].
     * @return the configured [SimpleQueryStore].
     */
    public fun build(onFetch: suspend (Q) -> T): SimpleQueryStore<Q, T>

    /**
     * Builds a [SimpleQueryStore] with a custom loader that emits values manually through
     * an [Emitter].
     *
     * @param loader suspending lambda, receiving the current query, that emits values via [Emitter].
     * @return the configured [SimpleQueryStore].
     */
    public fun buildCustom(loader: suspend Emitter<T>.(Q) -> Unit): SimpleQueryStore<Q, T>
}

/**
 * Builder for creating a [SimpleStore] backed by suspending (one-shot) local storage.
 *
 * @param T the type of data held by the store.
 */
public interface SimpleSuspendingBuilder<T : Any> : BaseBuilder<SimpleSuspendingBuilder<T>> {

    /**
     * Transitions the builder to a query-aware variant with suspending local storage.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query value to use when the store is first created.
     * @param debounceMillis how long (in milliseconds) to wait after a query change
     *   before triggering a reload; defaults to `0` (no debounce).
     * @return a [SimpleQuerySuspendingBuilder] configured with the given query parameters.
     */
    public fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long = 0): SimpleQuerySuspendingBuilder<Q, T>

    /**
     * Transitions the builder into a keyed variant with suspending local storage that
     * manages one value per key. See [SimpleKeyedSuspendingBuilder].
     *
     * @param Key the type of the keys managed by the resulting store.
     * @return a [SimpleKeyedSuspendingBuilder] preserving the configuration applied so far.
     */
    public fun <Key : Any> withKeys(): SimpleKeyedSuspendingBuilder<Key, T>

    /**
     * Builds a [SimpleStore] using the provided [SimpleSuspendingContract] implementation.
     *
     * @param contract the contract defining remote fetch and suspending local storage operations.
     * @return the configured [SimpleStore].
     */
    public fun build(contract: SimpleSuspendingContract<T>): SimpleStore<T>

    /**
     * Builds a [SimpleStore] using individual lambdas for remote fetch and local storage.
     *
     * @param onFetch suspending function that fetches remote data.
     * @param onSaveToStorage suspending function that persists fetched data locally.
     * @param onLoadFromStorage suspending function that loads cached data from local storage;
     *   returns `null` if no cached data is available.
     * @return the configured [SimpleStore].
     */
    public fun build(
        onFetch: suspend () -> T,
        onSaveToStorage: suspend (T) -> Unit,
        onLoadFromStorage: suspend () -> T?,
    ): SimpleStore<T>
}

/**
 * Builder for creating a [SimpleStore] backed by reactive (Flow-based) local storage.
 *
 * @param T the type of data held by the store.
 */
public interface SimpleReactiveBuilder<T : Any> : BaseBuilder<SimpleReactiveBuilder<T>> {

    /**
     * Transitions the builder to a query-aware variant with reactive local storage.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query value to use when the store is first created.
     * @param debounceMillis how long (in milliseconds) to wait after a query change
     *   before triggering a reload; defaults to `0` (no debounce).
     * @return a [SimpleQueryReactiveBuilder] configured with the given query parameters.
     */
    public fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long = 0): SimpleQueryReactiveBuilder<Q, T>

    /**
     * Configure a simple store without fetcher. In this case, the store manages only
     * local data via reactive flow (for example, Room or DataStore returning Flow of items).
     */
    public fun disableFetcher(): SimpleReactiveNoFetcherBuilder<T>

    /**
     * Transitions the builder into a keyed variant with reactive local storage that
     * manages one value per key. See [SimpleKeyedReactiveBuilder].
     *
     * @param Key the type of the keys managed by the resulting store.
     * @return a [SimpleKeyedReactiveBuilder] preserving the configuration applied so far.
     */
    public fun <Key : Any> withKeys(): SimpleKeyedReactiveBuilder<Key, T>

    /**
     * Builds a [SimpleStore] using the provided [SimpleReactiveContract] implementation.
     *
     * @param contract the contract defining remote fetch and reactive local storage operations.
     * @return the configured [SimpleStore].
     */
    public fun build(contract: SimpleReactiveContract<T>): SimpleStore<T>

    /**
     * Builds a [SimpleStore] using individual lambdas for remote fetch and reactive local storage.
     *
     * @param onFetch suspending function that fetches remote data.
     * @param onSaveToStorage suspending function that persists fetched data locally.
     * @param onObserveStorage function that returns a [Flow] emitting cached values;
     *   emits `null` when no cached data is available.
     * @return the configured [SimpleStore].
     */
    public fun build(
        onFetch: suspend () -> T,
        onSaveToStorage: suspend (T) -> Unit,
        onObserveStorage: () -> Flow<T?>,
    ): SimpleStore<T>
}

/**
 * Builder for creating a [SimpleQueryStore] backed by suspending (one-shot) local storage.
 *
 * @param Q the type representing the query.
 * @param T the type of data held by the store.
 */
public interface SimpleQuerySuspendingBuilder<Q : Any, T : Any> : BaseBuilder<SimpleQuerySuspendingBuilder<Q, T>> {

    /**
     * Transitions the builder into a keyed variant that manages one query-driven value
     * per key, backed by suspending local storage. See [SimpleKeyedQuerySuspendingBuilder].
     *
     * @param Key the type of the keys managed by the resulting store.
     * @return a [SimpleKeyedQuerySuspendingBuilder] preserving the configuration applied so far.
     */
    public fun <Key : Any> withKeys(): SimpleKeyedQuerySuspendingBuilder<Key, Q, T>

    /**
     * Builds a [SimpleQueryStore] using the provided [SimpleQuerySuspendingContract] implementation.
     *
     * @param contract the contract defining remote fetch and suspending local storage operations
     *   for the given query.
     * @return the configured [SimpleQueryStore].
     */
    public fun build(contract: SimpleQuerySuspendingContract<Q, T>): SimpleQueryStore<Q, T>

    /**
     * Builds a [SimpleQueryStore] using individual lambdas for remote fetch and local storage.
     *
     * @param onFetch suspending function that fetches remote data for the given query [Q].
     * @param onSaveToStorage suspending function that persists fetched data locally for the given query [Q].
     * @param onLoadFromStorage suspending function that loads cached data from local storage for the
     *   given query [Q]; returns `null` if no cached data is available.
     * @return the configured [SimpleQueryStore].
     */
    public fun build(
        onFetch: suspend (Q) -> T,
        onSaveToStorage: suspend (Q, T) -> Unit,
        onLoadFromStorage: suspend (Q) -> T?,
    ): SimpleQueryStore<Q, T>
}

/**
 * Builder for creating a [SimpleQueryStore] backed by reactive (Flow-based) local storage.
 *
 * @param Q the type representing the query.
 * @param T the type of data held by the store.
 */
public interface SimpleQueryReactiveBuilder<Q : Any, T : Any> : BaseBuilder<SimpleQueryReactiveBuilder<Q, T>> {

    /**
     * Configure a simple query store without fetcher. In this case, the store manages only
     * local data via reactive flow (for example, Room or DataStore returning Flow of items).
     */
    public fun disableFetcher(): SimpleQueryReactiveNoFetcherBuilder<Q, T>

    /**
     * Transitions the builder into a keyed variant that manages one query-driven value
     * per key, backed by reactive local storage. See [SimpleKeyedQueryReactiveBuilder].
     *
     * @param Key the type of the keys managed by the resulting store.
     * @return a [SimpleKeyedQueryReactiveBuilder] preserving the configuration applied so far.
     */
    public fun <Key : Any> withKeys(): SimpleKeyedQueryReactiveBuilder<Key, Q, T>

    /**
     * Builds a [SimpleQueryStore] using the provided [SimpleQueryReactiveContract] implementation.
     *
     * @param contract the contract defining remote fetch and reactive local storage operations
     *   for the given query.
     * @return the configured [SimpleQueryStore].
     */
    public fun build(contract: SimpleQueryReactiveContract<Q, T>): SimpleQueryStore<Q, T>

    /**
     * Builds a [SimpleQueryStore] using individual lambdas for remote fetch and reactive local storage.
     *
     * @param onFetch suspending function that fetches remote data for the given query [Q].
     * @param onSaveToStorage suspending function that persists fetched data locally for the given query [Q].
     * @param onObserveStorage function that returns a [Flow] emitting cached values for the given query [Q];
     *   emits `null` when no cached data is available.
     * @return the configured [SimpleQueryStore].
     */
    public fun build(
        onFetch: suspend (Q) -> T,
        onSaveToStorage: suspend (Q, T) -> Unit,
        onObserveStorage: (Q) -> Flow<T?>,
    ): SimpleQueryStore<Q, T>
}

/**
 * A builder for a simple store that has no remote fetcher and manages only local data
 * observed reactively via a [Flow].
 *
 * @param T the type of data managed by the store.
 */
public interface SimpleReactiveNoFetcherBuilder<T : Any> : BaseBuilder<SimpleReactiveNoFetcherBuilder<T>> {

    /**
     * Transitions the builder to a query-aware variant with reactive local storage.
     *
     * @param Q the type representing the query.
     * @param initialQuery the query value to use when the store is first created.
     * @param debounceMillis how long (in milliseconds) to wait after a query change
     *   before triggering a reload; defaults to `0` (no debounce).
     * @return a [SimpleQueryReactiveNoFetcherBuilder] configured with the given query parameters.
     */
    public fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long = 0): SimpleQueryReactiveNoFetcherBuilder<Q, T>

    /**
     * Transitions the builder into a keyed, fetcher-less variant that manages one
     * locally-observed value per key. See [SimpleKeyedReactiveNoFetcherBuilder].
     *
     * @param Key the type of the keys managed by the resulting store.
     * @return a [SimpleKeyedReactiveNoFetcherBuilder] preserving the configuration applied so far.
     */
    public fun <Key : Any> withKeys(): SimpleKeyedReactiveNoFetcherBuilder<Key, T>

    /**
     * Builds a [SimpleStore] using only a lambda for observing local data (no remote fetches).
     *
     * @param contract the contract defining reactive local storage observations.
     *
     * @return the configured [SimpleStore].
     */
    public fun build(contract: SimpleReactiveNoFetcherContract<T>): SimpleStore<T>

    /**
     * Builds a [SimpleStore] using only a lambda for observing local data (no remote fetches).
     *
     * @param onObserve function that returns a [Flow] emitting locally stored values.
     *
     * @return the configured [SimpleStore].
     */
    public fun build(onObserve: () -> Flow<T>): SimpleStore<T>

}

/**
 * A builder for a query-driven simple store that has no remote fetcher and manages only
 * local data observed reactively via a [Flow].
 *
 * @param Q the type representing the query.
 * @param T the type of data managed by the store.
 */
public interface SimpleQueryReactiveNoFetcherBuilder<Q : Any, T : Any> :
    BaseBuilder<SimpleQueryReactiveNoFetcherBuilder<Q, T>> {

    /**
     * Transitions the builder into a keyed, fetcher-less variant that manages one
     * query-driven, locally-observed value per key. See
     * [SimpleKeyedQueryReactiveNoFetcherBuilder].
     *
     * @param Key the type of the keys managed by the resulting store.
     * @return a [SimpleKeyedQueryReactiveNoFetcherBuilder] preserving the configuration applied so far.
     */
    public fun <Key : Any> withKeys(): SimpleKeyedQueryReactiveNoFetcherBuilder<Key, Q, T>

    /**
     * Builds a [SimpleQueryStore] using the provided [SimpleQueryReactiveNoFetcherContract]
     * implementation.
     *
     * @param contract the contract defining reactive local storage observations.
     *
     * @return the configured [SimpleQueryStore].
     */
    public fun build(contract: SimpleQueryReactiveNoFetcherContract<Q, T>): SimpleQueryStore<Q, T>

    /**
     * Builds a [SimpleStore] using only a lambda for observing local data (no remote fetches).
     *
     * @param onObserve function with input query param that returns a [Flow] emitting
     *   locally stored values.
     *
     * @return the configured [SimpleQueryStore].
     */
    public fun build(onObserve: (Q) -> Flow<T>): SimpleQueryStore<Q, T>

}
