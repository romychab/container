package com.elveum.store.builders

import com.elveum.container.Emitter
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleKeyedQueryContract
import com.elveum.store.contracts.SimpleKeyedQueryReactiveContract
import com.elveum.store.contracts.SimpleKeyedQueryReactiveNoFetcherContract
import com.elveum.store.contracts.SimpleKeyedQuerySuspendingContract
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.flow.Flow

/**
 * Builder for a remote-only keyed [KeyedStore] whose per-key query is driven by an external [Flow].
 *
 * Created via `SimpleKeyedBuilder.withQuery { key -> flow }`. Each key follows its own query flow;
 * the resulting store exposes no query API of its own.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of value cached per key.
 */
public interface SimpleKeyedExternalQueryBuilder<Key : Any, Q : Any, T : Any> :
    BaseBuilder<SimpleKeyedExternalQueryBuilder<Key, Q, T>> {

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot) local storage
     * layer, preserving the configured per-key external query flow.
     */
    public fun addSuspendingLocalStorage(): SimpleKeyedExternalQuerySuspendingBuilder<Key, Q, T>

    /**
     * Transitions the builder to a variant that supports a reactive (Flow-based) local storage
     * layer, preserving the configured per-key external query flow.
     */
    public fun addReactiveLocalStorage(): SimpleKeyedExternalQueryReactiveBuilder<Key, Q, T>

    /**
     * Configures a fetcher-less variant that observes only local reactive data per key, preserving
     * the configured per-key external query flow.
     */
    public fun disableFetcher(): SimpleKeyedExternalQueryReactiveNoFetcherBuilder<Key, Q, T>

    /**
     * Builds a [KeyedStore] using the provided [SimpleKeyedQueryContract] implementation.
     */
    public fun build(contract: SimpleKeyedQueryContract<Key, Q, T>): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] using a single lambda that fetches remote data for a key and query.
     */
    public fun build(onFetch: suspend (Key, Q) -> T): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] with a custom loader that emits values manually through an [Emitter],
     * receiving the key and the current query [Q].
     */
    public fun buildCustom(loader: suspend Emitter<T>.(Key, Q) -> Unit): KeyedStore<Key, T>
}

/**
 * Builder for a keyed [KeyedStore] backed by suspending local storage whose per-key query is
 * driven by an external [Flow]. Created via `SimpleKeyedSuspendingBuilder.withQuery { key -> flow }`.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of value cached per key.
 */
public interface SimpleKeyedExternalQuerySuspendingBuilder<Key : Any, Q : Any, T : Any> :
    BaseBuilder<SimpleKeyedExternalQuerySuspendingBuilder<Key, Q, T>> {

    /**
     * Builds a [KeyedStore] using the provided [SimpleKeyedQuerySuspendingContract] implementation.
     */
    public fun build(contract: SimpleKeyedQuerySuspendingContract<Key, Q, T>): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] using individual lambdas for remote fetch and suspending local storage.
     */
    public fun build(
        onFetch: suspend (Key, Q) -> T,
        onSaveToStorage: suspend (Key, Q, T) -> Unit,
        onLoadFromStorage: suspend (Key, Q) -> T?,
    ): KeyedStore<Key, T>
}

/**
 * Builder for a keyed [KeyedStore] backed by reactive local storage whose per-key query is driven
 * by an external [Flow]. Created via `SimpleKeyedReactiveBuilder.withQuery { key -> flow }`.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of value cached per key.
 */
public interface SimpleKeyedExternalQueryReactiveBuilder<Key : Any, Q : Any, T : Any> :
    BaseBuilder<SimpleKeyedExternalQueryReactiveBuilder<Key, Q, T>> {

    /**
     * Configures a fetcher-less variant that observes only local reactive data per key, preserving
     * the configured per-key external query flow.
     */
    public fun disableFetcher(): SimpleKeyedExternalQueryReactiveNoFetcherBuilder<Key, Q, T>

    /**
     * Builds a [KeyedStore] using the provided [SimpleKeyedQueryReactiveContract] implementation.
     */
    public fun build(contract: SimpleKeyedQueryReactiveContract<Key, Q, T>): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] using individual lambdas for remote fetch and reactive local storage.
     */
    public fun build(
        onFetch: suspend (Key, Q) -> T,
        onSaveToStorage: suspend (Key, Q, T) -> Unit,
        onObserveStorage: (Key, Q) -> Flow<T?>,
    ): KeyedStore<Key, T>
}

/**
 * Builder for a fetcher-less keyed [KeyedStore] that observes local data reactively per key,
 * parameterized by a per-key query driven by an external [Flow]. Created via
 * `SimpleKeyedReactiveNoFetcherBuilder.withQuery { key -> flow }`.
 *
 * @param Key the type of the keys managed by the store.
 * @param Q the type representing the query.
 * @param T the type of value cached per key.
 */
public interface SimpleKeyedExternalQueryReactiveNoFetcherBuilder<Key : Any, Q : Any, T : Any> :
    BaseBuilder<SimpleKeyedExternalQueryReactiveNoFetcherBuilder<Key, Q, T>> {

    /**
     * Builds a [KeyedStore] using the provided [SimpleKeyedQueryReactiveNoFetcherContract]
     * implementation.
     */
    public fun build(contract: SimpleKeyedQueryReactiveNoFetcherContract<Key, Q, T>): KeyedStore<Key, T>

    /**
     * Builds a [KeyedStore] using only a lambda for observing local data for a key and query.
     */
    public fun build(onObserve: (Key, Q) -> Flow<T>): KeyedStore<Key, T>
}
