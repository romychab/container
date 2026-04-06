package com.elveum.container.demo.feature.examples.subject_basics

import com.elveum.container.Container
import com.elveum.container.demo.errors.ErrorFlagRepository
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
    private val errorFlagRepository: ErrorFlagRepository,
    userProfileRepository: UserProfileRepository,
) : AbstractViewModel() {

    private val reducer: Reducer<State> = combineToReducer(
        userProfileRepository.getUserProfile(),
        errorFlagRepository.getErrorFlag(),
        initialState = State(),
        nextState = State::copy,
    )
    val stateFlow: StateFlow<State> = reducer.stateFlow

    fun toggleLoadErrors() {
        errorFlagRepository.toggle()
    }

    data class State(
        val userProfile: Container<UserProfile> = pendingContainer(),
        val isErrorsEnabled: Boolean = false,
    )
}
