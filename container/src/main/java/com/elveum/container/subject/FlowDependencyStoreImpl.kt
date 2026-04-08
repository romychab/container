@file:OptIn(FlowPreview::class)

package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.Container.Completed
import com.elveum.container.LoadConfig
import com.elveum.container.ReloadFunction
import com.elveum.container.factory.DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS
import com.elveum.container.pendingContainer
import com.elveum.container.subject.lazy.FlowDependencyStore
import com.elveum.container.subject.lazy.FlowDependencyStore.RecomposeFunction
import com.elveum.container.subject.lazy.LoadTaskManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class FlowDependencyStoreImpl<T>(
    private val loadTaskManager: LoadTaskManager<T>,
    private val reloadDependenciesPeriodMillis: Long = DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS,
) : FlowDependencyStore {

    private var scope: CoroutineScope? = null
    private var recomposeFunction: RecomposeFunction = {}

    private var debounceReloadJob: Job? = null
    private val debounceFlow = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val flowDependencyRecords = mutableMapOf<Any, FlowDependencyRecord<*>>()
    private val activeKeys = mutableSetOf<Any>()

    override fun initialize(
        scope: CoroutineScope,
        recomposeFunction: RecomposeFunction,
    ) = synchronized(loadTaskManager) {
        this.scope = scope
        this.recomposeFunction = recomposeFunction
        debounceReloadJob = debounceFlow
            .debounce(timeoutMillis = reloadDependenciesPeriodMillis)
            .onEach {
                recomposeFunction.execute(reloadDependencies = false)
            }
            .launchIn(scope)
    }

    override fun begin(
        reloadDependencies: Boolean,
        loadConfig: LoadConfig,
    ) = synchronized(loadTaskManager) {
        activeKeys.clear()
        if (reloadDependencies) {
            val reloadFunctions = flowDependencyRecords.values
                .map { it.lastReloadFunction }
                .distinct()
            reloadFunctions.forEach { it.invoke(loadConfig) }
        }
    }

    override suspend fun <R> dependsOn(
        key: Any,
        vararg keys: Any,
        flow: () -> Flow<Container<R>>,
    ): R {
        val combinedKey = listOf(key) + keys.toList()
        val awaitFunction: suspend () -> R = synchronized(loadTaskManager) {
            activeKeys.add(combinedKey)
            val existingRecord = flowDependencyRecords[combinedKey] as? FlowDependencyRecord<R>
            if (existingRecord == null) {
                val firstResultDeferred = CompletableDeferred<R>()
                val newRecord = FlowDependencyRecord<R>()
                flowDependencyRecords[combinedKey] = newRecord
                newRecord.job = scope?.launch {
                    newRecord.startFlow(combinedKey, flow, firstResultDeferred)
                }
                firstResultDeferred::await
            } else {
                existingRecord::await
            }
        }
        return awaitFunction()
    }

    override fun end() = synchronized(loadTaskManager) {
        val oldKeys = flowDependencyRecords
            .keys
            .filter { !activeKeys.contains(it) }
        oldKeys.forEach {
            val oldRecord = flowDependencyRecords.remove(it)
            oldRecord?.job?.cancel()
        }
        activeKeys.clear()
    }

    override fun shutdown() = synchronized(loadTaskManager) {
        debounceReloadJob?.cancel()
        debounceReloadJob = null
        flowDependencyRecords.values
            .toList()
            .forEach {
                it.job?.cancel()
            }
        flowDependencyRecords.clear()
        activeKeys.clear()
    }

    private suspend fun <R> FlowDependencyRecord<R>.startFlow(
        key: Any,
        flow: () -> Flow<Container<R>>,
        firstResultDeferred: CompletableDeferred<R>,
    ) {
        try {
            flow()
                .filterIsInstance<Completed<R>>()
                .onEach {
                    lastReloadFunction = it.reloadFunction
                }
                .map(Completed<R>::raw)
                .distinctUntilChanged()
                .collect { container ->
                    containerFlow.update { container }
                    if (!firstResultDeferred.isCompleted) {
                        when (container) {
                            is Container.Success<R> -> firstResultDeferred.complete(container.value)
                            is Container.Error -> firstResultDeferred.completeExceptionally(container.exception)
                        }
                    } else {
                        debounceFlow.tryEmit(Unit)
                    }
                }
        } finally {
            synchronized(loadTaskManager) {
                flowDependencyRecords.remove(key)
                activeKeys.remove(key)
            }
        }
    }

    private class FlowDependencyRecord<R> {
        val containerFlow = MutableStateFlow<Container<R>>(pendingContainer())
        @Volatile var lastReloadFunction: ReloadFunction = {}
        var job: Job? = null

        suspend fun await(): R {
            val container = containerFlow
                .filterIsInstance<Completed<R>>()
                .first()
            return when (container) {
                is Container.Error -> throw container.exception
                is Container.Success<R> -> container.value
            }
        }
    }

}
