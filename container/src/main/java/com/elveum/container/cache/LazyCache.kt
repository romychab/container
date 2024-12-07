package com.elveum.container.cache

import com.elveum.container.Container
import com.elveum.container.Emitter
import com.elveum.container.subject.LazyFlowSubject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

public typealias CacheValueLoader<Arg, T> = suspend Emitter<T>.(Arg) -> Unit

/**
 * [LazyCache] works in the same way as [LazyFlowSubject] but additionally it can hold
 * multiple cached values for different arguments.
 */
public interface LazyCache<Arg, T> {

    /**
     * Get the current status of loading data.
     *
     * This method returns [Container.Pending] if no one collects a flow returned
     * by [listen] call with the specified [arg].
     */
    public fun get(arg: Arg): Container<T>

    /**
     * Whether the value is being loaded right now.
     *
     * Please note, value is loading only when at least 1 collector starts collecting
     * a flow returned by [listen].
     */
    public fun isValueLoading(arg: Arg): StateFlow<Boolean>

    /**
     * Get the total number of active collectors.
     * Collector is active if it is currently subscribed to a flow returned by [listen] call.
     */
    public fun getActiveCollectorsCount(arg: Arg): Int

    /**
     * Whether there is at least 1 active collector or not.
     * @see getActiveCollectorsCount
     */
    public fun hasActiveCollectors(arg: Arg): Boolean {
        return getActiveCollectorsCount(arg) > 0
    }

    /**
     * Get a flow for listening values. The load function is started automatically
     * when at least 1 collector starts collecting the flow. See [LazyFlowSubject]
     * for more details.
     */
    public fun listen(arg: Arg): Flow<Container<T>>

    /**
     * Invalidate and reload data.
     *
     * This method does not have effect if there is no active collectors (because data
     * loading is started when at least 1 collector collects a flow returned by [listen] call).
     *
     * @param silently set to TRUE if you don't want to emit [Container.Pending] to a flow returned by [listen] call.
     *
     * @return a finite flow emitting loaded items; it may complete with error if load
     *         function throws an exception.
     */
    public fun reload(arg: Arg, silently: Boolean = false): Flow<T>

    /**
     * Put the specified [container] value into the cache.
     *
     * This method does not have effect if there is no active collectors. In this case,
     * [container] arg is ignored as the most actual value will be loaded automatically
     * when at least 1 collector appears and starts collecting a flow returned by [listen].
     */
    public fun updateWith(arg: Arg, container: Container<T>)

    public companion object {

        /**
         * Create a new instance of [LazyCache].
         *
         * Usage example:
         *
         * ```
         * val lazyCache: LazyCache<Long, User> = LazyCache.create { id ->
         *     val localUser = localDataSource.getById(id)
         *     if (localUser != null) emit(localUser)
         *     val remoteUser = remoteDataSource.getById(id)
         *     localDataSource.saveUser(remoteUser)
         *     emit(remoteUser)
         * }
         * ```
         *
         * @param cacheTimeoutMillis how much time cached values remain in cache if there is no collectors
         * @param loadingDispatcher dispatcher used for loading data
         * @param loader function that loads data into the cache on demand
         */
        public fun <Arg, T> create(
            cacheTimeoutMillis: Long = 1000L,
            loadingDispatcher: CoroutineDispatcher = Dispatchers.IO,
            loader: CacheValueLoader<Arg, T>,
        ): LazyCache<Arg, T> {
            return LazyCacheImpl(
                cacheTimeoutMillis = cacheTimeoutMillis,
                coroutineScopeFactory = {
                    CoroutineScope(SupervisorJob() + loadingDispatcher)
                },
                loader = loader,
            )
        }
    }
}