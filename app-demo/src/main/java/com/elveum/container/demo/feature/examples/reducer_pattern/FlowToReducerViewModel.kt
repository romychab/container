package com.elveum.container.demo.feature.examples.reducer_pattern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.container.demo.feature.examples.reducer_pattern.ColorFieldRepository.ColorField
import com.elveum.container.reducer.Reducer
import com.elveum.container.reducer.toReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class FlowToReducerViewModel @Inject constructor(
    repository: ColorFieldRepository,
) : ViewModel() {

    private val reducer: Reducer<State> = repository.getColorField()
        .toReducer(
            initialState = State(),
            nextState = State::copy,
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 1000,
                replayExpirationMillis = 1000,
            )
        )

    val stateFlow: StateFlow<State> = reducer.stateFlow

    fun setAlpha(alpha: Float) {
        reducer.update { oldState ->
            oldState.copy(alpha = alpha)
        }
    }

    data class State(
        // value from the repository:
        val field: ColorField = ColorField(),
        // manually updated value:
        val alpha: Float = 1f,
    )

}
