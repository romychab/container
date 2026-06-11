package com.elveum.store.builders.base

import com.elveum.container.factory.CoroutineScopeFactory
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/**
 * Base interface for all store builders, providing common configuration options
 * applicable to every store variant (simple, keyed, paged).
 *
 * @param OutBuilder the concrete builder type returned by each configuration method,
 *   enabling fluent method chaining.
 */
public interface BaseBuilder<OutBuilder> {

    /**
     * Sets the expiration time after which the in-memory cache is cleared once
     * the last observer unsubscribes from the store.
     *
     * @param duration how long cached data is kept alive after the last subscriber
     *   disconnects; use [Duration.ZERO] to clear immediately.
     * @return this builder instance for fluent chaining.
     */
    public fun setInMemoryCacheTimeout(duration: Duration): OutBuilder

    /**
     * Sets the coroutine context (e.g. a custom [kotlinx.coroutines.CoroutineDispatcher])
     * on which the store will execute its internal operations.
     *
     * @param context the [CoroutineContext] to use for store operations.
     * @return this builder instance for fluent chaining.
     */
    public fun setCoroutineContext(context: CoroutineContext): OutBuilder

    /**
     * Sets the factory used to create the internal [kotlinx.coroutines.CoroutineScope]
     * that manages load tasks within the store.
     *
     * @param factory the [CoroutineScopeFactory] responsible for creating the store's
     *   internal coroutine scope.
     * @return this builder instance for fluent chaining.
     */
    public fun setCoroutineScopeFactory(factory: CoroutineScopeFactory): OutBuilder

}
