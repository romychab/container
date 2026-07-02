package com.elveum.store.builders.base

import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.store.load.LoadRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    /**
     * Sets a reactive stream of load requests for the store. The store observes this
     * [flow] and applies the most recently emitted [LoadRequest] as its current default
     * request. Whenever `observe` is called without an explicit request (i.e. with
     * `null`), the latest value from this flow is used as the fallback.
     *
     * This overload lets the default request change over time (for example, switching to
     * offline mode when connectivity is lost). For a fixed request, use the
     * [setLoadRequest] overload that accepts a single [LoadRequest].
     *
     * @param flow a [Flow] emitting the load requests to apply; the latest emission is
     *   used as the current default. When no value has been emitted yet,
     *   [LoadRequest.Default] is used.
     * @return this builder instance for fluent chaining.
     */
    public fun setLoadRequest(flow: Flow<LoadRequest>): OutBuilder

    /**
     * Sets a fixed default [LoadRequest] for the store. This value is used for the initial
     * load and, for methods that accept a nullable request (such as `observe`), as the
     * fallback whenever they are invoked without an explicit request.
     *
     * Convenience overload equivalent to `setLoadRequest(flowOf(loadRequest))`.
     *
     * @param loadRequest the default load request to apply.
     * @return this builder instance for fluent chaining.
     */
    public fun setLoadRequest(loadRequest: LoadRequest): OutBuilder {
        return setLoadRequest(flowOf(loadRequest))
    }
}
