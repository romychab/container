package com.elveum.container.subject.lazy

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.FlowCollector

internal class ChannelFlowCollector<T>(
    private val originProducerScope: ProducerScope<T>
) : ProducerScope<T> by originProducerScope, FlowCollector<T> {
    override suspend fun emit(value: T) {
        send(value)
    }
}
