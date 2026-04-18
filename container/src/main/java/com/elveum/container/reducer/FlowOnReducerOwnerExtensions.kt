package com.elveum.container.reducer

import com.elveum.container.Container
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Create a [Reducer] from the origin flow.
 *
 * A [Reducer] converts the origin flow into a [StateFlow] that can
 * be observed via [Reducer.stateFlow] property.
 *
 * Additionally, values in the [ContainerReducer.stateFlow] can be updated by using
 * [Reducer.update] method.
 */
context(owner: ReducerOwner)
public fun <T, State> Flow<T>.toReducer(
    initialState: State,
    nextState: suspend (State, T) -> State,
): Reducer<State> {
    return toReducer(initialState, nextState, owner.reducerCoroutineScope, owner.reducerSharingStarted)
}

context(owner: ReducerOwner)
public fun <T> Flow<T>.toReducer(initialState: T): Reducer<T> {
    return toReducer(
        initialState = initialState,
        scope = owner.reducerCoroutineScope,
        started = owner.reducerSharingStarted,
    )
}

/**
 * Create a [ContainerReducer] from the origin flow.
 *
 * A [ContainerReducer] converts the origin flow into a [StateFlow] holding state
 * wrapped into [Container].
 */
context(owner: ReducerOwner)
public fun <T, State> Flow<T>.toContainerReducer(
    initialState: suspend (T) -> State,
    nextState: suspend (State, T) -> State = { oldState, newValue ->
        initialState(newValue)
    },
): ContainerReducer<State> {
    return toContainerReducer(initialState, nextState, owner.reducerCoroutineScope, owner.reducerSharingStarted)
}

/**
 * Create a [ContainerReducer] from the origin flow.
 *
 * A [ContainerReducer] converts the origin flow into a [StateFlow] holding state
 * wrapped into [Container].
 */
context(owner: ReducerOwner)
public fun <T> Flow<T>.toContainerReducer(): ContainerReducer<T> {
    return toContainerReducer(owner.reducerCoroutineScope, owner.reducerSharingStarted)
}

/**
 * Create a [ContainerReducer] from existing [Container] flow.
 */
context(owner: ReducerOwner)
public fun <T, State> Flow<Container<T>>.containerToReducer(
    initialState: suspend (T) -> State,
    nextState: suspend (State, T) -> State = { oldState, newValue ->
        initialState(newValue)
    },
): ContainerReducer<State> {
    return containerToReducer(initialState, nextState, owner.reducerCoroutineScope, owner.reducerSharingStarted)
}

/**
 * Create a [ContainerReducer] from existing [Container] flow.
 */
context(owner: ReducerOwner)
public fun <T> Flow<Container<T>>.containerToReducer(): ContainerReducer<T> {
    return containerToReducer(owner.reducerCoroutineScope, owner.reducerSharingStarted)
}
