package com.elveum.store.internal.stores.common

import com.elveum.container.Emitter
import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.store.exceptions.NoCachedDataException
import com.elveum.store.load.LoadRequestSource
import com.elveum.store.internal.load.loadRequestSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

internal suspend fun <Q : Any, T : Any> Emitter<T>.processDataLoad(
    query: Q,
    fetcher: suspend (Q) -> T,
    loader: suspend (Q) -> T?,
    saver: suspend (Q, T) -> Unit,
    observer: (Q) -> Flow<T?>,
    loadRequestSource: LoadRequestSource = metadata.loadRequestSource
) {
    val shouldLoadFromCache = loadRequestSource != LoadRequestSource.Fresh
    val shouldLoadFromRemote = loadRequestSource != LoadRequestSource.Offline

    val cachedValue = if (shouldLoadFromCache) {
        val isLastValue = !shouldLoadFromRemote
        (loader(query) ?: observer(query).firstOrNull())?.also {
            emit(it, LocalSourceType, isLastValue)
        }
    } else {
        null
    }

    if (shouldLoadFromRemote) {
        val fetchedValue = fetcher(query)
        emit(fetchedValue, RemoteSourceType, isLastValue = true)
        if (cachedValue != fetchedValue) {
            saver(query, fetchedValue)
        }
    } else if (cachedValue == null) {
        throw NoCachedDataException()
    }
}
