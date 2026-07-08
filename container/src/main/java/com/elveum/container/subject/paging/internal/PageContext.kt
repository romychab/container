package com.elveum.container.subject.paging.internal

import com.elveum.container.BackgroundLoadMetadata
import com.elveum.container.BackgroundLoadState
import com.elveum.container.ContainerMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import com.elveum.container.update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class PageContext<Key, T>(
    private val state: PageRecordsState<Key, T>,
    private val loadConfig: LoadConfig,
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
        if (loadConfig.isSilentLoadingEnabled) {
            state.updateRecord(
                pageIndex = pageIndex,
                pageKey = pageKey,
                container = { old ->
                    old.update { backgroundLoadState = BackgroundLoadState.Idle }
                }
            )
        }
    }

    suspend fun onLoadFailed(e: Exception) {
        state.updateRecord(
            pageIndex = pageIndex,
            pageKey = pageKey,
            container = { errorContainer(e) },
        )
    }

    suspend fun onPageDataLoaded(items: List<T>, metadata: ContainerMetadata) {
        val container = if (loadConfig.isSilentLoadingEnabled) {
            val bgLoadState = if (pageIndex == 0) {
                // report background loading only for the first page
                BackgroundLoadState.Loading
            } else {
                BackgroundLoadState.Idle
            }
            val bgLoadMetadata = BackgroundLoadMetadata(bgLoadState)
            successContainer(items, metadata + bgLoadMetadata)
        } else {
            successContainer(items, metadata)
        }
        state.updateRecord(
            pageIndex = pageIndex,
            pageKey = pageKey,
            container = { container },
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
