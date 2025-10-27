package com.elveum.container.reducer

import com.elveum.container.Container
import com.elveum.container.ContainerMapperScope
import com.elveum.container.successContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * A reducer that manages a reactive state. State values can be observed
 * by using [stateFlow] property. Also, the current state can be updated
 * via either [updateState] method or [updateContainer] method.
 */
public interface ContainerReducer<State> {

    /**
     * Flow containing the most actual state.
     */
    public val stateFlow: StateFlow<Container<State>>

    /**
     * Update the state value held by [stateFlow]. The state is updated only
     * if the current container is [Container.Success].
     */
    public fun updateState(transform: suspend ContainerMapperScope.(State) -> State)

    /**
     * Update the whole container held by [stateFlow].
     */
    public fun updateContainer(transform: suspend (Container<State>) -> Container<State>)

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
public fun <State> Flow<State>.toReducer(
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    @Suppress("UNCHECKED_CAST")
    return SimpleContainerReducer(
        scope = scope,
        started = started,
        inputFlow = map(::successContainer)
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
public fun <T, State> Flow<T>.toReducer(
    initialState: suspend (T) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
    nextState: suspend (State, T) -> State = { _, value ->
        initialState(value)
    },
): ContainerReducer<State> {
    @Suppress("UNCHECKED_CAST")
    return MapperContainerReducer(
        scope = scope,
        inputFlows = listOf(map(::successContainer)),
        initialValue = { initialState(it.first() as T) },
        nextValue = { state, list -> nextState(state, list.first() as T) },
        started = started,
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
public fun <State> Flow<Container<State>>.containerToReducer(
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    return SimpleContainerReducer(
        scope = scope,
        inputFlow = this,
        started = started,
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
public fun <T, State> Flow<Container<T>>.containerToReducer(
    initialState: suspend (T) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
    nextState: suspend (State, T) -> State = { _, value ->
        initialState(value)
    },
): ContainerReducer<State> {
    @Suppress("UNCHECKED_CAST")
    return MapperContainerReducer(
        scope = scope,
        inputFlows = listOf(this),
        initialValue = { initialState(it.first() as T) },
        nextValue = { state, list -> nextState(state, list.first() as T)},
        started = started,
    )
}

