package com.elveum.container.subject.paging.internal

import com.elveum.container.BackgroundLoadMetadata
import com.elveum.container.BackgroundLoadState
import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.StatefulEmitter
import com.elveum.container.getOrNull
import com.elveum.container.isCompleted
import com.elveum.container.isError
import com.elveum.container.isSuccess
import com.elveum.container.map
import com.elveum.container.pendingContainer
import com.elveum.container.subject.paging.NextPageStateMetadata
import com.elveum.container.subject.paging.OnItemRenderedCallbackMetadata
import com.elveum.container.subject.paging.PageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PageRecordsState<Key, T>(
    private val coroutineScope: CoroutineScope,
    private val originEmitter: StatefulEmitter<List<T>>,
    private val config: PageLoaderConfig<Key, T>,
    private val tryAgainRef: () -> Unit,
    private val onItemRenderedRef: (Int) -> Unit,
    private val onNextPageStateChanged: (PageState) -> Unit,
) {

    private val mutex = Mutex()
    private val records = mutableListOf<MutablePageRecord<Key, T>>()
    private val outputList = mutableListOf<T>()
    private val listMerger = ListMerger(outputList, config.itemId)
    private var sequence: Long = 0

    // snapshots that are safe to access from other threads:
    private val outputListSnapshotFlow = MutableStateFlow(emptyList<T>())
    private val recordsSnapshotFlow = MutableStateFlow(emptyMap<Int, ImmutablePageRecord<Key, T>>())

    suspend fun prepareRecord(pageIndex: Int, pageKey: Key): ImmutablePageRecord<Key, T> = mutex.withLock {
        val mutableRecord = findMutableRecord(pageIndex, pageKey)
            ?: MutablePageRecord<Key, T>(pageIndex, pageKey, priority = ++sequence)
                .also { records.add(it) }
        mutableRecord.immutable()
    }

    suspend fun markAsCompleted(pageIndex: Int, pageKey: Key) {
        mutex.withLock {
            val mutableRecord = findMutableRecord(pageIndex, pageKey) ?: return
            mutableRecord.isCompleted = true
        }
    }

    suspend fun updateRecord(
        pageIndex: Int,
        pageKey: Key,
        container: (Container<List<T>>) -> Container<List<T>>,
        isRetry: Boolean = false,
    ) {
        val oldSnapshots: List<Container<List<T>>?>
        val newSnapshots: List<Container<List<T>>?>
        mutex.withLock {
            oldSnapshots = containersSnapshot()
            if (isRetry) removeErrorRecordsExceptThis(pageIndex, pageKey)
            val mutableRecord = findMutableRecord(pageIndex, pageKey) ?: return
            val nonFinalOldItems = getNonFinalItems()
            val newContainer = container(mutableRecord.safeContainer)
            mutableRecord.container = newContainer
            if (newContainer.isCompleted()) {
                val nonFinalNewItems = getNonFinalItems()
                listMerger.mergeFrom(nonFinalOldItems, nonFinalNewItems)
                updateSnapshots()
            }
            newSnapshots = containersSnapshot()
        }
        if (oldSnapshots != newSnapshots) {
            emitUpdates()
        }
    }

    fun intercept(container: Container<List<T>>): Container<List<T>> {
        return if (container.isSuccess()) {
            coroutineScope.launch {
                mutex.withLock {
                    outputList.clear()
                    outputList.addAll(container.value)
                    outputListSnapshotFlow.update { container.value }
                }
            }
            container.map { it } // report other instance the loader handled the update manually
        } else {
            container
        }
    }

    fun observeListSnapshots() = outputListSnapshotFlow

    fun observeRecordSnapshots() = recordsSnapshotFlow

    fun observePageSnapshots(pageIndex: Int): Flow<List<T>> {
        return recordsSnapshotFlow
            .filter { map -> map.contains(pageIndex) }
            .mapNotNull { map ->
                map[pageIndex]?.container?.getOrNull()?.takeIf { it.isNotEmpty() }
            }
    }

    fun firstErrorPageRecord(): PageRecord<Key, T>? {
        return recordsSnapshotFlow.value.values.filter { it.safeContainer.isError() }
            .minByOrNull { it.pageIndex }
    }

    suspend fun isAllPagesLoaded(): Boolean = mutex.withLock {
        records.all { it.isCompleted && it.safeContainer.isSuccess() }
    }

    private fun outputListSnapshot(): List<T> = outputListSnapshotFlow.value

    private fun outputMergedMetadata(containers: List<Container<*>?>): ContainerMetadata {
        val initialMetadata: ContainerMetadata = EmptyMetadata
        return containers.fold(initialMetadata) { currentMetadata, nextContainer ->
            currentMetadata + (nextContainer?.metadata ?: EmptyMetadata)
        }
    }

    private suspend fun emitUpdates() {
        val finalRecords = mutex.withLock {
            buildFinalRecords().flatMap { listOf(it.value) }
        }

        val containers: List<Container<List<T>>?> = finalRecords.map { it.container }

        if (containers.isEmpty() || containers.all { it is Container.Pending }) {
            originEmitter.emitPendingState()
        } else if (containers.getOrNull(0)?.isError() == true ||
                containers.all { it is Container.Error }) {
            val firstError = containers.first() as Container.Error
            originEmitter.emitFailureState(firstError.exception)
        } else {
            val errorRecord = finalRecords.firstOrNull { it.container is Container.Error }
            val pageState = if (errorRecord != null) {
                val exception = (errorRecord.container as Container.Error).exception
                PageState.Error(
                    exception = exception,
                    retry = tryAgainRef,
                )
            } else if (containers.contains(pendingContainer())) {
                PageState.Pending
            } else {
                PageState.Idle
            }
            onNextPageStateChanged(pageState)
            val outputList = outputListSnapshot()

            val finalMetadata = BackgroundLoadMetadata(BackgroundLoadState.Idle) + if (config.emitMetadata) {
                OnItemRenderedCallbackMetadata(onItemRenderedRef) +
                        NextPageStateMetadata(pageState) +
                        outputMergedMetadata(containers)
            } else {
                EmptyMetadata
            }
            originEmitter.emit(
                value = outputList,
                metadata = finalMetadata,
            )
        }
    }

    private fun recordsSnapshot() = records.map { it.immutable() }
    private fun containersSnapshot() = records.map { it.container }

    private fun getNonFinalItems(): List<T> {
        return records
            .groupBy { it.pageIndex }
            .filterValues { records ->
                records.any { !it.isCompleted && !it.safeContainer.isError() }
            }
            .flatMap { entry ->
                entry.value
                    .withHighestPriority { it.safeContainer.isSuccess() }
                    ?.container
                    ?.getOrNull()
                    ?: emptyList()
            }

    }

    private fun Iterable<PageRecord<Key, T>>.withHighestPriority(
        predicate: (PageRecord<Key, T>) -> Boolean = { true }
    ): PageRecord<Key, T>? {
        return filter(predicate).maxByOrNull { it.priority }
    }

    private fun findMutableRecord(
        pageIndex: Int,
        pageKey: Key,
    ): MutablePageRecord<Key, T>? {
        return records.firstOrNull { it.pageIndex == pageIndex && it.pageKey == pageKey }
    }

    private fun updateSnapshots() {
        outputListSnapshotFlow.update { outputList.toList() }
        recordsSnapshotFlow.update {
            buildFinalRecords()
        }
    }

    private fun buildFinalRecords(): Map<Int, ImmutablePageRecord<Key, T>> {
        return recordsSnapshot()
            .groupBy { it.pageIndex }
            .mapValues { (_, records) ->
                val errorRecord = records.withHighestPriority { it.safeContainer.isError() }
                val successRecord = records.withHighestPriority { it.safeContainer.isSuccess() }
                (errorRecord ?: (successRecord ?: records.first())).immutable()
            }
    }

    private fun removeErrorRecordsExceptThis(pageIndex: Int, pageKey: Key) {
        records.removeAll { it.safeContainer.isError() && (it.pageIndex != pageIndex || it.pageKey != pageKey)  }
    }

}
