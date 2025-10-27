package com.elveum.container.reducer.owner

import com.elveum.container.reducer.ContainerReducer
import com.elveum.container.reducer.combineToReducer
import kotlinx.coroutines.flow.Flow

/**
 * Combine 2 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State, T1, T2> ReducerOwner.combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    initialState: suspend (T1, T2) -> State,
    nextState: suspend (State, T1, T2) -> State = { _, v1, v2 ->
        initialState(v1, v2)
    },
): ContainerReducer<State> {
    return combineToReducer(
        flow1 = flow1,
        flow2 = flow2,
        initialState = initialState,
        nextState = nextState,
        scope = reducerScope,
        started = sharingStarted,
    )
}

/**
 * Combine 3 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State, T1, T2, T3> ReducerOwner.combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    initialState: suspend (T1, T2, T3) -> State,
    nextState: suspend (State, T1, T2, T3) -> State = { _, v1, v2, v3 ->
        initialState(v1, v2, v3)
    },
): ContainerReducer<State> {
    return combineToReducer(
        flow1 = flow1,
        flow2 = flow2,
        flow3 = flow3,
        initialState = initialState,
        nextState = nextState,
        scope = reducerScope,
        started = sharingStarted,
    )
}

/**
 * Combine 4 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State, T1, T2, T3, T4> ReducerOwner.combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    initialState: suspend (T1, T2, T3, T4) -> State,
    nextState: suspend (State, T1, T2, T3, T4) -> State = { _, v1, v2, v3, v4 ->
        initialState(v1, v2, v3, v4)
    },
): ContainerReducer<State> {
    return combineToReducer(
        flow1 = flow1,
        flow2 = flow2,
        flow3 = flow3,
        flow4 = flow4,
        initialState = initialState,
        nextState = nextState,
        scope = reducerScope,
        started = sharingStarted,
    )
}

/**
 * Combine 5 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State, T1, T2, T3, T4, T5> ReducerOwner.combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    initialState: suspend (T1, T2, T3, T4, T5) -> State,
    nextState: suspend (State, T1, T2, T3, T4, T5) -> State = { _, v1, v2, v3, v4, v5 ->
        initialState(v1, v2, v3, v4, v5)
    },
): ContainerReducer<State> {
    return combineToReducer(
        flow1 = flow1,
        flow2 = flow2,
        flow3 = flow3,
        flow4 = flow4,
        flow5 = flow5,
        initialState = initialState,
        nextState = nextState,
        scope = reducerScope,
        started = sharingStarted,
    )
}

/**
 * Combine all input flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State> ReducerOwner.combineToReducer(
    flows: Iterable<Flow<*>>,
    initialState: suspend (List<*>) -> State,
    nextState: suspend (State, List<*>) -> State = { _, values ->
        initialState(values)
    },
): ContainerReducer<State> {
    return combineToReducer(
        flows = flows,
        initialState = initialState,
        nextState = nextState,
        scope = reducerScope,
        started = sharingStarted,
    )
}
