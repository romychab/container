package com.elveum.store.builders

import com.elveum.container.Emitter
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.SimpleQueryContract
import com.elveum.store.contracts.SimpleQueryReactiveContract
import com.elveum.store.contracts.SimpleQueryReactiveNoFetcherContract
import com.elveum.store.contracts.SimpleQuerySuspendingContract
import com.elveum.store.stores.simple.SimpleStore
import kotlinx.coroutines.flow.Flow

/**
 * Builder for a remote-only [SimpleStore] whose query is driven by an external [Flow].
 *
 * Created via `SimpleBuilder.withQuery { flow }`. The store follows the supplied query flow and
 * exposes no query API of its own.
 *
 * @param Q the type representing the query.
 * @param T the type of data held by the store.
 */
public interface SimpleExternalQueryBuilder<Q : Any, T : Any> : BaseBuilder<SimpleExternalQueryBuilder<Q, T>> {

    /**
     * Transitions the builder to a variant that supports a suspending (one-shot) local storage
     * layer, preserving the configured external query flow.
     */
    public fun addSuspendingLocalStorage(): SimpleExternalQuerySuspendingBuilder<Q, T>

    /**
     * Transitions the builder to a variant that supports a reactive (Flow-based) local storage
     * layer, preserving the configured external query flow.
     */
    public fun addReactiveLocalStorage(): SimpleExternalQueryReactiveBuilder<Q, T>

    /**
     * Configures a fetcher-less variant that observes only local reactive data, preserving the
     * configured external query flow.
     */
    public fun disableFetcher(): SimpleExternalQueryReactiveNoFetcherBuilder<Q, T>

    /**
     * Transitions the builder into a keyed variant. The configured external query flow is shared
     * by every key (each key follows the same query stream). For per-key query flows, call
     * `withKeys()` before `withQuery { key -> ... }` instead.
     *
     * @param Key the type of the keys managed by the resulting store.
     */
    public fun <Key : Any> withKeys(): SimpleKeyedExternalQueryBuilder<Key, Q, T>

    /**
     * Builds a [SimpleStore] using the provided [SimpleQueryContract] implementation.
     */
    public fun build(contract: SimpleQueryContract<Q, T>): SimpleStore<T>

    /**
     * Builds a [SimpleStore] using a single lambda that fetches remote data for the current query [Q].
     */
    public fun build(onFetch: suspend (Q) -> T): SimpleStore<T>

    /**
     * Builds a [SimpleStore] with a custom loader that emits values manually through an [Emitter],
     * receiving the current query [Q].
     */
    public fun buildCustom(loader: suspend Emitter<T>.(Q) -> Unit): SimpleStore<T>
}

/**
 * Builder for a [SimpleStore] backed by suspending (one-shot) local storage whose query is driven
 * by an external [Flow]. Created via `SimpleSuspendingBuilder.withQuery { flow }`.
 *
 * @param Q the type representing the query.
 * @param T the type of data held by the store.
 */
public interface SimpleExternalQuerySuspendingBuilder<Q : Any, T : Any> :
    BaseBuilder<SimpleExternalQuerySuspendingBuilder<Q, T>> {

    /**
     * Transitions the builder into a keyed variant with suspending local storage. The configured
     * external query flow is shared by every key.
     *
     * @param Key the type of the keys managed by the resulting store.
     */
    public fun <Key : Any> withKeys(): SimpleKeyedExternalQuerySuspendingBuilder<Key, Q, T>

    /**
     * Builds a [SimpleStore] using the provided [SimpleQuerySuspendingContract] implementation.
     */
    public fun build(contract: SimpleQuerySuspendingContract<Q, T>): SimpleStore<T>

    /**
     * Builds a [SimpleStore] using individual lambdas for remote fetch and suspending local storage.
     */
    public fun build(
        onFetch: suspend (Q) -> T,
        onSaveToStorage: suspend (Q, T) -> Unit,
        onLoadFromStorage: suspend (Q) -> T?,
    ): SimpleStore<T>
}

/**
 * Builder for a [SimpleStore] backed by reactive (Flow-based) local storage whose query is driven
 * by an external [Flow]. Created via `SimpleReactiveBuilder.withQuery { flow }`.
 *
 * @param Q the type representing the query.
 * @param T the type of data held by the store.
 */
public interface SimpleExternalQueryReactiveBuilder<Q : Any, T : Any> :
    BaseBuilder<SimpleExternalQueryReactiveBuilder<Q, T>> {

    /**
     * Configures a fetcher-less variant that observes only local reactive data, preserving the
     * configured external query flow.
     */
    public fun disableFetcher(): SimpleExternalQueryReactiveNoFetcherBuilder<Q, T>

    /**
     * Transitions the builder into a keyed variant with reactive local storage. The configured
     * external query flow is shared by every key.
     *
     * @param Key the type of the keys managed by the resulting store.
     */
    public fun <Key : Any> withKeys(): SimpleKeyedExternalQueryReactiveBuilder<Key, Q, T>

    /**
     * Builds a [SimpleStore] using the provided [SimpleQueryReactiveContract] implementation.
     */
    public fun build(contract: SimpleQueryReactiveContract<Q, T>): SimpleStore<T>

    /**
     * Builds a [SimpleStore] using individual lambdas for remote fetch and reactive local storage.
     */
    public fun build(
        onFetch: suspend (Q) -> T,
        onSaveToStorage: suspend (Q, T) -> Unit,
        onObserveStorage: (Q) -> Flow<T?>,
    ): SimpleStore<T>
}

/**
 * Builder for a fetcher-less [SimpleStore] that observes local data reactively, parameterized by a
 * query driven by an external [Flow]. Created via `SimpleReactiveNoFetcherBuilder.withQuery { flow }`.
 *
 * @param Q the type representing the query.
 * @param T the type of data held by the store.
 */
public interface SimpleExternalQueryReactiveNoFetcherBuilder<Q : Any, T : Any> :
    BaseBuilder<SimpleExternalQueryReactiveNoFetcherBuilder<Q, T>> {

    /**
     * Transitions the builder into a keyed, fetcher-less variant. The configured external query
     * flow is shared by every key.
     *
     * @param Key the type of the keys managed by the resulting store.
     */
    public fun <Key : Any> withKeys(): SimpleKeyedExternalQueryReactiveNoFetcherBuilder<Key, Q, T>

    /**
     * Builds a [SimpleStore] using the provided [SimpleQueryReactiveNoFetcherContract] implementation.
     */
    public fun build(contract: SimpleQueryReactiveNoFetcherContract<Q, T>): SimpleStore<T>

    /**
     * Builds a [SimpleStore] using only a lambda for observing local data for the current query [Q].
     */
    public fun build(onObserve: (Q) -> Flow<T>): SimpleStore<T>
}
