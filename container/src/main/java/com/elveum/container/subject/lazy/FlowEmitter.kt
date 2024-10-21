package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.Emitter
import com.elveum.container.LoadTrigger
import com.elveum.container.SourceIndicator
import com.elveum.container.subject.FlowSubject
import kotlinx.coroutines.flow.FlowCollector

internal class FlowEmitter<T>(
    override val loadTrigger: LoadTrigger,
    private val flowCollector: FlowCollector<Container<T>>,
    private val flowSubject: FlowSubject<T>? = null,
) : Emitter<T> {

    private var _hasEmittedItems = false
    val hasEmittedItems get() = _hasEmittedItems

    override suspend fun emit(item: T, source: SourceIndicator) {
        flowSubject?.onNext(item)
        flowCollector.emit(Container.Success(item, source))
        _hasEmittedItems = true
    }
}