package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
import com.elveum.container.errorContainer
import com.elveum.container.getOrNull
import com.elveum.container.isCompleted
import com.elveum.container.isSuccess
import com.elveum.container.map
import com.elveum.container.pendingContainer
import kotlinx.coroutines.sync.Mutex
import kotlin.math.max

internal class PageLoaderRecordStore<Key, T>(
    private val fetchDistance: Int,
    itemId: (T) -> Any,
) {

    private val outputList = mutableListOf<T>()
    private val listMerger = ListMerger(outputList, itemId)

    private val orderedRecords = LinkedHashMap<Key, PageKeyRecord<Key, T>>()

    private val awaitMutex = Mutex(locked = true)

    private var counter = 0
    private var isFinished = false

    fun forEach(action: (PageKeyRecord<Key, T>) -> Unit) = synchronized(this) {
        orderedRecords.values.forEach(action)
    }

    fun findNextKeyForIndex(index: Int): NextKeyLoadResult<Key> = synchronized(this) {
        val totalSize = orderedRecords.values.sumOf {
            it.container.getOrNull()?.size ?: 0
        }
        if (totalSize == 0) return@synchronized NextKeyLoadResult.Skip
        if (index !in 0..<totalSize) return@synchronized NextKeyLoadResult.Skip
        if (firstOrNull { it.container is Container.Error } != null) {
            return@synchronized NextKeyLoadResult.Skip
        }

        val adjustedFetchDistance = max(1, fetchDistance)
        val thresholdIndex = max(0, totalSize - adjustedFetchDistance)
        if (index < thresholdIndex) {
            NextKeyLoadResult.Skip
        } else {
            val firstNonLaunchedPendingPage = orderedRecords.values.firstOrNull {
                it.container == pendingContainer() && it.job == null
            }
            val hasLaunchedNonSuccessfulPages = orderedRecords.values.any {
                it.container !is Container.Success && it.job != null
            }
            if (firstNonLaunchedPendingPage != null) {
                NextKeyLoadResult.Key(firstNonLaunchedPendingPage.key)
            } else if (hasLaunchedNonSuccessfulPages) {
                NextKeyLoadResult.Skip
            } else {
                NextKeyLoadResult.ScheduleImmediateLoad
            }
        }
    }

    fun getOrPut(key: Key, defaultValue: () -> PageKeyRecord<Key, T>): PageKeyRecord<Key, T>? = synchronized(this) {
        if (isFinished) return@synchronized null
        return orderedRecords.getOrPut(key) {
            counter++
            defaultValue()
        }
    }

    fun updateContainer(key: Key, container: Container<List<T>>) = synchronized(this) {
        val nonFinalOldItems = getNonFinalItems()
        orderedRecords[key]?.container = container
        if (container.isCompleted()) {
            val nonFinalNewItems = getNonFinalItems()
            listMerger.mergeFrom(nonFinalOldItems, nonFinalNewItems)
        }
    }

    fun hasLaunchedKey(key: Key) = synchronized(this) {
        orderedRecords[key]?.job != null
    }

    fun onKeyContinued(key: Key) = synchronized(this) {
        orderedRecords[key]?.container = pendingContainer()
    }

    fun onKeyCompleted(key: Key) = synchronized(this) {
        counter--
        orderedRecords[key]?.completed = true
        if (counter == 0 && awaitMutex.isLocked) {
            awaitMutex.unlock()
        }
    }

    fun shutdown() = synchronized(this) {
        counter = 0
        isFinished = true
        orderedRecords.clear()
        outputList.clear()
        if (awaitMutex.isLocked) {
            awaitMutex.unlock()
        }
    }

    fun onKeyFailed(key: Key, exception: Exception) = synchronized(this) {
        val nonFinalOldItems = getNonFinalItems()
        orderedRecords[key]?.container = errorContainer(exception)
        val nonFinalNewItems = emptyList<T>()
        val keysList = orderedRecords.keys.toList()
        val startIndex = keysList.indexOf(key)
        if (startIndex != -1 && startIndex != keysList.lastIndex) {
            // cancel and remove all pages located after the failed page:
            val keysToBeCancelled = keysList.subList(startIndex + 1, keysList.size)
            keysToBeCancelled.forEach { key ->
                cancelKey(key)
            }
        }
        listMerger.mergeFrom(nonFinalOldItems, nonFinalNewItems)
    }

    fun buildOutputList(): List<T> = synchronized(this) {
        return outputList.toList()
    }

    fun getAllContainers() = synchronized(this) {
        orderedRecords.values.map { it.container }
    }

    fun firstOrNull(
        predicate: (PageKeyRecord<Key, T>) -> Boolean,
    ): PageKeyRecord<Key, T>? = synchronized(this) {
        orderedRecords.values.firstOrNull(predicate)
    }

    suspend fun await() {
        awaitMutex.lock()
    }

    fun keys(): List<Key> = synchronized(this) {
        orderedRecords.keys.toList()
    }

    fun intercept(container: Container<List<T>>): Container<List<T>> = synchronized(this) {
        if (container.isSuccess()) {
            outputList.clear()
            outputList.addAll(container.value)
            container.map { buildOutputList() }
        } else {
            container
        }
    }

    private fun cancelKey(key: Key) {
        val record = orderedRecords.remove(key)
        if (record != null) {
            record.job?.cancel()
            counter--
        }
    }

    private fun getNonFinalItems() = orderedRecords.values
        .mapNotNull { record ->
            record.container.getOrNull().takeIf { !record.completed }
        }
        .flatten()

    sealed class NextKeyLoadResult<out Key> {
        data object Skip : NextKeyLoadResult<Nothing>()
        data class Key<Key>(val key: Key) : NextKeyLoadResult<Key>()
        data object ScheduleImmediateLoad : NextKeyLoadResult<Nothing>()
    }

}
