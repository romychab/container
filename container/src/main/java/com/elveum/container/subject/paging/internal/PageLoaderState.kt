package com.elveum.container.subject.paging.internal

import com.elveum.container.successContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine

internal class PageLoaderState<Key, T>(
    private val pageResultsEmitter: PageResultsEmitter<Key, T>,
    private val store: PageLoaderRecordStore<Key, T>,
) {

    @Volatile
    private var isImmediateLaunchScheduled = false

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
        store.updateContainer(key, successContainer(list))
        pageResultsEmitter.emitResults()
    }

    suspend fun await() {
        store.await()
    }

    fun registerKey(
        key: Key,
        job: Job? = null,
    ): PageKeyRecord<Key, T> {
        return store.getOrPut(key) { PageKeyRecord(key, job) }
    }

    fun findNextKeyForIndex(index: Int): Key? = synchronized(store) {
        when (val result = store.findNextKeyForIndex(index)) {
            is PageLoaderRecordStore.NextKeyLoadResult.Key<Key> -> result.key
            PageLoaderRecordStore.NextKeyLoadResult.Skip -> null
            PageLoaderRecordStore.NextKeyLoadResult.ScheduleImmediateLoad -> {
                scheduleImmediateLaunch()
                null
            }
        }
    }

    fun hasLaunchedKey(key: Key): Boolean = store.hasLaunchedKey(key)

    private suspend fun continueKey(key: Key) {
        store.onKeyContinued(key)
        pageResultsEmitter.emitResults()
    }

    private suspend fun completeKey() {
        store.onKeyCompleted()
        pageResultsEmitter.emitResults()
    }

    private suspend fun failKey(key: Key, exception: Exception) {
        store.onKeyFailed(key, exception)
        pageResultsEmitter.emitResults()
    }

    private fun scheduleImmediateLaunch() {
        isImmediateLaunchScheduled = true
    }

    fun isImmediateLaunchScheduled(): Boolean {
        return isImmediateLaunchScheduled
    }

}
