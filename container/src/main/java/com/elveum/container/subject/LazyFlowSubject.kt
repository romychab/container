package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.Emitter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
    public fun listen(): Flow<Container<T>>

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
     * @param once if set to TRUE and there has been at least one loader without this flag
     *             then this loader will be executed only once and then it will be replaced
     *             back to the previous loader
     *
     * @return Flow with values only emitted by the [valueLoader].
     *         The flow completes when the last value is emitted or when the
     *         load has been failed or cancelled (e.g. all listeners have
     *         been unsubscribed or a new load has been submitted)
     */
    public fun newLoad(
        silently: Boolean = false,
        once: Boolean = false,
        valueLoader: ValueLoader<T>
    ): Flow<T>

    /**
     * Start a new load which will replace existing value in the flow
     * returned by [listen].
     * The load is managed by [valueLoader] which can emit more than one value.
     *
     * Use this method instead of [newLoad] if you don't want to listen for the
     * current load results.
     *
     * @param silently if set to TRUE, [Container.Pending] is not emitted by the [listen] flow.
     * @param once if set to TRUE and there has been at least one loader without this flag
     *             then this loader will be executed only once and then it will be replaced
     *             back to the previous loader
     *
     * Please note that the load starts only when at least one subscriber listens
     * for flow returned by [listen] method.
     */
    public fun newAsyncLoad(
        silently: Boolean = false,
        once: Boolean = false,
        valueLoader: ValueLoader<T>
    )

    /**
     * Update the value immediately in a flow returned by [listen].
     *
     * This method cancels the current load.
     */
    public fun updateWith(container: Container<T>)

    /**
     * Update the value immediately in a flow returned by [listen] by
     * using the [updater] function which accepts an old value in arguments.
     *
     * This method cancels the current load.
     */
    public fun updateWith(updater: (Container<T>) -> Container<T>)

    /**
     * Whether the container is loading data or not. The flow emits TRUE
     * if the value loader is loading data right now. Please note that
     * assigning just a simple [Container.Pending] to the subject via
     * [updateWith] call doesn't affect the flow returned by this method.
     */
    public fun isValueLoading(): StateFlow<Boolean>

    public companion object {

        public fun <T> create(
            cacheTimeoutMillis: Long = 1000L,
            loadingDispatcher: CoroutineDispatcher = Dispatchers.IO,
            valueLoader: ValueLoader<T>? = null,
        ): LazyFlowSubject<T> {
            val configuration = FlowSubjects.defaultConfiguration
            return configuration.lazyFlowSubjectFactory.create<T>(
                cacheTimeoutMillis, loadingDispatcher, CreatorImpl
            ).also {
                if (valueLoader != null) {
                    it.newAsyncLoad(valueLoader = valueLoader)
                }
            }
        }

    }

}