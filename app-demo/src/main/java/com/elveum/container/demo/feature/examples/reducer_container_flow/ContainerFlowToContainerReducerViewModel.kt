package com.elveum.container.demo.feature.examples.reducer_container_flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.container.Container
import com.elveum.container.demo.feature.examples.reducer_container_flow.LogsRepository.LogLevel
import com.elveum.container.demo.feature.examples.reducer_container_flow.LogsRepository.LogMessage
import com.elveum.container.reducer.ContainerReducer
import com.elveum.container.reducer.containerToReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ContainerFlowToContainerReducerViewModel @Inject constructor(
    repository: LogsRepository,
) : ViewModel() {

    private val reducer: ContainerReducer<State> = repository
        .getRecentLogs() // Flow<Container<List<LogMessage>>>
        .containerToReducer(
            initialState = ::State,
            nextState = State::copy,
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1_000),
        )

    val stateFlow: StateFlow<Container<State>> = reducer.stateFlow

    fun toggleLogLevel(logLevel: LogLevel) = reducer.updateState { oldState ->
        val newEnabledLevels = if (oldState.enabledLevels.contains(logLevel)) {
            oldState.enabledLevels - logLevel
        } else {
            oldState.enabledLevels + logLevel
        }
        oldState.copy(enabledLevels = newEnabledLevels)
    }

    data class State(
        val logs: List<LogMessage> = emptyList(),
        val enabledLevels: Set<LogLevel> = LogLevel.entries.toSet(),
    ) {
        val filteredLogs = logs
            .filter { enabledLevels.contains(it.level) }
            .asReversed()
            .take(20)
    }

}
