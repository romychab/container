package com.elveum.container.reducer

import com.elveum.container.Container
import com.elveum.container.pendingContainer
import com.elveum.container.reducer.impl.ContainerReducerImpl
import com.elveum.container.reducer.impl.ReducerImpl
import com.elveum.container.successContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
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
public fun <T, State> Flow<T>.toReducer(
    initialState: State,
    nextState: suspend (State, T) -> State,
    scope: CoroutineScope,
    started: SharingStarted,
): Reducer<State> {
    return ReducerImpl(
        initialState = initialState,
        originFlow = this,
        combiner = nextState,
        scope = scope,
        started = started,
    )
}

/**
 * Create a [Reducer] from the origin flow.
 *
 * A [Reducer] converts the origin flow into a [StateFlow] that can
 * be observed via [Reducer.stateFlow] property.
 *
 * Additionally, values in the [ContainerReducer.stateFlow] can be updated by using
 * [Reducer.update] method.
 */
public fun <T> Flow<T>.toReducer(
    initialState: T,
    scope: CoroutineScope,
    started: SharingStarted,
): Reducer<T> {
    return toReducer(
        initialState = initialState,
        nextState = { oldState, newValue -> newValue },
        scope = scope,
        started = started,
    )
}

/**
 * Create a [ContainerReducer] from the origin flow.
 *
 * A [ContainerReducer] converts the origin flow into a [StateFlow] holding state
 * wrapped into [Container].
 */
public fun <T, State> Flow<T>.toContainerReducer(
    initialState: suspend (T) -> State,
    nextState: suspend (State, T) -> State = { oldState, newValue ->
        initialState(newValue)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    val reducer = toReducer<T, Container<State>>(
        initialState = pendingContainer(),
        nextState = { oldContainer, newValue ->
            val newState = oldContainer.fold(
                onPending = { initialState(newValue) },
                onError = { initialState(newValue) },
                onSuccess = { nextState(it, newValue) }
            )
            successContainer(newState)
        },
        scope = scope,
        started = started,
    )
    return ContainerReducerImpl(reducer)
}

/**
 * Create a [ContainerReducer] from the origin flow.
 *
 * A [ContainerReducer] converts the origin flow into a [StateFlow] holding state
 * wrapped into [Container].
 */
public fun <T> Flow<T>.toContainerReducer(
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<T> {
    return toContainerReducer(
        initialState = { it },
        scope = scope,
        started = started,
    )
}

/**
 * Create a [ContainerReducer] from existing [Container] flow.
 */
public fun <T, State> Flow<Container<T>>.containerToReducer(
    initialState: suspend (T) -> State,
    nextState: suspend (State, T) -> State = { oldState, newValue ->
        initialState(newValue)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<State> {
    val reducer = toReducer<Container<T>, Container<State>>(
        initialState = pendingContainer(),
        nextState = { oldStateContainer, newContainer ->
            newContainer.fold(
                onPending = ::pendingContainer,
                onError = { errorContainer(it) },
                onSuccess = { newInputValue ->
                    val newState: State = when (oldStateContainer) {
                        Container.Pending, is Container.Error -> {
                            initialState(newInputValue)
                        }
                        is Container.Success -> {
                            nextState(oldStateContainer.value, newInputValue)
                        }
                    }
                    successContainer(newState)
                }
            )
        },
        scope = scope,
        started = started,
    )
    return ContainerReducerImpl(reducer)
}

/**
 * Create a [ContainerReducer] from existing [Container] flow.
 */
public fun <T> Flow<Container<T>>.containerToReducer(
    scope: CoroutineScope,
    started: SharingStarted,
): ContainerReducer<T> {
    return containerToReducer(
        initialState = { it },
        scope = scope,
        started = started,
    )
}

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
