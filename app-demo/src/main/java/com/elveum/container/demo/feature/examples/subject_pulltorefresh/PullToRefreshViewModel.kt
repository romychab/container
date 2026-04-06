package com.elveum.container.demo.feature.examples.subject_pulltorefresh

import com.elveum.container.Container
import com.elveum.container.demo.feature.examples.reducer_owner.AbstractViewModel
import com.elveum.container.demo.feature.examples.subject_pulltorefresh.CatsRepository.Cat
import com.elveum.container.reducer.ContainerReducer
import com.elveum.container.reducer.containerToReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PullToRefreshViewModel @Inject constructor(
    repository: CatsRepository,
) : AbstractViewModel() {

    private val reducer: ContainerReducer<State> = repository
        .getCats()
        .containerToReducer(
            initialState = ::State,
            nextState = State::copy,
        )

    val stateFlow: StateFlow<Container<State>> = reducer.stateFlow

    data class State(
        val cats: List<Cat>,
    )

}
