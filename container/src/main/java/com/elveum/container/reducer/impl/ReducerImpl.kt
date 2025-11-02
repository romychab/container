package com.elveum.container.reducer.impl

import com.elveum.container.reducer.Reducer
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

internal class ReducerImpl<T, State>(
    private val initialState: State,
    private val originFlow: Flow<T>,
    private val combiner: suspend (State, T) -> State,
    private val scope: CoroutineScope,
    private val started: SharingStarted,
) : Reducer<State> {

    override val stateFlow = MutableStateFlow(initialState)

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

    override fun update(transform: suspend State.() -> State) {
        scope.launch {
            stateFlow.update { oldState ->
                oldState.transform()
            }
        }
    }

    private suspend fun setupStateFlow() {
        if (started === SharingStarted.Eagerly) {
            startCollecting()
        } else if (started === SharingStarted.Lazily) {
            stateFlow.subscriptionCount.first { it > 0 }
            startCollecting()
        } else {
            started
                .command(stateFlow.subscriptionCount)
                .distinctUntilChanged()
                .collectLatest { command ->
                    when (command) {
                        SharingCommand.START -> startCollecting()
                        SharingCommand.STOP -> { /* do nothing, auto-cancelled */ }
                        SharingCommand.STOP_AND_RESET_REPLAY_CACHE -> {
                            stateFlow.value = initialState
                        }
                    }
                }
        }
    }

    private suspend fun startCollecting() {
        originFlow.collect { newOriginValue ->
            stateFlow.update { oldState ->
                combiner(oldState, newOriginValue)
            }
        }
    }

}
