package com.elveum.container.reducer

import com.elveum.container.Container
import com.elveum.container.pendingContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted

internal class MapperContainerReducer<State>(
    inputFlows: Iterable<Flow<Container<*>>>,
    scope: CoroutineScope,
    started: SharingStarted,
    private val initialValue: suspend (List<*>) -> State,
    private val nextValue: suspend (State, List<*>) -> State,
) : AbstractContainerReducer<State>(
    scope = scope,
    started = started,
    inputFlows = inputFlows,
) {

    override suspend fun transform(
        oldStateContainer: Container<State>,
        newContainer: Container<List<*>>
    ): Container<State> {
        return newContainer.fold(
            onPending = ::pendingContainer,
            onError = { errorContainer(it) },
            onSuccess = { newInputValue ->
                val newState: State = when (oldStateContainer) {
                    Container.Pending, is Container.Error -> {
                        initialValue(newInputValue)
                    }
                    is Container.Success -> {
                        nextValue(oldStateContainer.value, newInputValue)
                    }
                }
                successContainer(newState)
            }
        )
    }

}
