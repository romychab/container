package com.elveum.container.reducer

import com.elveum.container.Container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted

/**
 * Combine 2 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State, T1, T2> combineContainersToReducer(
    flow1: Flow<Container<T1>>,
    flow2: Flow<Container<T2>>,
    initialState: suspend (T1, T2) -> State,
    nextState: suspend (State, T1, T2) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    @Suppress("UNCHECKED_CAST")
    return MapperContainerReducer(
        inputFlows = listOf(flow1, flow2),
        scope = scope,
        started = started,
        initialValue = { list ->
            initialState(list[0] as T1, list[1] as T2)
        },
        nextValue = { state, list ->
            nextState(state, list[0] as T1, list[1] as T2)
        }
    )
}

/**
 * Combine 3 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State, T1, T2, T3> combineContainersToReducer(
    flow1: Flow<Container<T1>>,
    flow2: Flow<Container<T2>>,
    flow3: Flow<Container<T3>>,
    initialState: suspend (T1, T2, T3) -> State,
    nextState: suspend (State, T1, T2, T3) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    @Suppress("UNCHECKED_CAST")
    return MapperContainerReducer(
        inputFlows = listOf(flow1, flow2, flow3),
        scope = scope,
        started = started,
        initialValue = { list ->
            initialState(list[0] as T1, list[1] as T2, list[2] as T3)
        },
        nextValue = { state, list ->
            nextState(state, list[0] as T1, list[1] as T2, list[2] as T3)
        }
    )
}

/**
 * Combine 4 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State, T1, T2, T3, T4> combineContainersToReducer(
    flow1: Flow<Container<T1>>,
    flow2: Flow<Container<T2>>,
    flow3: Flow<Container<T3>>,
    flow4: Flow<Container<T4>>,
    initialState: suspend (T1, T2, T3, T4) -> State,
    nextState: suspend (State, T1, T2, T3, T4) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    @Suppress("UNCHECKED_CAST")
    return MapperContainerReducer(
        inputFlows = listOf(flow1, flow2, flow3, flow4),
        scope = scope,
        started = started,
        initialValue = { list ->
            initialState(list[0] as T1, list[1] as T2, list[2] as T3, list[3] as T4)
        },
        nextValue = { state, list ->
            nextState(state, list[0] as T1, list[1] as T2, list[2] as T3, list[3] as T4)
        }
    )
}

/**
 * Combine 5 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State, T1, T2, T3, T4, T5> combineContainersToReducer(
    flow1: Flow<Container<T1>>,
    flow2: Flow<Container<T2>>,
    flow3: Flow<Container<T3>>,
    flow4: Flow<Container<T4>>,
    flow5: Flow<Container<T5>>,
    initialState: suspend (T1, T2, T3, T4, T5) -> State,
    nextState: suspend (State, T1, T2, T3, T4, T5) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    @Suppress("UNCHECKED_CAST")
    return MapperContainerReducer(
        inputFlows = listOf(flow1, flow2, flow3, flow4, flow5),
        scope = scope,
        started = started,
        initialValue = { list ->
            initialState(list[0] as T1, list[1] as T2, list[2] as T3, list[3] as T4, list[4] as T5)
        },
        nextValue = { state, list ->
            nextState(state, list[0] as T1, list[1] as T2, list[2] as T3, list[3] as T4, list[4] as T5)
        }
    )
}

/**
 * Combine all input flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State> combineContainersToReducer(
    flows: Iterable<Flow<Container<*>>>,
    initialState: suspend (List<*>) -> State,
    nextState: suspend (State, List<*>) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    return MapperContainerReducer(
        inputFlows = flows,
        scope = scope,
        started = started,
        initialValue = initialState,
        nextValue = nextState,
    )
}
