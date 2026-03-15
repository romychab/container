package com.elveum.container.demo.feature.examples.subject_basics

import com.elveum.container.Container
import com.elveum.container.demo.feature.examples.reducer_owner.AbstractViewModel
import com.elveum.container.demo.feature.examples.subject_basics.UserProfileRepository.UserProfile
import com.elveum.container.pendingContainer
import com.elveum.container.reducer.Reducer
import com.elveum.container.reducer.combineToReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SubjectBasicsViewModel @Inject constructor(
    private val repository: UserProfileRepository
) : AbstractViewModel() {

    private val reducer: Reducer<State> = combineToReducer(
        repository.getUserProfile(),
        repository.isErrorsEnabled(),
        initialState = State(),
        nextState = State::copy,
    )
    val stateFlow: StateFlow<State> = reducer.stateFlow

    fun toggleLoadErrors() {
        repository.toggleLoadErrors()
    }

    data class State(
        val userProfile: Container<UserProfile> = pendingContainer(),
        val isErrorsEnabled: Boolean = false,
    )
}
