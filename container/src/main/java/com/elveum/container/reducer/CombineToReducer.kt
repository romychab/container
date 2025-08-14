package com.elveum.container.reducer

import com.elveum.container.successContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map

/**
 * Combine 2 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State, T1, T2> combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    initialState: suspend (T1, T2) -> State,
    nextState: suspend (State, T1, T2) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    return combineContainersToReducer(
        flow1 = flow1.map(::successContainer),
        flow2 = flow2.map(::successContainer),
        initialState = initialState,
        nextState = nextState,
        scope = scope,
        started = started,
    )
}

/**
 * Combine 3 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State, T1, T2, T3> combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    initialState: suspend (T1, T2, T3) -> State,
    nextState: suspend (State, T1, T2, T3) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    return combineContainersToReducer(
        flow1 = flow1.map(::successContainer),
        flow2 = flow2.map(::successContainer),
        flow3 = flow3.map(::successContainer),
        initialState = initialState,
        nextState = nextState,
        scope = scope,
        started = started,
    )
}

/**
 * Combine 4 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State, T1, T2, T3, T4> combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    initialState: suspend (T1, T2, T3, T4) -> State,
    nextState: suspend (State, T1, T2, T3, T4) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    return combineContainersToReducer(
        flow1 = flow1.map(::successContainer),
        flow2 = flow2.map(::successContainer),
        flow3 = flow3.map(::successContainer),
        flow4 = flow4.map(::successContainer),
        initialState = initialState,
        nextState = nextState,
        scope = scope,
        started = started,
    )
}

/**
 * Combine 5 flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State, T1, T2, T3, T4, T5> combineToReducer(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    initialState: suspend (T1, T2, T3, T4, T5) -> State,
    nextState: suspend (State, T1, T2, T3, T4, T5) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    return combineContainersToReducer(
        flow1 = flow1.map(::successContainer),
        flow2 = flow2.map(::successContainer),
        flow3 = flow3.map(::successContainer),
        flow4 = flow4.map(::successContainer),
        flow5 = flow5.map(::successContainer),
        initialState = initialState,
        nextState = nextState,
        scope = scope,
        started = started,
    )
}

/**
 * Combine all input flows into a [ContainerReducer]. Values from all flows are transformed
 * into output state by using [initialState] and [nextState] functions.
 */
public fun <State> combineToReducer(
    flows: Iterable<Flow<*>>,
    initialState: suspend (List<*>) -> State,
    nextState: suspend (State, List<*>) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    return MapperContainerReducer(
        inputFlows = flows.map { flow -> flow.map(::successContainer) },
        scope = scope,
        started = started,
        initialValue = initialState,
        nextValue = nextState,
    )
}
