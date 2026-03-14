package com.elveum.container.demo.feature.examples.reducer_container

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.container.Container
import com.elveum.container.demo.feature.examples.reducer_container.MathRepository.MathPoint
import com.elveum.container.reducer.ContainerReducer
import com.elveum.container.reducer.toContainerReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class FlowToContainerReducerViewModel @Inject constructor(
    repository: MathRepository,
) : ViewModel() {

    private val reducer: ContainerReducer<State> = repository
        .getMathPoints() // Flow<List<MathPoint>>
        .toContainerReducer(
            initialState = ::State,
            nextState = State::copy,
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1_000),
        )

    val state: StateFlow<Container<State>> = reducer.stateFlow

    fun setColor(color: Color) = reducer.updateState { oldState ->
        oldState.copy(color = color)
    }

    fun setStrokeWidth(width: StrokeWidth) = reducer.updateState { oldState ->
        oldState.copy(strokeWidth = width)
    }

    enum class StrokeWidth(val dp: Float) {
        Thin(1.5f),
        Normal(3f),
        Thick(6f),
    }

    data class State(
        val points: List<MathPoint>,
        val color: Color = Color.Magenta,
        val strokeWidth: StrokeWidth = StrokeWidth.Normal,
    )

}
