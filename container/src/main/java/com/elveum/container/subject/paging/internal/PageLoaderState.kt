package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
import com.elveum.container.StatefulEmitter
import com.elveum.container.errorContainer
import com.elveum.container.getOrNull
import com.elveum.container.pendingContainer
import com.elveum.container.subject.paging.PageState
import com.elveum.container.successContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.resume

internal class PageLoaderState<Key, T>(
    private val emitter: StatefulEmitter<List<T>>,
    private val onNextPageStateChanged: (PageState) -> Unit,
) {

    val orderedRecords = LinkedHashMap<Key, PageKeyRecord<Key, T>>()

    private val awaitMutex = Mutex(locked = true)

    private var counter = 0

    @Volatile
    private var isImmediateLaunchScheduled = false

    private val retry = {
        orderedRecords.values.forEach { record ->
            record.retryContinuation?.resume(Unit)
            record.retryContinuation = null
        }
    }

    suspend fun processKey(
        key: Key,
        job: Job,
        block: suspend () -> Unit,
    ) {
        val record = registerKey(key, job)
        isImmediateLaunchScheduled = false
        while (true) {
            try {
                continueKey(key)
                block()
                completeKey()
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failKey(key, e)
                suspendCancellableCoroutine<Unit> { continuation ->
                    record.retryContinuation = continuation
                }
            }
        }
    }

    suspend fun onKeyLoaded(key: Key, list: List<T>) {
        synchronized(this) {
            orderedRecords[key]?.container = successContainer(list)
        }
        onChanged()
    }

    suspend fun await() {
        awaitMutex.lock()
    }

    fun registerKey(
        key: Key,
        job: Job? = null,
    ): PageKeyRecord<Key, T> = synchronized(this) {
        return orderedRecords.getOrPut(key) {
            PageKeyRecord<Key, T>(key, job).also { counter++ }
        }
    }

    fun findNextKeyForIndex(index: Int): Key? = synchronized(this) {
        var pageStartIndex = 0
        orderedRecords.values.forEach { record ->
            if (index < pageStartIndex) {
                return@synchronized record.key
            }
            val pageList = record.container.getOrNull() ?: return@synchronized null
            pageStartIndex += pageList.size
        }
        if (index == pageStartIndex - 1) {
            scheduleImmediateLaunch()
        }
        null
    }

    fun hasLaunchedKey(key: Key): Boolean = synchronized(this) {
        return orderedRecords[key]?.job != null
    }

    private suspend fun continueKey(key: Key) {
        synchronized(this) {
            orderedRecords[key]?.container = pendingContainer()
        }
        onChanged()
    }

    private suspend fun completeKey() {
        synchronized(this) {
            counter--
            if (counter == 0) {
                awaitMutex.unlock()
            }
        }
        onChanged()
    }

    private suspend fun failKey(key: Key, exception: Exception) {
        synchronized(this) {
            orderedRecords[key]?.container = errorContainer(exception)
            val keysList = orderedRecords.keys.toList()
            val startIndex = keysList.indexOf(key)
            if (startIndex != -1 && startIndex != keysList.lastIndex) {
                // cancel and remove all pages located after the failed page:
                val keysToBeCancelled = keysList.subList(startIndex + 1, keysList.lastIndex)
                keysToBeCancelled.forEach { key ->
                    orderedRecords.remove(key)?.job?.cancel()
                }
            }
        }
        onChanged()
    }

    private suspend fun onChanged() {
        val containers = orderedRecords.values.map { it.container }
        if (containers.isEmpty() || containers.all { it is Container.Pending }) {
            emitter.emitPendingState()
        } else if (containers.all { it is Container.Error }) {
            val firstError = containers.first() as Container.Error
            emitter.emitFailureState(firstError.exception)
        } else {
            val firstErrorRecord = orderedRecords.values.firstOrNull { it.container is Container.Error }
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
            val outputList = buildOutputList()
            emitter.emit(outputList)
        }
    }

    private fun buildOutputList(): List<T> {
        return orderedRecords.values
            .map { record -> record.container }
            .mapNotNull { container -> container.getOrNull() }
            .flatten()
    }

    private fun scheduleImmediateLaunch() {
        isImmediateLaunchScheduled = true
    }

    fun isImmediateLaunchScheduled(): Boolean {
        return isImmediateLaunchScheduled
    }

}
