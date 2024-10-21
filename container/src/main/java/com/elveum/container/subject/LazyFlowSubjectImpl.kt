package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.LoadTrigger
import com.elveum.container.subject.factories.LoadingScopeFactory
import com.elveum.container.subject.lazy.LoadTask
import com.elveum.container.subject.lazy.LoadTaskManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

internal class LazyFlowSubjectImpl<T>(
    private val loadingScopeFactory: LoadingScopeFactory,
    private val loadingDispatcher: CoroutineDispatcher,
    private val cacheTimeoutMillis: Long,
    private val loadTaskManager: LoadTaskManager<T>,
) : LazyFlowSubject<T> {

    override val currentValue: Container<T> get() = loadTaskManager.listen().value

    private var count = 0
    private var scope: CoroutineScope? = null
    private var cancellationJob: Job? = null

    override fun listen(): Flow<Container<T>> = callbackFlow {
        onStart()

        val job = scope?.launch {
            loadTaskManager.listen().collect(::trySend)
        }

        awaitClose {
            onStop(job)
        }
    }

    override fun newLoad(silently: Boolean, valueLoader: ValueLoader<T>): Flow<T> {
        return doNewLoad(silently, valueLoader, LoadTrigger.NewLoad)
    }

    override fun updateWith(container: Container<T>) {
        loadTaskManager.submitNewLoadTask(
            LoadTask.Instant(container, loadTaskManager.getLastRealLoader())
        )
    }

    override fun reload(silently: Boolean): Flow<T> {
        return loadTaskManager.getLastRealLoader()?.let { lastLoader ->
            doNewLoad(
                silently = silently,
                valueLoader = lastLoader,
                loadTrigger = LoadTrigger.Reload,
            )
        } ?: emptyFlow()
    }

    override fun isValueLoading(): StateFlow<Boolean> = loadTaskManager.isValueLoading()

    private fun doNewLoad(
        silently: Boolean,
        valueLoader: ValueLoader<T>,
        loadTrigger: LoadTrigger,
    ): Flow<T> = synchronized(loadTaskManager) {
        val flowSubject = FlowSubject.create<T>()
        val newTask = LoadTask.Load(valueLoader, loadTrigger, silently, flowSubject)
        loadTaskManager.submitNewLoadTask(newTask)
        flowSubject.flow()
    }

    private fun onStart() = synchronized(loadTaskManager) {
        count++
        if (count == 1) {
            cancellationJob?.cancel()
            cancellationJob = null
            startLoading()
        }
    }

    private fun onStop(job: Job?) = synchronized(loadTaskManager) {
        count--
        job?.cancel()
        if (count == 0) {
            scheduleStopLoading()
        }
    }

    private fun startLoading() {
        if (scope != null) return
        scope = loadingScopeFactory.createScope(loadingDispatcher)
            .also(loadTaskManager::startProcessingLoads)
    }

    private fun scheduleStopLoading() {
        cancellationJob = scope?.launch {
            delay(cacheTimeoutMillis)
            synchronized(loadTaskManager) {
                cancellationJob = null
                if (count == 0) { // double check required
                    loadTaskManager.cancelProcessingLoads()
                    scope?.cancel()
                    scope = null
                }
            }
        }
    }

}
