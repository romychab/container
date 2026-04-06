package com.elveum.container.subject.paging.internal

import com.elveum.container.BackgroundLoadMetadata
import com.elveum.container.BackgroundLoadState
import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.StatefulEmitter
import com.elveum.container.pendingContainer
import com.elveum.container.subject.paging.PageState
import kotlin.coroutines.resume

internal class PageResultsEmitter<Key, T>(
    private val store: PageLoaderRecordStore<Key, T>,
    private val emitter: StatefulEmitter<List<T>>,
    private val metadataProvider: () -> ContainerMetadata,
    private val onNextPageStateChanged: (PageState) -> Unit,
) {

    private val retry = {
        store.forEach { record ->
            record.retryContinuation?.resume(Unit)
            record.retryContinuation = null
        }
    }

    suspend fun emitResults() {
        val containers = store.getAllContainers()
        if (containers.isEmpty() || containers.all { it is Container.Pending }) {
            emitter.emitPendingState()
        } else if (containers.all { it is Container.Error }) {
            val firstError = containers.first() as Container.Error
            emitter.emitFailureState(firstError.exception)
        } else {
            val firstErrorRecord = store.firstOrNull { it.container is Container.Error }
            val pageState = if (firstErrorRecord != null) {
                val exception = (firstErrorRecord.container as Container.Error).exception
                PageState.Error(
                    exception = exception,
                    retry = retry,
                )
            } else if (containers.contains(pendingContainer())) {
                PageState.Pending
            } else {
                PageState.Idle
            }
            onNextPageStateChanged(pageState)
            val outputList = store.buildOutputList()
            emitter.emit(
                value = outputList,
                // override background load state to Idle after outputList emitted, since
                // the pageLoader uses its own PageLoaderState for tracking the current load state:
                metadata = metadataProvider() + BackgroundLoadMetadata(BackgroundLoadState.Idle),
            )
        }
    }

}
