@file:OptIn(ExperimentalForInheritanceCoroutinesApi::class)

package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyReloadFunction
import com.elveum.container.LoadTrigger
import com.elveum.container.LoadTriggerMetadata
import com.elveum.container.ReloadDependenciesMetadata
import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.container.factory.DefaultReloadDependenciesPeriodMillis
import com.elveum.container.internalDistinctUntilChanged
import com.elveum.container.subject.lazy.LoadTask
import com.elveum.container.subject.lazy.LoadTaskManager
import com.elveum.container.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class LazyFlowSubjectImpl<T>(
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val cacheTimeoutMillis: Long,
    private val loadTaskManager: LoadTaskManager<T>,
    private val reloadDependenciesPeriodMillis: Long = DefaultReloadDependenciesPeriodMillis,
    private val loadTaskFactory: LoadTaskFactory = LoadTaskFactory.Default,
    private val flowDependencyStore: FlowDependencyStoreImpl<T> = FlowDependencyStoreImpl(
        loadTaskManager = loadTaskManager,
        reloadDependenciesPeriodMillis = reloadDependenciesPeriodMillis,
    )
) : LazyFlowSubject<T> {

    override val activeCollectorsCount: Int get() = collectorsCountFlow.value

    private val currentValue: Container<T> get() = loadTaskManager.listen().value
    private val collectorsCountFlow = MutableStateFlow(0)
    private var scope: CoroutineScope? = null
    private var cancellationJob: Job? = null

    override fun currentValue(configuration: ContainerConfiguration): Container<T> {
        return currentValue.applyConfiguration(configuration)
    }

    override fun listen(configuration: ContainerConfiguration): StateFlow<Container<T>> {
        return ListenStateFlowImpl(configuration)
    }

    override fun newLoad(
        silently: Boolean,
        metadata: ContainerMetadata,
        valueLoader: ValueLoader<T>,
    ): Flow<T> {
        return doNewLoad(
            silently = silently,
            valueLoader = valueLoader,
            metadata = LoadTriggerMetadata(LoadTrigger.NewLoad) + metadata,
        )
    }

    override fun updateWith(container: Container<T>) = synchronized(loadTaskManager) {
        loadTaskManager.submitNewLoadTask(
            LoadTask.Instant(
                initialContainer = container,
                lastRealLoader = loadTaskManager.getLastRealLoader(),
                lastRealMetadata = loadTaskManager.getLastRealMetadata(),
            )
        )
    }

    override fun reload(
        silently: Boolean,
        metadata: ContainerMetadata,
    ): Flow<T> = synchronized(loadTaskManager) {
        loadTaskManager.getLastRealLoader()?.let { lastLoader ->
            doNewLoad(
                silently = silently,
                valueLoader = lastLoader,
                metadata = LoadTriggerMetadata(LoadTrigger.Reload) +
                        ReloadDependenciesMetadata(true) +
                        metadata,
            )
        } ?: emptyFlow()
    }

    private fun doNewLoad(
        silently: Boolean,
        valueLoader: ValueLoader<T>,
        metadata: ContainerMetadata,
    ): Flow<T> = synchronized(loadTaskManager) {
        val loadTaskRecord = loadTaskFactory.create(silently, valueLoader, metadata)
        loadTaskManager.submitNewLoadTask(loadTaskRecord.loadTask)
        loadTaskRecord.flowSubject.flow()
    }

    private fun onStart() = synchronized(loadTaskManager) {
        collectorsCountFlow.value++
        if (collectorsCountFlow.value == 1) {
            cancellationJob?.cancel()
            cancellationJob = null
            startLoading()
        }
    }

    private fun onStop() = synchronized(loadTaskManager) {
        collectorsCountFlow.value--
        if (collectorsCountFlow.value == 0) {
            scheduleStopLoading()
        }
    }

    private fun startLoading() {
        if (scope != null) return
        scope = coroutineScopeFactory.createScope()
            .also { scope ->
                flowDependencyStore.initialize(scope) { reloadDependencies ->
                    reloadAsync(silently = true, metadata = ReloadDependenciesMetadata(reloadDependencies))
                }
                loadTaskManager.startProcessingLoads(
                    scope = scope,
                    flowDependencyStore = flowDependencyStore,
                )
            }
    }

    private fun scheduleStopLoading() {
        cancellationJob = scope?.launch {
            delay(cacheTimeoutMillis)
            synchronized(loadTaskManager) {
                cancellationJob = null
                if (collectorsCountFlow.value == 0) { // double check required
                    loadTaskManager.cancelProcessingLoads()
                    flowDependencyStore.shutdown()
                    scope?.cancel()
                    scope = null
                }
            }
        }
    }

    private fun Container<T>.applyConfiguration(
        configuration: ContainerConfiguration,
    ): Container<T> {
        return this
            .runIf(!configuration.emitReloadFunction) {
                update(reloadFunction = EmptyReloadFunction)
            }
            .runIf(!configuration.emitBackgroundLoads) {
                update(isLoadingInBackground = false)
            }
    }

    private inner class ListenStateFlowImpl(
        private val configuration: ContainerConfiguration,
    ) : StateFlow<Container<T>> {

        override val replayCache: List<Container<T>> get() = listOf(value)
        override val value: Container<T> get() = currentValue

        override suspend fun collect(collector: FlowCollector<Container<T>>): Nothing {
            try {
                onStart()
                loadTaskManager.listen()
                    .map { it.update(reloadFunction = ::reload) }
                    .map { it.applyConfiguration(configuration) }
                    .internalDistinctUntilChanged()
                    .collect(collector)
                awaitCancellation()
            } finally {
                onStop()
            }
        }
    }

    private inline fun <T> T.runIf(condition: Boolean, block: T.() -> T): T {
        return if (condition) {
            block()
        } else {
            this
        }
    }

    interface LoadTaskFactory {

        fun <T> create(
            silently: Boolean,
            valueLoader: ValueLoader<T>,
            metadata: ContainerMetadata,
        ): LoadTaskRecord<T>

        object Default : LoadTaskFactory {
            override fun <T> create(
                silently: Boolean,
                valueLoader: ValueLoader<T>,
                metadata: ContainerMetadata,
            ): LoadTaskRecord<T> {
                val flowSubject = FlowSubject.create<T>()
                val loadTask = LoadTask.Load(valueLoader, metadata, silently, flowSubject)
                return LoadTaskRecord(loadTask, flowSubject)
            }
        }

        class LoadTaskRecord<T>(
            val loadTask: LoadTask<T>,
            val flowSubject: FlowSubject<T>,
        )

    }

}
