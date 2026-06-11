package com.elveum.store.internal.reducers

import com.elveum.container.reducer.Reducer
import com.elveum.store.load.StoreResult
import com.elveum.store.load.map
import com.elveum.store.reducers.StoreResultReducer

internal class StoreResultReducerImpl<State>(
    private val reducer: Reducer<StoreResult<State>>,
) : StoreResultReducer<State>, Reducer<StoreResult<State>> by reducer {

    override fun updateState(transform: suspend (State) -> State) {
        reducer.update { oldResult ->
            oldResult.map { transform(it) }
        }
    }

}
