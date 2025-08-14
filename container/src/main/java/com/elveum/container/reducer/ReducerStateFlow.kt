package com.elveum.container.reducer

import com.elveum.container.Container
import com.elveum.container.combineContainerFlows
import com.elveum.container.pendingContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@PublishedApi
internal class ReducerStateFlow<R>(
    private val originFlows: Iterable<Flow<Container<*>>>,
    scope: CoroutineScope,
    private val started: SharingStarted,
    private val transform: suspend (Container<R>, Container<List<*>>) -> Container<R>,
    @PublishedApi internal val outputFlow: MutableStateFlow<Container<R>> = MutableStateFlow(pendingContainer()),
) : MutableStateFlow<Container<R>> by outputFlow {

    init {
        val start = if (started == SharingStarted.Eagerly) {
            CoroutineStart.DEFAULT
        } else {
            CoroutineStart.UNDISPATCHED
        }
        scope.launch(start = start) {
           setupStateFlow()
        }
    }

    private suspend fun setupStateFlow() {
        if (started === SharingStarted.Eagerly) {
            startCollecting()
        } else if (started === SharingStarted.Lazily) {
            outputFlow.subscriptionCount.first { it > 0 }
            startCollecting()
        } else {
            started
                .command(outputFlow.subscriptionCount)
                .distinctUntilChanged()
                .collectLatest { command ->
                    when (command) {
                        SharingCommand.START -> startCollecting()
                        SharingCommand.STOP -> { /* do nothing, auto-cancelled */ }
                        SharingCommand.STOP_AND_RESET_REPLAY_CACHE -> {
                            outputFlow.value = pendingContainer()
                        }
                    }
                }
        }
    }

    private suspend fun startCollecting() {
        val combinedFlow = combineContainerFlows(
            flows = originFlows,
            transform = { values ->
                values
            }
        )
        combinedFlow.collect { newOriginContainers ->
            outputFlow.update { oldResultContainer ->
                transform(oldResultContainer, newOriginContainers)
            }
        }
    }

}
