package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.Emitter
import com.elveum.container.EmptyMetadata
import com.elveum.container.IsLoadingInBackgroundMetadata
import com.elveum.container.LoadTrigger
import com.elveum.container.LoadUuidMetadata
import com.elveum.container.getOrNull
import com.elveum.container.pendingContainer
import com.elveum.container.subject.FlowSubject
import com.elveum.container.successContainer
import kotlinx.coroutines.flow.FlowCollector

internal class FlowEmitter<T>(
    override val loadTrigger: LoadTrigger,
    private val flowCollector: FlowCollector<Container<T>>,
    private val executeParams: LoadTask.ExecuteParams<T> = LoadTask.ExecuteParams(),
    private val flowSubject: FlowSubject<T>? = null,
) : Emitter<T> {

    private var lastEmittedValue: Container<Container<T>> = Container.Pending
    private var _hasEmittedValues = false
    val hasEmittedValues get() = _hasEmittedValues

    private val loadUuid get() = executeParams.loadUuid

    override suspend fun emit(value: T, metadata: ContainerMetadata, isLastValue: Boolean) {
        _hasEmittedValues = true
        flowSubject?.onNext(value)
        val loadUuidMetadata = loadUuid.takeIf { it.isNotBlank() }
            ?.let(::LoadUuidMetadata)
            ?: EmptyMetadata
        flowCollector.emit(successContainer(value, loadUuidMetadata + IsLoadingInBackgroundMetadata(!isLastValue)) + metadata)
        lastEmittedValue = if (isLastValue) {
            pendingContainer()
        } else {
            successContainer(
                successContainer(value, loadUuidMetadata + IsLoadingInBackgroundMetadata(false) + metadata)
            )
        }
    }

    internal suspend fun emitLastItem() {
        lastEmittedValue.getOrNull()?.let { container ->
            flowCollector.emit(container)
        }
    }

}
