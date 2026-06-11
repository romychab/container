package com.elveum.store.reducers

import com.elveum.container.reducer.ReducerOwner
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Create a [StoreResultReducer] from the origin flow.
 *
 * A [StoreResultReducer] converts the origin flow into a [StateFlow] holding state
 * wrapped into [StoreResult].
 */
context(owner: ReducerOwner)
public fun <T, State> Flow<T>.toStoreResultReducer(
    initialState: suspend (T) -> State,
    nextState: suspend (State, T) -> State = { oldState, newValue ->
        initialState(newValue)
    },
): StoreResultReducer<State> {
    return toStoreResultReducer(initialState, nextState, owner.reducerCoroutineScope, owner.reducerSharingStarted)
}

/**
 * Create a [StoreResultReducer] from the origin flow.
 *
 * A [StoreResult] converts the origin flow into a [StateFlow] holding state
 * wrapped into [StoreResult].
 */
context(owner: ReducerOwner)
public fun <T> Flow<T>.toStoreResultReducer(): StoreResultReducer<T> {
    return toStoreResultReducer(owner.reducerCoroutineScope, owner.reducerSharingStarted)
}

/**
 * Create a [StoreResultReducer] from existing [StoreResult] flow.
 */
context(owner: ReducerOwner)
public fun <T, State> Flow<StoreResult<T>>.storeResultToReducer(
    initialState: suspend (T) -> State,
    nextState: suspend (State, T) -> State = { oldState, newValue ->
        initialState(newValue)
    },
): StoreResultReducer<State> {
    return storeResultToReducer(initialState, nextState, owner.reducerCoroutineScope, owner.reducerSharingStarted)
}

/**
 * Create a [StoreResultReducer] from existing [StoreResult] flow.
 */
context(owner: ReducerOwner)
public fun <T> Flow<StoreResult<T>>.storeResultToReducer(): StoreResultReducer<T> {
    return storeResultToReducer(owner.reducerCoroutineScope, owner.reducerSharingStarted)
}
