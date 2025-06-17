package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.Emitter
import com.elveum.container.LoadTrigger
import com.elveum.container.SourceType
import com.elveum.container.getOrNull
import com.elveum.container.pendingContainer
import com.elveum.container.subject.FlowSubject
import com.elveum.container.successContainer
import kotlinx.coroutines.flow.FlowCollector

internal class FlowEmitter<T>(
    override val loadTrigger: LoadTrigger,
    private val flowCollector: FlowCollector<Container<T>>,
    private val flowSubject: FlowSubject<T>? = null,
) : Emitter<T> {

    private var lastEmittedValue: Container<Container<T>> = Container.Pending
    private var _hasEmittedValues = false
    val hasEmittedValues get() = _hasEmittedValues

    override suspend fun emit(value: T, source: SourceType, isLastValue: Boolean) {
        _hasEmittedValues = true
        flowSubject?.onNext(value)
        flowCollector.emit(successContainer(value, source, isLoadingInBackground = !isLastValue))
        lastEmittedValue = if (isLastValue) {
            pendingContainer()
        } else {
            successContainer(
                successContainer(value, source, isLoadingInBackground = false)
            )
        }
    }

    internal suspend fun emitLastItem() {
        lastEmittedValue.getOrNull()?.let { container ->
            flowCollector.emit(container)
        }
    }

}
