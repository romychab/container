package com.elveum.container.reducer

import com.elveum.container.Container
import com.elveum.container.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted

internal abstract class AbstractContainerReducer<State>(
    private val scope: CoroutineScope,
    started: SharingStarted,
    inputFlows: Iterable<Flow<Container<*>>>,
) : ContainerReducer<State> {

    override val stateFlow: ReducerStateFlow<State> = ReducerStateFlow(
        scope = scope,
        originFlows = inputFlows,
        started = started,
        transform = ::transform,
    )

    abstract suspend fun transform(
        oldStateContainer: Container<State>,
        newContainer: Container<List<*>>,
    ): Container<State>

    override fun updateContainer(transform: suspend (Container<State>) -> Container<State>) {
        stateFlow.update { transform(it) }
    }

    override fun updateValue(transform: suspend State.() -> State) {
        stateFlow.update { container ->
            container.map { transform(it) }
        }
    }

}
