package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.LoadTrigger
import com.elveum.container.LoadTriggerMetadata
import com.elveum.container.subject.transformation.ContainerTransformation
import com.elveum.container.subject.transformation.EmptyContainerTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class LoadTaskManager<T>(
    private val transformation: ContainerTransformation<T> = EmptyContainerTransformation(),
) {

    private val inputFlow = MutableStateFlow<LoadTask<T>>(LoadTask.Instant(Container.Pending))
    private val outputFlow = MutableStateFlow<Container<T>>(Container.Pending)
    private var job: Job? = null

    fun listen(): StateFlow<Container<T>> = outputFlow

    fun startProcessingLoads(
        scope: CoroutineScope,
        flowDependencyStore: FlowDependencyStore,
    ) = synchronized(this) {
        if (job != null) return@synchronized
        val currentContainer: () -> Container<T> = { outputFlow.value }
        job = scope.launch {
            inputFlow
                .collectLatest { loadTask ->
                    val executeParams = LoadTask.ExecuteParams(
                        flowDependencyStore = flowDependencyStore,
                        currentContainer = currentContainer,
                    )
                    loadTask.execute(executeParams)
                        .run(transformation)
                        .collectLatest { value ->
                            setOutputValueIfNotCancelled(loadTask, value)
                        }
                }
        }
    }

    fun cancelProcessingLoads() = synchronized(this) {
        if (job == null) return@synchronized
        job?.cancel()
        job = null
        outputFlow.value = Container.Pending
        inputFlow.value.cancel("Load task cancelled since the last collector has been stopped.")
        inputFlow.update { oldLoadTask ->
            val updatedMetadata = oldLoadTask.metadata + LoadTriggerMetadata(LoadTrigger.CacheExpired)
            oldLoadTask.restoreLoadTask(updatedMetadata)
        }
    }

    fun submitNewLoadTask(loadTask: LoadTask<T>) = synchronized(this) {
        if (job != null) {
            loadTask.initialContainer?.let { outputFlow.value = it }
        }
        inputFlow.update { oldLoadTask ->
            oldLoadTask.cancel("Load task cancelled due to a new load submission")
            loadTask
        }
    }

    fun getLastRealLoader() = inputFlow.value.lastRealLoader

    fun getLastRealMetadata() = inputFlow.value.lastRealMetadata

    private suspend fun setOutputValueIfNotCancelled(
        loadTask: LoadTask<T>,
        value: Container<T>
    ) {
        val isActive = currentCoroutineContext().isActive
        synchronized(this) {
            if (isActive && inputFlow.value === loadTask) {
                outputFlow.value = value.filterMetadata {
                    it !is ContainerMetadata.Hidden
                }
            }
        }
    }

}
