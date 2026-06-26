package com.elveum.store.builders

import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.KeyedContract
import com.elveum.store.contracts.KeyedReactiveContract
import com.elveum.store.contracts.KeyedReactiveNoFetcherContract
import com.elveum.store.contracts.KeyedSuspendingContract
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.flow.Flow

/**
 * Builder for creating a [KeyedStore] without local storage (remote-only).
 *
 * Call [addSuspendingLocalStorage] or [addReactiveLocalStorage] to opt into
 * local persistence, or call [build] directly for a network-only store.
 *
 * @param Key the type used to identify individual items in the store.
 * @param T the type of data held by the store.
 */
public interface KeyedBuilder<Key : Any, T : Any> : BaseBuilder<KeyedBuilder<Key, T>> {

    /**
     * Configure a keyed store without fetcher. In this case, the store manages only
     * local data via reactive flow (for example, Room or DataStore returning Flow of items).
     */
    public fun disableFetcher(): KeyedReactiveNoFetcherBuilder<Key, T>

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot)
     * local storage layer.
     *
     * @return a [KeyedSuspendingBuilder] for configuring suspending storage callbacks.
     */
    public fun addSuspendingLocalStorage(): KeyedSuspendingBuilder<Key, T>

    /**
     * Transitions the builder to a variant that supports a reactive (Flow-based)
     * local storage layer.
     *
     * @return a [KeyedReactiveBuilder] for configuring reactive storage callbacks.
     */
    public fun addReactiveLocalStorage(): KeyedReactiveBuilder<Key, T>

    /**
     * Builds a [KeyedStore] using the provided [KeyedContract] implementation.
     *
     * @param contract the contract that defines how remote data is fetched for a given key.
     * @return the configured [KeyedStore].
     */
    public fun build(contract: KeyedContract<Key, T>): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] using a single lambda for remote fetching.
     *
     * @param onFetch suspending function that fetches remote data for the given [Key].
     * @return the configured [KeyedStore].
     */
    public fun build(onFetch: suspend (Key) -> T): KeyedStore<Key, T>
}

/**
 * Builder for creating a [KeyedStore] backed by suspending (one-shot) local storage.
 *
 * @param Key the type used to identify individual items in the store.
 * @param T the type of data held by the store.
 */
public interface KeyedSuspendingBuilder<Key : Any, T : Any> : BaseBuilder<KeyedSuspendingBuilder<Key, T>> {

    /**
     * Builds a [KeyedStore] using the provided [KeyedSuspendingContract] implementation.
     *
     * @param contract the contract defining remote fetch and suspending local storage operations.
     * @return the configured [KeyedStore].
     */
    public fun build(contract: KeyedSuspendingContract<Key, T>): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] using individual lambdas for remote fetch and local storage.
     *
     * @param onFetch suspending function that fetches remote data for the given [Key].
     * @param onSaveToStorage suspending function that persists fetched data locally for the given [Key].
     * @param onLoadFromStorage suspending function that loads cached data from local storage for
     *   the given [Key]; returns `null` if no cached data is available.
     * @return the configured [KeyedStore].
     */
    public fun build(
        onFetch: suspend (Key) -> T,
        onSaveToStorage: suspend (Key, T) -> Unit,
        onLoadFromStorage: suspend (Key) -> T?,
    ): KeyedStore<Key, T>
}

/**
 * Builder for creating a [KeyedStore] backed by reactive (Flow-based) local storage.
 *
 * @param Key the type used to identify individual items in the store.
 * @param T the type of data held by the store.
 */
public interface KeyedReactiveBuilder<Key : Any, T : Any> : BaseBuilder<KeyedReactiveBuilder<Key, T>> {

    /**
     * Configure a keyed store without fetcher. In this case, the store manages only
     * local data via reactive flow (for example, Room or DataStore returning Flow of items).
     */
    public fun disableFetcher(): KeyedReactiveNoFetcherBuilder<Key, T>

    /**
     * Builds a [KeyedStore] using the provided [KeyedReactiveContract] implementation.
     *
     * @param contract the contract defining remote fetch and reactive local storage operations.
     * @return the configured [KeyedStore].
     */
    public fun build(contract: KeyedReactiveContract<Key, T>): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] using individual lambdas for remote fetch and reactive local storage.
     *
     * @param onFetch suspending function that fetches remote data for the given [Key].
     * @param onSaveToStorage suspending function that persists fetched data locally for the given [Key].
     * @param onObserveStorage function that returns a [Flow] emitting cached values for the given [Key];
     *   emits `null` when no cached data is available.
     * @return the configured [KeyedStore].
     */
    public fun build(
        onFetch: suspend (Key) -> T,
        onSaveToStorage: suspend (Key, T) -> Unit,
        onObserveStorage: (Key) -> Flow<T?>,
    ): KeyedStore<Key, T>
}

/**
 * A builder for a keyed store that has no remote fetcher and manages only local data
 * observed reactively via a [Flow].
 *
 * @param Key the type of the key identifying each item.
 * @param T the type of data managed by the store.
 */
public interface KeyedReactiveNoFetcherBuilder<Key : Any, T : Any> :
    BaseBuilder<KeyedReactiveNoFetcherBuilder<Key, T>> {

    /**
     * Builds a [KeyedStore] using the provided [KeyedReactiveNoFetcherContract] implementation.
     *
     * @param contract the contract defining reactive local storage observations.
     * @return the configured [KeyedStore].
     */
    public fun build(contract: KeyedReactiveNoFetcherContract<Key, T>): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] using only a lambda for observing local data (no remote fetches).
     *
     * @param onObserve function that, for a given key, returns a [Flow] emitting the
     *   locally stored values.
     * @return the configured [KeyedStore].
     */
    public fun build(onObserve: (Key) -> Flow<T>): KeyedStore<Key, T>
}
