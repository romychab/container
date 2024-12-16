package com.elveum.container

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Cast [MutableStateFlow] to [StateFlow].
 */
public fun <T> MutableStateFlow<T>.public(): StateFlow<T> {
    return this
}

/**
 * Set a new [value] to the [StateFlow] if it is [MutableStateFlow].
 * Otherwise, do nothing.
 */
public fun <T> StateFlow<T>.tryUpdate(value: T) {
    (this as? MutableStateFlow)?.value = value
}

/**
 * Calculate a new value for the [StateFlow] by using [updater] lambda
 * function if this [StateFlow] is [MutableStateFlow]. Otherwise, do nothing.
 */
public inline fun <T> StateFlow<T>.tryUpdate(updater: (T) -> T) {
    if (this is MutableStateFlow) {
        update(updater)
    }
}

/**
 * Almost the same operator as [Flow.map] but it returns [StateFlow] instance
 * instead of [Flow].
 */
public fun <T, R> StateFlow<T>.stateMap(mapper: (T) -> R): StateFlow<R> {
    return MapStateFlow(this, mapper)
}

/**
 * Almost the same operator as [combine] but it returns [StateFlow] instance
 * instead of [Flow].
 */
public fun <T1, T2, R> combineStates(
    stateFlow1: StateFlow<T1>,
    stateFlow2: StateFlow<T2>,
    transform: (T1, T2) -> R,
): StateFlow<R> {
    return combineStates(listOf(stateFlow1, stateFlow2)) { input ->
        @Suppress("UNCHECKED_CAST")
        transform(input[0] as T1, input[1] as T2)
    }

}

/**
 * Almost the same operator as [combine] but it returns [StateFlow] instance
 * instead of [Flow].
 */
public fun <T1, T2, T3, R> combineStates(
    stateFlow1: StateFlow<T1>,
    stateFlow2: StateFlow<T2>,
    stateFlow3: StateFlow<T3>,
    transform: (T1, T2, T3) -> R,
): StateFlow<R> {
    return combineStates(listOf(stateFlow1, stateFlow2, stateFlow3)) { input ->
        @Suppress("UNCHECKED_CAST")
        transform(input[0] as T1, input[1] as T2, input[2] as T3)
    }

}

/**
 * Almost the same operator as [combine] but it returns [StateFlow] instance
 * instead of [Flow].
 */
public fun <T1, T2, T3, T4, R> combineStates(
    stateFlow1: StateFlow<T1>,
    stateFlow2: StateFlow<T2>,
    stateFlow3: StateFlow<T3>,
    stateFlow4: StateFlow<T4>,
    transform: (T1, T2, T3, T4) -> R,
): StateFlow<R> {
    return combineStates(listOf(stateFlow1, stateFlow2, stateFlow3, stateFlow4)) { input ->
        @Suppress("UNCHECKED_CAST")
        transform(input[0] as T1, input[1] as T2, input[2] as T3, input[3] as T4)
    }
}

/**
 * Almost the same operator as [combine] but it returns [StateFlow] instance
 * instead of [Flow].
 */
public fun <T1, T2, T3, T4, T5, R> combineStates(
    stateFlow1: StateFlow<T1>,
    stateFlow2: StateFlow<T2>,
    stateFlow3: StateFlow<T3>,
    stateFlow4: StateFlow<T4>,
    stateFlow5: StateFlow<T5>,
    transform: (T1, T2, T3, T4, T5) -> R,
): StateFlow<R> {
    return combineStates(listOf(stateFlow1, stateFlow2, stateFlow3, stateFlow4, stateFlow5)) { input ->
        @Suppress("UNCHECKED_CAST")
        transform(input[0] as T1, input[1] as T2, input[2] as T3, input[3] as T4, input[4] as T5)
    }
}

/**
 * Combine multiple state-flows into one.
 */
public fun <R> combineStates(
    stateFlows: Iterable<StateFlow<*>>,
    transform: (List<*>) -> R,
): StateFlow<R> {
    return CombineStateFlow(
        flowsIterable = stateFlows,
        transform = transform,
    )
}

internal class MapStateFlow<T, R>(
    private val originFlow: StateFlow<T>,
    mapper: (T) -> R,
) : StateFlow<R> {

    private val cachingFunction = CachingFunction(mapper)

    override val replayCache: List<R>
        get() = listOf(value)
    override val value: R
        get() = cachingFunction(originFlow.value)

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        originFlow.collect {
            collector.emit(cachingFunction(it))
        }
    }
}

internal class CombineStateFlow<R>(
    flowsIterable: Iterable<StateFlow<*>>,
    transform: (List<*>) -> R,
) : StateFlow<R> {

    private val flows = flowsIterable.toList()

    private val currentInputs get() = flows.map { it.value }
    private val cachingFunction = CachingFunction(transform)

    override val replayCache: List<R>
        get() = listOf(value)
    override val value: R
        get() = cachingFunction(currentInputs)

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        while (true) {
            coroutineScope {
                val gatheredInputs = Array<Container<*>>(flows.size) { Container.Pending }
                flows.forEachIndexed { currentFlowIndex, currentStateFlow ->
                    launch {
                        currentStateFlow
                            .collect { item ->
                                gatheredInputs[currentFlowIndex] = Container.Success(item)
                                if (gatheredInputs.all { it is Container.Success }) {
                                    val inputs = gatheredInputs.filterIsInstance<Container.Success<*>>().map { it.value }
                                    val transformedValue = cachingFunction(inputs)
                                    collector.emit(transformedValue)
                                }
                            }
                    }
                }
            }
        }
    }

}
