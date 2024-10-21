package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.LoadTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext


internal class LoadTaskManager<T> {

    private val progressCounter = AtomicInteger(0)
    private val isLoadingFlow = MutableStateFlow(false)
    private val inputFlow = MutableStateFlow<LoadTask<T>>(LoadTask.Instant(Container.Pending))
    private val outputFlow = MutableStateFlow<Container<T>>(Container.Pending)
    private var job: Job? = null

    fun listen(): StateFlow<Container<T>> = outputFlow

    fun isValueLoading(): StateFlow<Boolean> = isLoadingFlow

    fun startProcessingLoads(scope: CoroutineScope) = synchronized(this) {
        if (job != null) return
        job = scope.launch {
            inputFlow
                .collectLatest { loadTask ->
                    try {
                        isLoadingFlow.value = progressCounter.incrementAndGet() != 0
                        loadTask.execute().collectLatest(::setOutputValueIfNotCancelled)
                    } finally {
                        isLoadingFlow.value = progressCounter.decrementAndGet() != 0
                    }
                }
        }
    }

    fun cancelProcessingLoads() = synchronized(this) {
        if (job == null) return@synchronized
        job?.cancel()
        job = null
        outputFlow.value = Container.Pending
        inputFlow.value.apply {
            cancel()
            setLoadTrigger(LoadTrigger.CacheExpired)
        }
    }

    fun submitNewLoadTask(loadTask: LoadTask<T>) = synchronized(this) {
        inputFlow.update { oldLoadTask ->
            oldLoadTask.cancel()
            loadTask
        }
    }

    fun getLastRealLoader() = inputFlow.value.lastRealLoader

    private suspend fun setOutputValueIfNotCancelled(value: Container<T>) {
        synchronized(this) {
            if (coroutineContext.isActive) {
                outputFlow.value = value
            }
        }
    }

}
