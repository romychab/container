package com.elveum.container.reducer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine

/**
 * Combine all input flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State> combineToContainerReducer(
    flows: Iterable<Flow<*>>,
    initialState: suspend (List<*>) -> State,
    nextState: suspend (State, List<*>) -> State = { oldState, newValues ->
        initialState(newValues)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    val combinedFlow = combine(flows) { values -> values.toList() }
    return combinedFlow.toContainerReducer(
        initialState = initialState,
        nextState = nextState,
        scope = scope,
        started = started,
    )
}

/**
 * Combine 2 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, State> combineToContainerReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    initialState: suspend (T1, T2) -> State,
    nextState: suspend (State, T1, T2) -> State = { oldState, v1, v2 ->
        initialState(v1, v2)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    return combineToContainerReducer(
        flows = listOf(flow1, flow2),
        initialState = { values ->
            initialState(values[0] as T1, values[1] as T2)
        },
        nextState = { oldState, values ->
            nextState(oldState, values[0] as T1, values[1] as T2)
        },
        scope = scope,
        started = started,
    )
}

/**
 * Combine 3 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, State> combineToContainerReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    initialState: suspend (T1, T2, T3) -> State,
    nextState: suspend (State, T1, T2, T3) -> State = { oldState, v1, v2, v3 ->
        initialState(v1, v2, v3)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    return combineToContainerReducer(
        flows = listOf(flow1, flow2, flow3),
        initialState = { values ->
            initialState(values[0] as T1, values[1] as T2, values[2] as T3)
        },
        nextState = { oldState, values ->
            nextState(oldState, values[0] as T1, values[1] as T2, values[2] as T3)
        },
        scope = scope,
        started = started,
    )
}

/**
 * Combine 4 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, T4, State> combineToContainerReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    initialState: suspend (T1, T2, T3, T4) -> State,
    nextState: suspend (State, T1, T2, T3, T4) -> State = { oldState, v1, v2, v3, v4 ->
        initialState(v1, v2, v3, v4)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    return combineToContainerReducer(
        flows = listOf(flow1, flow2, flow3, flow4),
        initialState = { values ->
            initialState(values[0] as T1, values[1] as T2, values[2] as T3, values[3] as T4)
        },
        nextState = { oldState, values ->
            nextState(oldState, values[0] as T1, values[1] as T2, values[2] as T3, values[3] as T4)
        },
        scope = scope,
        started = started,
    )
}

/**
 * Combine 5 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, T4, T5, State> combineToContainerReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    initialState: suspend (T1, T2, T3, T4, T5) -> State,
    nextState: suspend (State, T1, T2, T3, T4, T5) -> State = { oldState, v1, v2, v3, v4, v5 ->
        initialState(v1, v2, v3, v4, v5)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    return combineToContainerReducer(
        flows = listOf(flow1, flow2, flow3, flow4, flow5),
        initialState = { values ->
            initialState(values[0] as T1, values[1] as T2, values[2] as T3, values[3] as T4, values[4] as T5)
        },
        nextState = { oldState, values ->
            nextState(oldState, values[0] as T1, values[1] as T2, values[2] as T3, values[3] as T4, values[4] as T5)
        },
        scope = scope,
        started = started,
    )
}

/**
 * Combine all input flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State> ReducerOwner.combineToContainerReducer(
    flows: Iterable<Flow<*>>,
    initialState: suspend (List<*>) -> State,
    nextState: suspend (State, List<*>) -> State = { oldState, newValues ->
        initialState(newValues)
    },
): ContainerReducer<State> {
    return combineToContainerReducer(flows, initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}

/**
 * Combine 2 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, State> ReducerOwner.combineToContainerReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    initialState: suspend (T1, T2) -> State,
    nextState: suspend (State, T1, T2) -> State = { oldState, v1, v2 ->
        initialState(v1, v2)
    },
): ContainerReducer<State> {
    return combineToContainerReducer(flow1, flow2, initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}

/**
 * Combine 3 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, State> ReducerOwner.combineToContainerReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    initialState: suspend (T1, T2, T3) -> State,
    nextState: suspend (State, T1, T2, T3) -> State = { oldState, v1, v2, v3 ->
        initialState(v1, v2, v3)
    },
): ContainerReducer<State> {
    return combineToContainerReducer(flow1, flow2, flow3, initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}

/**
 * Combine 4 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, T4, State> ReducerOwner.combineToContainerReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    initialState: suspend (T1, T2, T3, T4) -> State,
    nextState: suspend (State, T1, T2, T3, T4) -> State = { oldState, v1, v2, v3, v4 ->
        initialState(v1, v2, v3, v4)
    },
): ContainerReducer<State> {
    return combineToContainerReducer(flow1, flow2, flow3, flow4, initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}

/**
 * Combine 5 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, T4, T5, State> ReducerOwner.combineToContainerReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    initialState: suspend (T1, T2, T3, T4, T5) -> State,
    nextState: suspend (State, T1, T2, T3, T4, T5) -> State = { oldState, v1, v2, v3, v4, v5 ->
        initialState(v1, v2, v3, v4, v5)
    },
): ContainerReducer<State> {
    return combineToContainerReducer(flow1, flow2, flow3, flow4, flow5, initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}
