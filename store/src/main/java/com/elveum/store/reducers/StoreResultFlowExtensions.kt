package com.elveum.store.reducers

import com.elveum.container.reducer.toReducer
import com.elveum.store.internal.reducers.StoreResultReducerImpl
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

/**
 * Create a [StoreResultReducer] from the origin flow.
 *
 * A [StoreResultReducer] converts the origin flow into a [StateFlow] holding state
 * wrapped into [StoreResult].
 */
public fun <T, State> Flow<T>.toStoreResultReducer(
    initialState: suspend (T) -> State,
    nextState: suspend (State, T) -> State = { oldState, newValue ->
        initialState(newValue)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): StoreResultReducer<State> {
    val reducer = toReducer<T, StoreResult<State>>(
        initialState = { StoreResult.Loading },
        nextState = { oldResult, newValue ->
            val newState = when (oldResult) {
                StoreResult.Loading -> initialState(newValue)
                is StoreResult.Failed -> initialState(newValue)
                is StoreResult.Loaded -> nextState(oldResult.value, newValue)
            }
            StoreResult.Loaded(newState)
        },
        scope = scope,
        started = started,
    )
    return StoreResultReducerImpl(reducer)
}

/**
 * Create a [StoreResultReducer] from the origin flow.
 *
 * A [StoreResultReducer] converts the origin flow into a [StateFlow] holding state
 * wrapped into [StoreResult].
 */
public fun <T> Flow<T>.toStoreResultReducer(
    scope: CoroutineScope,
    started: SharingStarted,
): StoreResultReducer<T> {
    return toStoreResultReducer(
        initialState = { it },
        scope = scope,
        started = started,
    )
}

/**
 * Create a [StoreResultReducer] from existing [StoreResult] flow.
 */
public fun <T, State> Flow<StoreResult<T>>.storeResultToReducer(
    initialState: suspend (T) -> State,
    nextState: suspend (State, T) -> State = { oldState, newValue ->
        initialState(newValue)
    },
    scope: CoroutineScope,
    started: SharingStarted,
): StoreResultReducer<State> {
    val reducer = toReducer<StoreResult<T>, StoreResult<State>>(
        initialState = { StoreResult.Loading },
        nextState = { oldStateResult, newResult ->
            when (newResult) {
                StoreResult.Loading -> { StoreResult.Loading }
                is StoreResult.Failed -> { StoreResult.Failed(newResult.exception, newResult.metadata) }
                is StoreResult.Loaded -> {
                    val newState: State = when (oldStateResult) {
                        StoreResult.Loading, is StoreResult.Failed -> {
                            initialState(newResult.value)
                        }
                        is StoreResult.Loaded -> {
                            nextState(oldStateResult.value, newResult.value)
                        }
                    }
                    StoreResult.Loaded(newState, newResult.metadata)
                }
            }
        },
        scope = scope,
        started = started,
    )
    return StoreResultReducerImpl(reducer)
}

/**
 * Create a [StoreResultReducer] from existing [StoreResult] flow.
 */
public fun <T> Flow<StoreResult<T>>.storeResultToReducer(
    scope: CoroutineScope,
    started: SharingStarted,
): StoreResultReducer<T> {
    return storeResultToReducer(
        initialState = { it },
        scope = scope,
        started = started,
    )
}
