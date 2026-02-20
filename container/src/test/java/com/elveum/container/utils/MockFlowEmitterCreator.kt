package com.elveum.container.utils

import com.elveum.container.Container
import com.elveum.container.LoadTrigger
import com.elveum.container.subject.lazy.FlowEmitter
import com.elveum.container.subject.lazy.LoadTask
import com.elveum.container.subject.lazy.LoadTask.ExecuteParams
import kotlinx.coroutines.flow.FlowCollector

internal class MockFlowEmitterCreator(
    private val onCreate: (FlowCollector<Container<String>>, ExecuteParams<String>) -> FlowEmitter<String>
) : LoadTask.FlowEmitterCreator<String>(
    null, { LoadTrigger.NewLoad },
) {
    override fun create(
        flowCollector: FlowCollector<Container<String>>,
        executeParams: ExecuteParams<String>
    ): FlowEmitter<String> = onCreate(flowCollector, executeParams)
}