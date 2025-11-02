package com.elveum.container.reducer

import kotlinx.coroutines.flow.StateFlow

/**
 * A reducer that manages a reactive state. State values can be observed
 * by using [stateFlow] property. Also, the current state can be updated
 * via [update] method.
 */
public interface Reducer<State> {

    /**
     * Flow containing the most actual state.
     */
    public val stateFlow: StateFlow<State>

    /**
     * Update the state value held by [stateFlow].
     */
    public fun update(transform: suspend (State) -> State)

}
