@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.paging.internal

import com.elveum.container.StatefulEmitter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ScopedPageLoader<Key, T>(
    val coroutineScope: CoroutineScope,
    private val originEmitter: StatefulEmitter<List<T>>,
    private val config: PageLoaderConfig<Key, T>,
) {

    private val lastRenderedIndexFlow = MutableStateFlow(0)
    private val sessionCompleteDeferred = CompletableDeferred<Unit>()

    fun loadPage(context: PageContext<Key, T>) {
        coroutineScope.launch {
            if (!context.isRetry) awaitFetchDistanceSatisfied(context)
            val emitter = PageEmitterImpl(
                context = context,
                originEmitter = originEmitter,
            )
            try {
                context.onLoadStarted(context.isRetry)
                config.block(emitter, context.pageKey)
                if (!emitter.isPageEmitted) {
                    sessionCompleteDeferred.completeExceptionally(
                        IllegalStateException("emitPage() must be called at least once.")
                    )
                    return@launch
                }
                completeLoad(context)
            } catch (e: Exception) {
                handleLoadError(context, emitter.isPageEmitted, e)
            }
        }
    }

    fun onItemRendered(index: Int) {
        lastRenderedIndexFlow.update { oldIndex ->
            if (index > oldIndex) {
                index
            } else {
                oldIndex
            }
        }
    }

    private suspend fun handleLoadError(
        context: PageContext<Key, T>,
        isPageEmitted: Boolean,
        e: Exception,
    ) {
        currentCoroutineContext().ensureActive()
        val isSilentErrors = originEmitter.loadConfig.isSilentErrorsEnabled
        if (isSilentErrors && isPageEmitted) {
            // page data already exists, errors can be suppressed by silent flag
            completeLoad(context)
        } else {
            if (context.pageIndex == 0) {
                sessionCompleteDeferred.completeExceptionally(e)
            }
            context.onLoadFailed(e)
        }
    }

    private suspend fun completeLoad(context: PageContext<Key, T>) {
        context.onLoadCompleted()
        if (context.isAllPagesCompleted()) {
            sessionCompleteDeferred.complete(Unit)
        }
    }

    private suspend fun awaitFetchDistanceSatisfied(context: PageContext<Key, T>) {
        if (context.pageIndex == 0) return // immediate load of the initial page

        val pagesFlow = combine(
            context.observeListSnapshots(),
            context.observePageSnapshot(context.pageIndex - 1),
        ) { allItems, prevPageItems -> allItems to prevPageItems }

        val indexOfLastItemOfPageFlow = pagesFlow
            .mapLatest { (allItems, prevPageItems) ->
                val prevPageIds = prevPageItems.map { config.itemId(it) }.toSet()
                allItems
                    .indexOfLast { prevPageIds.contains(config.itemId(it)) }
                    .takeIf { it != -1 }
                    ?: allItems.lastIndex // fallback to the last index of the whole list
                                          // if index of the last item of page hasn't been found
            }
            .distinctUntilChanged()

        val finalFlow = combine(
            indexOfLastItemOfPageFlow,
            lastRenderedIndexFlow
        ) { indexOfLastItemOfPage, lastRenderedIndex -> indexOfLastItemOfPage to lastRenderedIndex }

        finalFlow.first { (indexOfLastItemOfPage, lastRenderedIndex) ->
            val result = lastRenderedIndex > indexOfLastItemOfPage - config.finalFetchDistance
            result
        }
    }

    suspend fun await() {
        sessionCompleteDeferred.await()
    }

}
