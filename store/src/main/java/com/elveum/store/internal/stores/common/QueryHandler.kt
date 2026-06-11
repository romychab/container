@file:OptIn(FlowPreview::class)

package com.elveum.store.internal.stores.common

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.LoadRequestSource
import com.elveum.store.stores.base.BaseStore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce

internal class QueryHandler<Q : Any>(
    private val initialQuery: Q,
    private val queryDebounceMillis: Long,
    private val baseStore: BaseStore<*>,
) {

    @Volatile
    private var lastLoadRequest: LoadRequest? = null

    val queryFlow = MutableStateFlow(initialQuery)

    private val asyncQueryRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    suspend fun submitQuery(query: Q, loadRequest: LoadRequest) {
        queryFlow.value = query
        lastLoadRequest = loadRequest
        delay(queryDebounceMillis)
        if (queryFlow.value == query && lastLoadRequest == loadRequest) {
            baseStore.invalidate(loadRequest)
        }
    }

    fun submitQueryAsync(query: Q, loadRequest: LoadRequest) {
        queryFlow.value = query
        lastLoadRequest = loadRequest
        asyncQueryRequests.tryEmit(Unit)
    }

    fun handleObserveRequest(request: LoadRequest) {
        if (lastLoadRequest != request || request.requestSource == LoadRequestSource.Fresh) {
            lastLoadRequest = request
            baseStore.invalidateAsync(request)
        }
    }

    suspend fun observeAsyncQueryRequests() {
        asyncQueryRequests
            .debounce(queryDebounceMillis)
            .collect {
                lastLoadRequest?.let { baseStore.invalidateAsync(it) }
            }
    }
}
