package com.elveum.container.demo.feature.examples.reducer_owner

import com.elveum.container.reducer.Reducer
import com.elveum.container.reducer.toReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ReducerOwnerViewModel @Inject constructor(
    repository: ParticlesRepository,
) : AbstractViewModel() {

    private val reducer: Reducer<State> = repository.getParticles()
        .toReducer(
            initialState = State(),
            nextState = State::copy,
        )

    val stateFlow: StateFlow<State> = reducer.stateFlow

    fun setParticleSize(size: Float) {
        reducer.update { it.copy(particleSize = size) }
    }

    data class State(
        // value from the repository:
        val particles: List<ParticlesRepository.Particle> = emptyList(),
        // manually updated value:
        val particleSize: Float = 0.5f,
    )
}
