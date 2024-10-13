package com.elveum.container.subject.state

import com.elveum.container.Emitter
import com.elveum.container.subject.ValueLoader

/**
 * Extended emitter accessible from loaders created by [loaderWithState]
 * function.
 */
public interface EmitterWithState<T, C> : Emitter<T> {

    /**
     * Get the current state value. It can be modifier by [updateState]
     * and also it is retained across loader executions.
     */
    public val state: C

    /**
     * Update the state value held by [state] property and also store it for
     * all further loader executions.
     */
    public fun updateState(state: C)
}

/**
 * Create a value loader which has an additional state retained
 * between executions.
 */
public fun <T, State> loaderWithState(
    initialState: State,
    loader: suspend EmitterWithState<T, State>.() -> Unit,
): ValueLoader<T> {
    val stateHolder = StateHolder(initialState)
    return {
        EmitterWithStateImpl(originEmitter = this, stateHolder).loader()
    }
}

private class StateHolder<C>(
    var state: C
)

private class EmitterWithStateImpl<T, C>(
    private val originEmitter: Emitter<T>,
    private val stateHolder: StateHolder<C>,
) : Emitter<T> by originEmitter, EmitterWithState<T, C> {

    override val state: C get() = stateHolder.state

    override fun updateState(state: C) {
        stateHolder.state = state
    }

}