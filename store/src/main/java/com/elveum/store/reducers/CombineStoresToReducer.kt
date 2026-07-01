@file:Suppress("UNCHECKED_CAST")

package com.elveum.store.reducers

import com.elveum.container.reducer.ReducerOwner
import com.elveum.store.load.StoreResult
import com.elveum.store.load.combineStores
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted

/**
 * Combine all input [StoreResult] flows into a [StoreResultReducer]. Values from all flows are
 * transformed into output state by using [initialState] and [nextState] functions.
 */
public fun <State> combineStoresToReducer(
    flows: Iterable<Flow<StoreResult<*>>>,
    initialState: suspend (List<*>) -> State,
    nextState: suspend (State, List<*>) -> State = { _, values ->
        initialState(values)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): StoreResultReducer<State> {
    val combinedFlow = combineStores(flows.toList()) { values -> values }
    return combinedFlow.storeResultToReducer(
        initialState = initialState,
        nextState = nextState,
        scope = scope,
        started = started,
    )
}

/**
 * Combine 2 input [StoreResult] flows into a [StoreResultReducer]. Values from all flows are
 * transformed into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, State> combineStoresToReducer(
    flow1: Flow<StoreResult<T1>>,
    flow2: Flow<StoreResult<T2>>,
    initialState: suspend (T1, T2) -> State,
    nextState: suspend (State, T1, T2) -> State = { _, v1, v2 ->
        initialState(v1, v2)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): StoreResultReducer<State> {
    return combineStoresToReducer(
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
 * Combine 3 input [StoreResult] flows into a [StoreResultReducer]. Values from all flows are
 * transformed into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, State> combineStoresToReducer(
    flow1: Flow<StoreResult<T1>>,
    flow2: Flow<StoreResult<T2>>,
    flow3: Flow<StoreResult<T3>>,
    initialState: suspend (T1, T2, T3) -> State,
    nextState: suspend (State, T1, T2, T3) -> State = { _, v1, v2, v3 ->
        initialState(v1, v2, v3)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): StoreResultReducer<State> {
    return combineStoresToReducer(
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
 * Combine 4 input [StoreResult] flows into a [StoreResultReducer]. Values from all flows are
 * transformed into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, T4, State> combineStoresToReducer(
    flow1: Flow<StoreResult<T1>>,
    flow2: Flow<StoreResult<T2>>,
    flow3: Flow<StoreResult<T3>>,
    flow4: Flow<StoreResult<T4>>,
    initialState: suspend (T1, T2, T3, T4) -> State,
    nextState: suspend (State, T1, T2, T3, T4) -> State = { _, v1, v2, v3, v4 ->
        initialState(v1, v2, v3, v4)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): StoreResultReducer<State> {
    return combineStoresToReducer(
        flows = listOf(flow1, flow2, flow3, flow4),
        initialState = { values ->
            @Suppress("MagicNumber")
            initialState(values[0] as T1, values[1] as T2, values[2] as T3, values[3] as T4)
        },
        nextState = { oldState, values ->
            @Suppress("MagicNumber")
            nextState(oldState, values[0] as T1, values[1] as T2, values[2] as T3, values[3] as T4)
        },
        scope = scope,
        started = started,
    )
}

/**
 * Combine 5 input [StoreResult] flows into a [StoreResultReducer]. Values from all flows are
 * transformed into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, T4, T5, State> combineStoresToReducer(
    flow1: Flow<StoreResult<T1>>,
    flow2: Flow<StoreResult<T2>>,
    flow3: Flow<StoreResult<T3>>,
    flow4: Flow<StoreResult<T4>>,
    flow5: Flow<StoreResult<T5>>,
    initialState: suspend (T1, T2, T3, T4, T5) -> State,
    nextState: suspend (State, T1, T2, T3, T4, T5) -> State = { _, v1, v2, v3, v4, v5 ->
        initialState(v1, v2, v3, v4, v5)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): StoreResultReducer<State> {
    return combineStoresToReducer(
        flows = listOf(flow1, flow2, flow3, flow4, flow5),
        initialState = { values ->
            @Suppress("MagicNumber")
            initialState(values[0] as T1, values[1] as T2, values[2] as T3, values[3] as T4, values[4] as T5)
        },
        nextState = { oldState, values ->
            @Suppress("MagicNumber")
            nextState(oldState, values[0] as T1, values[1] as T2, values[2] as T3, values[3] as T4, values[4] as T5)
        },
        scope = scope,
        started = started,
    )
}

/**
 * Combine all input [StoreResult] flows into a [StoreResultReducer]. Values from all flows are
 * transformed into output state by using [initialState] and [nextState] functions.
 */
public fun <State> ReducerOwner.combineStoresToReducer(
    flows: Iterable<Flow<StoreResult<*>>>,
    initialState: suspend (List<*>) -> State,
    nextState: suspend (State, List<*>) -> State = { _, values ->
        initialState(values)
    },
): StoreResultReducer<State> {
    return combineStoresToReducer(flows, initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}

/**
 * Combine 2 input [StoreResult] flows into a [StoreResultReducer]. Values from all flows are
 * transformed into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, State> ReducerOwner.combineStoresToReducer(
    flow1: Flow<StoreResult<T1>>,
    flow2: Flow<StoreResult<T2>>,
    initialState: suspend (T1, T2) -> State,
    nextState: suspend (State, T1, T2) -> State = { _, v1, v2 ->
        initialState(v1, v2)
    },
): StoreResultReducer<State> {
    return combineStoresToReducer(flow1, flow2,
        initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}

/**
 * Combine 3 input [StoreResult] flows into a [StoreResultReducer]. Values from all flows are
 * transformed into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, State> ReducerOwner.combineStoresToReducer(
    flow1: Flow<StoreResult<T1>>,
    flow2: Flow<StoreResult<T2>>,
    flow3: Flow<StoreResult<T3>>,
    initialState: suspend (T1, T2, T3) -> State,
    nextState: suspend (State, T1, T2, T3) -> State = { _, v1, v2, v3 ->
        initialState(v1, v2, v3)
    },
): StoreResultReducer<State> {
    return combineStoresToReducer(flow1, flow2, flow3,
        initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}

/**
 * Combine 4 input [StoreResult] flows into a [StoreResultReducer]. Values from all flows are
 * transformed into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, T4, State> ReducerOwner.combineStoresToReducer(
    flow1: Flow<StoreResult<T1>>,
    flow2: Flow<StoreResult<T2>>,
    flow3: Flow<StoreResult<T3>>,
    flow4: Flow<StoreResult<T4>>,
    initialState: suspend (T1, T2, T3, T4) -> State,
    nextState: suspend (State, T1, T2, T3, T4) -> State = { _, v1, v2, v3, v4 ->
        initialState(v1, v2, v3, v4)
    },
): StoreResultReducer<State> {
    return combineStoresToReducer(flow1, flow2, flow3, flow4,
        initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}

/**
 * Combine 5 input [StoreResult] flows into a [StoreResultReducer]. Values from all flows are
 * transformed into output state by using [initialState] and [nextState] functions.
 */
public fun <T1, T2, T3, T4, T5, State> ReducerOwner.combineStoresToReducer(
    flow1: Flow<StoreResult<T1>>,
    flow2: Flow<StoreResult<T2>>,
    flow3: Flow<StoreResult<T3>>,
    flow4: Flow<StoreResult<T4>>,
    flow5: Flow<StoreResult<T5>>,
    initialState: suspend (T1, T2, T3, T4, T5) -> State,
    nextState: suspend (State, T1, T2, T3, T4, T5) -> State = { _, v1, v2, v3, v4, v5 ->
        initialState(v1, v2, v3, v4, v5)
    },
): StoreResultReducer<State> {
    return combineStoresToReducer(flow1, flow2, flow3, flow4, flow5,
        initialState, nextState, reducerCoroutineScope, reducerSharingStarted)
}
