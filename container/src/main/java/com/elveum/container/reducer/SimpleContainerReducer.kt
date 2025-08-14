package com.elveum.container.reducer

import com.elveum.container.Container
import com.elveum.container.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted

internal class SimpleContainerReducer<State>(
    scope: CoroutineScope,
    started: SharingStarted,
    inputFlow: Flow<Container<State>>,
) : AbstractContainerReducer<State>(
    scope = scope,
    started = started,
    inputFlows = listOf(inputFlow),
) {

    override suspend fun transform(
        oldStateContainer: Container<State>,
        newContainer: Container<List<*>>
    ): Container<State> {
        @Suppress("UNCHECKED_CAST")
        return newContainer.map { it.first() as State }
    }

}
