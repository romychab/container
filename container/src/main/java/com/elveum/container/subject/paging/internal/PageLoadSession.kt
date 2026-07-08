package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
import com.elveum.container.StatefulEmitter
import com.elveum.container.subject.paging.PageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class PageLoadSession<Key, T>(
    val coroutineScope: CoroutineScope,
    val originEmitter: StatefulEmitter<List<T>>,
    val config: PageLoaderConfig<Key, T>,
    val onNextPageStateChanged: (PageState) -> Unit
) {

    private val tryAgainRef = ::retryLastPageLoad
    private val onItemRenderedRef = ::onItemRendered

    private val state = PageRecordsState(
        coroutineScope = coroutineScope,
        originEmitter = originEmitter,
        config = config,
        tryAgainRef = tryAgainRef,
        onItemRenderedRef = onItemRenderedRef,
        onNextPageStateChanged = onNextPageStateChanged,
    )

    private val scopedLoader = ScopedPageLoader(
        coroutineScope = coroutineScope,
        originEmitter = originEmitter,
        config = config,
    )

    suspend fun startLoading(
        pageIndex: Int,
        pageKey: Key,
        isRetry: Boolean = false,
    ) {
        val record = state.prepareRecord(pageIndex, pageKey)
        val context = PageContext(
            state = state,
            loadConfig = originEmitter.loadConfig,
            initialRecord = record,
            isRetry = isRetry,
            onScheduleNextKey = { nextIndex, nextKey ->
                startLoading(nextIndex, nextKey)
            }
        )
        scopedLoader.loadPage(context)
    }

    suspend fun await() {
        scopedLoader.await()
    }

    fun intercept(container: Container<List<T>>): Container<List<T>> {
        return state.intercept(container)
    }

    fun onItemRendered(index: Int) {
        scopedLoader.onItemRendered(index)
    }

    private fun retryLastPageLoad() {
        val record = state.firstErrorPageRecord() ?: return
        coroutineScope.launch {
            startLoading(pageIndex = record.pageIndex, pageKey = record.pageKey, isRetry = true)
        }
    }

}
