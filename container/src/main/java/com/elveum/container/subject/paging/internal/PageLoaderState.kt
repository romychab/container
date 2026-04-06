package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
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
        val record = registerKey(key, job) ?: return
        isImmediateLaunchScheduled = false
        while (true) {
            try {
                continueKey(key)
                block()
                completeKey(key)
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failKey(key, e)
                if (store.getAllContainers().all { it is Container.Error }) {
                    store.shutdown()
                    break
                } else {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        record.retryContinuation = continuation
                        continuation.invokeOnCancellation {
                            record.retryContinuation = null
                        }
                    }
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
    ): PageKeyRecord<Key, T>? {
        val record = store.getOrPut(key) { PageKeyRecord(key, job) }
        if (job != null) {
            record?.job = job
        }
        return record
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

    private fun completeKey(key: Key) {
        store.onKeyCompleted(key)
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

    fun intercept(container: Container<List<T>>): Container<List<T>> {
        return store.intercept(container)
    }

}
