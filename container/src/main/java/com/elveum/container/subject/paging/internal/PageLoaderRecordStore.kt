package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
import com.elveum.container.errorContainer
import com.elveum.container.getOrNull
import com.elveum.container.pendingContainer
import kotlinx.coroutines.sync.Mutex

internal class PageLoaderRecordStore<Key, T>(
    private val threshold: Float
) {

    private val orderedRecords = LinkedHashMap<Key, PageKeyRecord<Key, T>>()

    private val awaitMutex = Mutex(locked = true)

    private var counter = 0
    private var isFinished = false

    fun forEach(action: (PageKeyRecord<Key, T>) -> Unit) = synchronized(this) {
        orderedRecords.values.forEach(action)
    }

    fun findNextKeyForIndex(index: Int): NextKeyLoadResult<Key> = synchronized(this) {
        val totalItemsCount = orderedRecords.values
            .sumOf { (it.container as? Container.Success)?.value?.size ?: 0 }
        if (index !in 0..<totalItemsCount) return@synchronized NextKeyLoadResult.Skip
        if (firstOrNull { it.container is Container.Error } != null) {
            return@synchronized NextKeyLoadResult.Skip
        }
        var pageStartIndex = 0
        var lastPageSize = 0
        orderedRecords.values.forEach { record ->
            if (index < pageStartIndex) {
                // 'record' is a pending next page; only trigger if threshold reached in previous page
                val thresholdIndex = getThresholdIndex(pageStartIndex, lastPageSize)
                return@synchronized if (index >= thresholdIndex) {
                    NextKeyLoadResult.Key(record.key)
                } else {
                    NextKeyLoadResult.Skip
                }
            }
            val pageList = record.container.getOrNull() ?: return@synchronized NextKeyLoadResult.Skip
            lastPageSize = pageList.size
            pageStartIndex += pageList.size
        }
        // No pending next-page record; schedule immediate load when threshold is reached
        if (lastPageSize == 0) return@synchronized NextKeyLoadResult.Skip
        val thresholdIndex = getThresholdIndex(pageStartIndex, lastPageSize)
        return@synchronized if (index >= thresholdIndex) {
            NextKeyLoadResult.ScheduleImmediateLoad
        } else {
            NextKeyLoadResult.Skip
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
        orderedRecords[key]?.container = container
    }

    fun hasLaunchedKey(key: Key) = synchronized(this) {
        orderedRecords[key]?.job != null
    }

    fun onKeyContinued(key: Key) = synchronized(this) {
        orderedRecords[key]?.container = pendingContainer()
    }

    fun onKeyCompleted() = synchronized(this) {
        counter--
        if (counter == 0 && awaitMutex.isLocked) {
            awaitMutex.unlock()
        }
    }

    fun shutdown() = synchronized(this) {
        counter = 0
        isFinished = true
        orderedRecords.clear()
        if (awaitMutex.isLocked) {
            awaitMutex.unlock()
        }
    }

    fun onKeyFailed(key: Key, exception: Exception) = synchronized(this) {
        orderedRecords[key]?.container = errorContainer(exception)
        val keysList = orderedRecords.keys.toList()
        val startIndex = keysList.indexOf(key)
        if (startIndex != -1 && startIndex != keysList.lastIndex) {
            // cancel and remove all pages located after the failed page:
            val keysToBeCancelled = keysList.subList(startIndex + 1, keysList.size)
            keysToBeCancelled.forEach { key ->
                cancelKey(key)
            }
        }
    }

    fun buildOutputList(): List<T> = synchronized(this) {
        return orderedRecords.values
            .map { record -> record.container }
            .mapNotNull { container -> container.getOrNull() }
            .flatten()
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

    private fun cancelKey(key: Key) {
        val record = orderedRecords.remove(key)
        if (record != null) {
            record.job?.cancel()
            counter--
        }
    }

    private fun getThresholdIndex(
        pageStartIndex: Int,
        lastPageSize: Int,
    ) = pageStartIndex - lastPageSize + (threshold * lastPageSize).toInt()
        .coerceIn(0, pageStartIndex - 1)

    sealed class NextKeyLoadResult<out Key> {
        data object Skip : NextKeyLoadResult<Nothing>()
        data class Key<Key>(val key: Key) : NextKeyLoadResult<Key>()
        data object ScheduleImmediateLoad : NextKeyLoadResult<Nothing>()
    }

}
