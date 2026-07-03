package com.elveum.store.internal.stores.common

import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.store.exceptions.NoCachedDataException
import com.elveum.store.load.LoadRequestSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

internal interface CoreLoaderDelegate<R : Any> {
    suspend fun processDataLoad(
        emitter: CoreEmitter<R>,
        requestSource: LoadRequestSource,
        fetcher: suspend () -> R,
        loader: suspend () -> R?,
        observer: () -> Flow<R?>,
        saver: suspend (R) -> Unit,
    )
}

/**
 * Default [CoreLoaderDelegate] implementing the cache-then-remote loading algorithm
 * shared by all store types:
 *
 * - [LoadRequestSource.Default]: emit the cached value first (from `loader`, falling back
 *   to the first value of `observer`), then fetch and emit the remote value, saving it if
 *   it differs from the cached one.
 * - [LoadRequestSource.Fresh]: skip caches entirely and emit only the remote value.
 * - [LoadRequestSource.Offline]: emit only the cached value; never fetch. If there is no
 *   cached value, throw [NoCachedDataException].
 */
internal class DefaultCoreLoaderDelegate<R : Any> : CoreLoaderDelegate<R> {

    override suspend fun processDataLoad(
        emitter: CoreEmitter<R>,
        requestSource: LoadRequestSource,
        fetcher: suspend () -> R,
        loader: suspend () -> R?,
        observer: () -> Flow<R?>,
        saver: suspend (R) -> Unit,
    ) {
        val shouldLoadFromCache = requestSource != LoadRequestSource.Fresh
        val shouldLoadFromRemote = requestSource != LoadRequestSource.Offline

        val cachedValue = if (shouldLoadFromCache) {
            val isLastValue = !shouldLoadFromRemote
            (loader() ?: observer().firstOrNull())?.also {
                emitter.emit(it, LocalSourceType, isLastValue)
            }
        } else {
            null
        }

        if (shouldLoadFromRemote) {
            val fetchedValue = fetcher()
            emitter.emit(fetchedValue, RemoteSourceType, isLastValue = true)
            if (cachedValue != fetchedValue) {
                saver(fetchedValue)
            }
        } else if (cachedValue == null) {
            throw NoCachedDataException()
        }
    }
}
