package com.elveum.container

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

/**
 * Execute a [block] and wrap its result into [Container.Completed].
 */
public inline fun <T> containerOf(
    metadata: ContainerMetadata = defaultMetadata(),
    block: () -> T,
): Container.Completed<T> {
    return try {
        successContainer(block(), metadata)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        errorContainer(e, metadata)
    }
}

/**
 * A wrapper for [flow] call which additionally encapsulates all emitted
 * values into [Container.Success]. If the flow builder [block] fails,
 * its exception is encapsulated into [Container.Error].
 */
public inline fun <T> containerFlowOf(
    metadata: ContainerMetadata = defaultMetadata(),
    crossinline block: FlowCollector<T>.() -> Unit
): Flow<Container<T>> {
    return flow {
        val containerFlowCollector = FlowCollector<T> { value ->
            this@flow.emit(successContainer(value, metadata))
        }
        emit(pendingContainer())
        try {
            block.invoke(containerFlowCollector)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(errorContainer(e, metadata))
        }
    }
}
