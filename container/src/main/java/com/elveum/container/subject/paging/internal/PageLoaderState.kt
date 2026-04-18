package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
import com.elveum.container.successContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine

internal class PageLoaderState<Key, T>(
    private val pageResultsEmitter: PageResultsEmitter<Key, T>,
    private val store: PageLoaderRecordStore<Key, T>,
    private val pageKeyProcessor: PageKeyProcessor<Key, T> = PageKeyProcessor(pageResultsEmitter, store),
) {

    suspend fun processKey(
        key: Key,
        job: Job,
        block: suspend () -> Unit,
    ) {
        val record = registerKey(key, job) ?: return
        pageKeyProcessor.resetScheduledImmediateLaunch()

        do {
            val isFinished: Boolean = try {
                pageKeyProcessor.continueKey(key)
                block()
                pageKeyProcessor.completeKey(key)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                processFailure(key, record, e)
            }
        } while (!isFinished)
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
                pageKeyProcessor.scheduleImmediateLaunch()
                null
            }
        }
    }

    fun hasLaunchedKey(key: Key): Boolean = store.hasLaunchedKey(key)

    fun intercept(container: Container<List<T>>): Container<List<T>> {
        return store.intercept(container)
    }

    fun isImmediateLaunchScheduled(): Boolean {
        return pageKeyProcessor.isImmediateLaunchScheduled()
    }

    private suspend fun processFailure(
        key: Key,
        record: PageKeyRecord<Key, T>,
        e: Exception,
    ): Boolean {
        pageKeyProcessor.failKey(key, e)
        return if (store.getAllContainers().all { it is Container.Error }) {
            store.shutdown()
            true
        } else {
            suspendCancellableCoroutine<Unit> { continuation ->
                record.retryContinuation = continuation
                continuation.invokeOnCancellation {
                    record.retryContinuation = null
                }
            }
            false
        }
    }

}
