package com.elveum.container.reducer

import com.elveum.container.reducer.impl.ReducerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine

/**
 * Combine all input flows into a [Reducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State> combineToReducer(
    flows: Iterable<Flow<*>>,
    initialState: State,
    nextState: suspend (State, List<*>) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): Reducer<State> = ReducerImpl(
    initialState = initialState,
    originFlow = combine(flows) { values -> values.toList() },
    combiner = nextState,
    scope = scope,
    started = started,
)

/**
 * Combine 2 flows into a [Reducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, State> combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    initialState: State,
    nextState: suspend (State, T1, T2) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): Reducer<State> {
    return combineToReducer(
        flows = listOf(flow1, flow2),
        initialState = initialState,
        nextState = { oldState, values ->
            nextState(oldState, values[0] as T1, values[1] as T2)
        },
        scope = scope,
        started = started,
    )
}

/**
 * Combine 3 flows into a [Reducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, State> combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    initialState: State,
    nextState: suspend (State, T1, T2, T3) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): Reducer<State> {
    return combineToReducer(
        flows = listOf(flow1, flow2, flow3),
        initialState = initialState,
        nextState = { oldState, values ->
            nextState(oldState, values[0] as T1, values[1] as T2, values[2] as T3)
        },
        scope = scope,
        started = started,
    )
}

/**
 * Combine 4 flows into a [Reducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, T4, State> combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    initialState: State,
    nextState: suspend (State, T1, T2, T3, T4) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): Reducer<State> {
    return combineToReducer(
        flows = listOf(flow1, flow2, flow3, flow4),
        initialState = initialState,
        nextState = { oldState, values ->
            nextState(oldState, values[0] as T1, values[1] as T2, values[2] as T3, values[3] as T4)
        },
        scope = scope,
        started = started,
    )
}

/**
 * Combine 5 flows into a [Reducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, T4, T5, State> combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    initialState: State,
    nextState: suspend (State, T1, T2, T3, T4, T5) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): Reducer<State> {
    return combineToReducer(
        flows = listOf(flow1, flow2, flow3, flow4, flow5),
        initialState = initialState,
        nextState = { oldState, values ->
            nextState(oldState, values[0] as T1, values[1] as T2, values[2] as T3, values[3] as T4, values[4] as T5)
        },
        scope = scope,
        started = started,
    )
}

/**
 * Combine all input flows into a [Reducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State> ReducerOwner.combineToReducer(
    flows: Iterable<Flow<*>>,
    initialState: State,
    nextState: suspend (State, List<*>) -> State,
): Reducer<State> {
    return combineToReducer(flows, initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}

/**
 * Combine 2 flows into a [Reducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, State> ReducerOwner.combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    initialState: State,
    nextState: suspend (State, T1, T2) -> State,
): Reducer<State> {
    return combineToReducer(flow1, flow2, initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}

/**
 * Combine 3 flows into a [Reducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, State> ReducerOwner.combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    initialState: State,
    nextState: suspend (State, T1, T2, T3) -> State,
): Reducer<State> {
    return combineToReducer(flow1, flow2, flow3, initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}

/**
 * Combine 4 flows into a [Reducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, T4, State> ReducerOwner.combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    initialState: State,
    nextState: suspend (State, T1, T2, T3, T4) -> State,
): Reducer<State> {
    return combineToReducer(flow1, flow2, flow3, flow4, initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}

/**
 * Combine 5 flows into a [Reducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, T4, T5, State> ReducerOwner.combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    initialState: State,
    nextState: suspend (State, T1, T2, T3, T4, T5) -> State,
): Reducer<State> {
    return combineToReducer(flow1, flow2, flow3, flow4, flow5, initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}
