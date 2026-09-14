package com.elveum.container.reducer.impl

import com.elveum.container.Container
import com.elveum.container.map
import com.elveum.container.reducer.ContainerReducer
import com.elveum.container.reducer.Reducer

internal class ContainerReducerImpl<State>(
    private val reducer: Reducer<Container<State>>,
) : ContainerReducer<State>, Reducer<Container<State>> by reducer {

    override fun updateState(transform: suspend State.() -> State) {
        reducer.update { oldContainer ->
            oldContainer.map { transform(it) }
        }
    }

}
