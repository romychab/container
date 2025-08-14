package com.elveum.container.reducer

import com.elveum.container.Container
import com.elveum.container.ContainerMapperScope
import com.elveum.container.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            stateFlow.update { transform(it) }
        }
    }

    override fun updateState(transform: suspend ContainerMapperScope.(State) -> State) {
        updateContainer { container ->
            container.map { transform(it) }
        }
    }

}
