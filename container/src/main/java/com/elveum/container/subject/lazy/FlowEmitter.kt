package com.elveum.container.subject.lazy

import com.elveum.container.BackgroundLoadState
import com.elveum.container.BackgroundLoadMetadata
import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.Emitter
import com.elveum.container.subject.FlowSubject
import com.elveum.container.successContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.map

internal class FlowEmitter<T>(
    override val metadata: ContainerMetadata,
    private val flowCollector: FlowCollector<Container<T>>,
    private val executeParams: LoadTask.ExecuteParams<T>,
    private val flowSubject: FlowSubject<T>? = null,
) : Emitter<T> {

    private var lastEmittedValue: Container<T>? = null
    private var _hasEmittedValues = false
    val hasEmittedValues get() = _hasEmittedValues

    private val flowDependencyStore = executeParams.flowDependencyStore

    override suspend fun emit(value: T, metadata: ContainerMetadata, isLastValue: Boolean) {
        _hasEmittedValues = true
        flowSubject?.onNext(value)
        flowCollector.emit(buildOutputContainer(value, metadata, isLoadingInBackground = !isLastValue))
        lastEmittedValue = if (isLastValue) {
            null
        } else {
            buildOutputContainer(value, metadata, isLoadingInBackground = false)
        }
    }

    override suspend fun <R> dependsOnFlow(
        key: Any,
        vararg keys: Any,
        flow: () -> Flow<R>,
    ): R {
        return dependsOnContainerFlow(key, *keys) {
            flow().map<R, Container<R>>(::successContainer)
        }
    }

    override suspend fun <R> dependsOnContainerFlow(
        key: Any,
        vararg keys: Any,
        flow: () -> Flow<Container<R>>,
    ): R {
        return flowDependencyStore.dependsOn(key, *keys, flow = flow)
    }

    private fun buildOutputContainer(
        value: T,
        metadata: ContainerMetadata,
        isLoadingInBackground: Boolean,
    ): Container<T> {
        val backgroundLoadState = if (isLoadingInBackground) {
            BackgroundLoadState.Loading
        } else {
            BackgroundLoadState.Idle
        }
        return successContainer(
            value = value,
            metadata = BackgroundLoadMetadata(backgroundLoadState) + metadata + this.metadata
        )

    }

    internal suspend fun emitLastItem() {
        lastEmittedValue?.let { container ->
            flowCollector.emit(container)
        }
    }

}
