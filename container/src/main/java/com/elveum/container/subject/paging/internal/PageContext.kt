package com.elveum.container.subject.paging.internal

import com.elveum.container.ContainerMetadata
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class PageContext<Key, T>(
    private val state: PageRecordsState<Key, T>,
    val isRetry: Boolean,
    private val onScheduleNextKey: suspend (Int, Key) -> Unit,
    initialRecord: ImmutablePageRecord<Key, T>,
) {

    val pageIndex = initialRecord.pageIndex
    val pageKey = initialRecord.pageKey

    suspend fun onLoadStarted(isRetry: Boolean) {
        state.updateRecord(
            pageIndex = pageIndex,
            pageKey = pageKey,
            container = {
                if (isRetry) {
                    pendingContainer()
                } else {
                    it
                }
            },
            isRetry = isRetry,
        )
    }

    suspend fun onLoadCompleted() {
        state.markAsCompleted(pageIndex, pageKey)
    }

    suspend fun onLoadFailed(e: Exception) {
        state.updateRecord(
            pageIndex = pageIndex,
            pageKey = pageKey,
            container = { errorContainer(e) },
        )
    }

    suspend fun onPageDataLoaded(items: List<T>, metadata: ContainerMetadata) {
        state.updateRecord(
            pageIndex = pageIndex,
            pageKey = pageKey,
            container = { successContainer(items, metadata) },
        )
    }

    suspend fun scheduleNextKey(key: Key) {
        onScheduleNextKey(pageIndex + 1, key)
    }

    fun observePageSnapshot(pageIndex: Int): Flow<List<T>> {
        return state.observePageSnapshots(pageIndex)
    }

    fun observeListSnapshots(): StateFlow<List<T>> {
        return state.observeListSnapshots()
    }

    suspend fun isAllPagesCompleted(): Boolean {
        return state.isAllPagesLoaded()
    }

}
