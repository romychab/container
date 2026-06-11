package com.elveum.store.reducers

import com.elveum.container.reducer.Reducer
import com.elveum.store.load.StoreResult

public interface StoreResultReducer<State> : Reducer<StoreResult<State>> {
    /**
     * Update the state value held by [stateFlow]. The state is updated only
     * if the current state is [StoreResult.Loaded].
     */
    public fun updateState(transform: suspend (State) -> State)
}
