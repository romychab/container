package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.Emitter
import com.elveum.container.EmptyMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.container.factory.DEFAULT_CACHE_TIMEOUT_MILLIS
import com.elveum.container.factory.DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS
import com.elveum.container.subject.LazyFlowSubject.Companion.create
import com.elveum.container.subject.lazy.LoadTaskManager
import com.elveum.container.subject.transformation.ContainerTransformation
import com.elveum.container.subject.transformation.EmptyContainerTransformation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the infinite flow which acts as an async container for
 * loading data and listening the current state of loading process.
 *
 * Loader starts loading data lazily when at least
 * one subscriber starts collecting the flow returned by [listen] method.
 *
 * A value loader can be assigned or replaced by using the following
 * methods:
 * - [newLoad] and [newAsyncLoad] if you need to load and emit more than 1 value
 * - [newSimpleLoad] and [newSimpleAsyncLoad] if you need to load only one value
 *
 * Whenever you start a new load, an old load is cancelled and replaced by a new one.
 *
 * Please note that the load is not started immediately if there are no
 * active collectors on a flow returned by [listen] call. So any call of
 * [newLoad], [newAsyncLoad], [newSimpleAsyncLoad] and [newSimpleLoad] doesn't
 * take effect immediately if there is no active collectors on [listen] flow.
 *
 * Also it's possible to update the value directly by calling [updateWith] method.
 *
 * The latest value is cached until the last subscriber stops collecting the flow.
 * There may be a timeout after the last subscriber stops collecting. If a new
 * subscriber starts collecting the flow before timeout expires, an old cached
 * value is passed to a new subscriber and the load doesn't start from scratch.
 * Timeout is specified either by constructor or by [create] method.
 */
@Suppress("ComplexInterface")
public interface LazyFlowSubject<T> {

    /**
     * Get the total number of active collectors.
     *
     * Pleas note, collector is active only after calling a terminal operator
     * (such as `collect`) on the flow returned by [listen].
     */
    public val activeCollectorsCount: Int

    /**
     * Whether there is at least one active collector.
     * @see activeCollectorsCount
     */
    public val hasActiveCollectors: Boolean get() = activeCollectorsCount > 0

    /**
     * Get the current value held by this subject.
     */
    public fun currentValue(
        configuration: ContainerConfiguration = ContainerConfiguration(),
    ): Container<T>

    /**
     * Listen for values loaded by this subject.
     *
     * Value is loaded automatically when at least 1 subscriber starts
     * collecting the returned flow. The loaded value is cached so when
     * new subscribers start collecting the flow they receive the cached
     * value. Also only one load is active at a time so it's safe to
     * start collecting the flow by 2 or more subscribers at a time. Only the first
     * subscriber triggers a real load. All other subscribers will receive
     * a cached value.
     *
     * The load logic can be assigned by [newLoad], [newAsyncLoad], [newSimpleLoad]
     * or [newSimpleAsyncLoad] calls.
     *
     * @return infinite flow which emits the current state of value load, always success, exceptions are wrapped to [Container.Error]
     */
    public fun listen(
        configuration: ContainerConfiguration = ContainerConfiguration(),
    ): StateFlow<Container<T>>

    /**
     * Spy on the subject's state. Subscribing to the flow returned by this
     * call does not trigger data loading. If there is no real listeners
     * subscribed via [listen] call, the returned flow emits Pending state.
     */
    public fun spy(
        configuration: ContainerConfiguration = ContainerConfiguration(),
    ): StateFlow<Container<T>>

    /**
     * Start a new load which will replace existing value in the flow
     * returned by [listen].
     *
     * The [valueLoader] can emit more than one value by using `emit`
     * method provided by [Emitter].
     *
     * Please note that the load starts only when at least one subscriber listens
     * for the flow returned by [listen] method. Otherwise the flow returned by [newLoad]
     * doesn't emit values and waits until the first subscriber starts collecting
     * the flow returned by [listen] method.
     *
     * Also the returned flow is cancelled if the last subscriber stops collecting
     * the flow returned by [listen] method.
     *
     * @param config defines how the loading state will be propagated to subsequent containers.
     * @param metadata Additional metadata values to be attached to subsequent emitted containers.
     * @param valueLoader A loader that loads new values asynchronously.
     *
     * @return Flow with values only emitted by the [valueLoader].
     *         The flow completes when the last value is emitted or when the
     *         load has been failed or cancelled (e.g. all listeners have
     *         been unsubscribed or a new load has been submitted)
     */
    public fun newLoad(
        config: LoadConfig? = null,
        metadata: ContainerMetadata = EmptyMetadata,
        valueLoader: ValueLoader<T>
    ): Flow<T>

    /**
     * Update the value immediately in a flow returned by [listen].
     *
     * This method cancels the current load.
     */
    public fun updateWith(container: Container<T>)

    /**
     * Atomically compare the [currentValue] with [expected] value and
     * update it with [updated] value.
     *
     * @return `true` if the [currentValue] is set to [updated].
     */
    public fun compareAndSet(
        configuration: ContainerConfiguration = ContainerConfiguration(),
        expected: Container<T>,
        updated: Container<T>,
    ): Boolean

    /**
     * The same as [newLoad] but using the previous loader function
     * to update a value held by this subject.
     * @see newLoad
     */
    public fun reload(
        config: LoadConfig? = null,
        metadata: ContainerMetadata = EmptyMetadata,
    ): Flow<T>

    /**
     * Launch the [block] when at least one observer subscribes to
     * a flow returned by [listen] call. The [block] is automatically
     * cancelled when the last observer unsubscribes from the flow.
     *
     * @param spyMode Whether the `listen` call within the `whenActive { ... }` block
     *     acts as `spy` call (enabled by default).
     * @param block A suspending block of code with a CoroutineScope executed
     *     only when the subject has at least 1 active subscriber. The scope
     *     is cancelled when the last subscriber is unsubscribed (after cache timeout)
     *
     * @return this LazyFlowSubject instance.
     */
    public fun whenActive(
        spyMode: Boolean = true,
        block: suspend ScopedLazyFlowSubject<T>.() -> Unit,
    ): LazyFlowSubject<T>

    public companion object {

        /**
         * Create a new [LazyFlowSubject] instance.
         *
         * @param T the type of values held by the subject.
         * @param cacheTimeoutMillis how much time loaded values remain cached when there are no collectors
         * @param reloadDependenciesPeriodMillis how often dependencies are checked for reload triggers
         * @param coroutineScopeFactory factory used to create coroutine scopes for loading
         * @param transformation optional transformation applied to loaded containers
         * @param loadConfig defines how the loading state of the initial load is propagated
         * @param metadata metadata values to be attached to the initial load request
         * @param valueLoader optional function that loads data into the subject; when `null` no initial load is started
         * @return the created [LazyFlowSubject] instance
         */
        public fun <T> create(
            cacheTimeoutMillis: Long = DEFAULT_CACHE_TIMEOUT_MILLIS,
            reloadDependenciesPeriodMillis: Long = DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS,
            coroutineScopeFactory: CoroutineScopeFactory = CoroutineScopeFactory,
            transformation: ContainerTransformation<T> = EmptyContainerTransformation(),
            loadConfig: LoadConfig = LoadConfig.Normal,
            metadata: ContainerMetadata = EmptyMetadata,
            valueLoader: ValueLoader<T>? = null,
        ): LazyFlowSubject<T> {
            return LazyFlowSubjectImpl(
                coroutineScopeFactory = coroutineScopeFactory,
                cacheTimeoutMillis = cacheTimeoutMillis,
                loadTaskManager = LoadTaskManager(transformation),
                reloadDependenciesPeriodMillis = reloadDependenciesPeriodMillis,
            ).apply {
                if (valueLoader != null) {
                    newAsyncLoad(valueLoader = valueLoader, config = loadConfig, metadata = metadata)
                }
            }
        }

    }

}
