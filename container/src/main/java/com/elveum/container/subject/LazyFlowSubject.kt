package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.Emitter
import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.container.factory.DefaultCacheTimeoutMillis
import com.elveum.container.subject.LazyFlowSubject.Companion.create
import com.elveum.container.subject.lazy.LoadTaskManager
import com.elveum.container.subject.transformation.ContainerTransformation
import com.elveum.container.subject.transformation.EmptyContainerTransformation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Loader function for [LazyFlowSubject] which can emit loaded values.
 */
public typealias ValueLoader<T> = suspend Emitter<T>.() -> Unit

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
     * @param silently if set to TRUE, [Container.Pending] is not emitted by the [listen] flow.
     *
     * @return Flow with values only emitted by the [valueLoader].
     *         The flow completes when the last value is emitted or when the
     *         load has been failed or cancelled (e.g. all listeners have
     *         been unsubscribed or a new load has been submitted)
     */
    public fun newLoad(
        silently: Boolean = false,
        valueLoader: ValueLoader<T>
    ): Flow<T>

    /**
     * Update the value immediately in a flow returned by [listen].
     *
     * This method cancels the current load.
     */
    public fun updateWith(container: Container<T>)

    /**
     * The same as [newLoad] but using the previous loader function
     * to update a value held by this subject.
     * @see newLoad
     */
    public fun reload(silently: Boolean = false): Flow<T>

    public companion object {

        public fun <T> create(
            cacheTimeoutMillis: Long = DefaultCacheTimeoutMillis,
            coroutineScopeFactory: CoroutineScopeFactory = CoroutineScopeFactory,
            transformation: ContainerTransformation<T> = EmptyContainerTransformation(),
            valueLoader: ValueLoader<T>? = null,
        ): LazyFlowSubject<T> {
            return LazyFlowSubjectImpl(
                coroutineScopeFactory = coroutineScopeFactory,
                cacheTimeoutMillis = cacheTimeoutMillis,
                loadTaskManager = LoadTaskManager(transformation),
            ).apply {
                if (valueLoader != null) {
                    newAsyncLoad(valueLoader = valueLoader)
                }
            }
        }

    }

}
