package com.elveum.container.reducer

import com.elveum.container.Container
import kotlinx.coroutines.flow.StateFlow

/**
 * A reducer that manages a reactive state and wraps it into [Container].
 *
 * @see Reducer
 * @see Container
 */
public interface ContainerReducer<State> : Reducer<Container<State>>{

    /**
     * Flow containing the most actual state wrapped into [Container].
     */
    public override val stateFlow: StateFlow<Container<State>>

    /**
     * Update the whole container held by [stateFlow].
     */
    public override fun update(transform: suspend (Container<State>) -> Container<State>)

    /**
     * Update the state value held by [stateFlow]. The state is updated only
     * if the current container is [Container.Success].
     */
    public fun updateState(transform: suspend (State) -> State)

}
