package com.elveum.container.reducer.owner

import com.elveum.container.Container
import com.elveum.container.reducer.ContainerReducer
import com.elveum.container.reducer.containerToReducer
import com.elveum.container.reducer.toReducer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Create a [ContainerReducer] from the origin flow.
 *
 * A [ContainerReducer] converts the origin flow into a [StateFlow] that can
 * be observed via [ContainerReducer.stateFlow] property.
 *
 * Additionally, values in the [ContainerReducer.stateFlow] can be updated by using
 * [ContainerReducer.updateContainer] or [ContainerReducer.updateState] methods.
 */
context(owner: ReducerOwner)
public fun <State> Flow<State>.toReducer(): ContainerReducer<State> {
    return toReducer(
        scope = owner.reducerScope,
        started = owner.sharingStarted,
    )
}

/**
 * Create a [ContainerReducer] from the origin flow.
 *
 * A [ContainerReducer] converts the origin flow into a [StateFlow] that can
 * be observed via [ContainerReducer.stateFlow] property.
 *
 * Additionally, values in the [ContainerReducer.stateFlow] can be updated by using
 * [ContainerReducer.updateContainer] or [ContainerReducer.updateState] methods.
 */
context(owner: ReducerOwner)
public fun <T, State> Flow<T>.toReducer(
    initialState: suspend (T) -> State,
    nextState: suspend (State, T) -> State = { _, value ->
        initialState(value)
    },
): ContainerReducer<State> {
    return toReducer(
        scope = owner.reducerScope,
        started = owner.sharingStarted,
        initialState = initialState,
        nextState = nextState,
    )
}

/**
 * Create a [ContainerReducer] from the origin flow.
 *
 * A [ContainerReducer] converts the origin flow into a [StateFlow] that can
 * be observed via [ContainerReducer.stateFlow] property.
 *
 * Additionally, values in the [ContainerReducer.stateFlow] can be updated by using
 * [ContainerReducer.updateContainer] or [ContainerReducer.updateState] methods.
 */
context(owner: ReducerOwner)
public fun <State> Flow<Container<State>>.containerToReducer(): ContainerReducer<State> {
    return containerToReducer(
        scope = owner.reducerScope,
        started = owner.sharingStarted,
    )
}

/**
 * Create a [ContainerReducer] from the origin flow.
 *
 * A [ContainerReducer] converts the origin flow into a [StateFlow] that can
 * be observed via [ContainerReducer.stateFlow] property.
 *
 * Additionally, values in the [ContainerReducer.stateFlow] can be updated by using
 * [ContainerReducer.updateContainer] or [ContainerReducer.updateState] methods.
 */
context(owner: ReducerOwner)
public fun <T, State> Flow<Container<T>>.containerToReducer(
    initialState: suspend (T) -> State,
    nextState: suspend (State, T) -> State = { _, value ->
        initialState(value)
    },
): ContainerReducer<State> {
    return containerToReducer(
        initialState = initialState,
        nextState = nextState,
        scope = owner.reducerScope,
        started = owner.sharingStarted,
    )
}

