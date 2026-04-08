package com.elveum.container.cache

import com.elveum.container.Container
import com.elveum.container.LoadConfig
import com.elveum.container.SourceType
import com.elveum.container.factory.DEFAULT_CACHE_TIMEOUT_MILLIS
import com.elveum.container.transform

public typealias SimpleCacheValueLoader<Arg, T> = suspend (Arg) -> T

/**
 * Update value in a flow returned by [LazyCache.listen].
 *
 * This method cancels the current load.
 *
 * @param Arg the type of the argument used to identify cached entries.
 * @param T the type of values held in the cache.
 * @param arg the argument identifying which cached entry to update.
 * @param updater function which makes a new value to be sent into the cache.
 */
public inline fun <Arg, T> LazyCache<Arg, T>.updateWith(
    arg: Arg,
    updater: (Container<T>) -> Container<T>
) {
    val oldValue = get(arg)
    val newValue = updater(oldValue)
    if (newValue == oldValue) return
    updateWith(arg, newValue)
}

/**
 * The same as [LazyCache.reload] but without returning a flow.
 */
public fun <Arg, T> LazyCache<Arg, T>.reloadAsync(
    arg: Arg,
    config: LoadConfig = LoadConfig.Normal,
) {
    @Suppress("UnusedFlow")
    reload(arg, config)
}

/**
 * Create a [LazyCache] instance with simpler load function which can
 * load only one single result.
 *
 * For example:
 *
 * ```
 * val lazyCache: LazyCache<Long, User> = LazyCache.createSimple { id ->
 *   remoteUsersDataSource.getById(id)
 * }
 * ``
 */
public fun <Arg, T> LazyCache.Companion.createSimple(
    cacheTimeoutMillis: Long = DEFAULT_CACHE_TIMEOUT_MILLIS,
    valueLoader: SimpleCacheValueLoader<Arg, T>,
): LazyCache<Arg, T> {
    return create(
        cacheTimeoutMillis = cacheTimeoutMillis,
    ) { arg ->
        emit(valueLoader.invoke(arg))
    }
}

/**
 * Update value in the cache if the previous value is [Container.Success].
 */
public inline fun <Arg, T> LazyCache<Arg, T>.updateIfSuccess(
    arg: Arg,
    sourceType: SourceType? = null,
    updater: (T) -> T,
) {
    updateWith(arg) { oldContainer ->
        oldContainer.transform(
            onSuccess = {
                successContainer(updater(it), sourceType)
            }
        )
    }
}
