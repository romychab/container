package com.elveum.container.cache

import com.elveum.container.Container
import com.elveum.container.SourceType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public typealias SimpleCacheValueLoader<Arg, T> = suspend (Arg) -> T

/**
 * Update value in a flow returned by [LazyCache.listen].
 *
 * This method cancels the current load.
 *
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
    silently: Boolean = false,
) {
    reload(arg, silently)
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
    cacheTimeoutMillis: Long = 1000L,
    loadingDispatcher: CoroutineDispatcher = Dispatchers.IO,
    loader: SimpleCacheValueLoader<Arg, T>,
): LazyCache<Arg, T> {
    return create(
        cacheTimeoutMillis = cacheTimeoutMillis,
        loadingDispatcher = loadingDispatcher,
    ) { arg ->
        emit(loader.invoke(arg))
    }
}

/**
 * Update value in the cache if the previous value is [Container.Success].
 */
public fun <Arg, T> LazyCache<Arg, T>.updateIfSuccess(
    arg: Arg,
    source: SourceType? = null,
    updater: (T) -> T,
) {
    updateWith(arg) { container ->
        if (container is Container.Success) {
            Container.Success(updater(container.value), source ?: container.source)
        } else {
            container
        }
    }
}
