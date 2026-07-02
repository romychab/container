package com.elveum.store.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal suspend fun <T> processItems(
    flow: Flow<Set<T>>,
    onAdded: suspend (T) -> Unit = {},
    onRemoved: suspend (T) -> Unit = {},
    job: suspend (T) -> Unit = { awaitCancellation() },
) = coroutineScope {
    val mutex = Mutex()
    val jobMap = mutableMapOf<T, Job>()
    flow.collectLatest { items ->
        removeMissingItems(mutex, jobMap, items, onRemoved)
        addNewItems(mutex, jobMap, items, onAdded, onRemoved, job)
    }
}

private suspend fun <T> removeMissingItems(
    mutex: Mutex,
    jobMap: MutableMap<T, Job>,
    items: Set<T>,
    onRemoved: suspend (T) -> Unit,
) {
    val removedItems = mutex.withLock {
        val oldItems = jobMap.keys - items
        oldItems.forEach { jobMap.remove(it)?.cancel() }
        oldItems
    }
    // onRemoved is invoked here (not from the job completion handler) so that it always fires
    // exactly once per removed key - the identity-guarded completion handler in addNewItems
    // would otherwise skip a job that has already been replaced by a re-added key.
    removedItems.forEach { onRemoved(it) }
}

private suspend fun <T> CoroutineScope.addNewItems(
    mutex: Mutex,
    jobMap: MutableMap<T, Job>,
    items: Set<T>,
    onAdded: suspend (T) -> Unit,
    onRemoved: suspend (T) -> Unit,
    job: suspend (T) -> Unit,
) {
    val newItems = mutex.withLock { items - jobMap.keys }
    newItems.forEach { item ->
        onAdded(item)
        // Launch and register the job atomically under the same lock (no cancellable suspension
        // between them): a concurrent emission can never cancel this block after the job is
        // launched but before it is tracked, which would otherwise leak the job and let the next
        // emission launch a duplicate for the same key.
        mutex.withLock {
            val newJob = launch { job(item) }
            jobMap[item] = newJob
            newJob.invokeOnCompletion {
                launch {
                    // Guard by job identity: if the key was removed and re-added, jobMap[item]
                    // now points at a newer job and this stale completion must not remove it.
                    val stillTracked = mutex.withLock {
                        (jobMap[item] === newJob).also { if (it) jobMap.remove(item) }
                    }
                    if (stillTracked) onRemoved(item)
                }
            }
        }
    }
}
