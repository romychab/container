package com.elveum.store.demo.feature.examples.store_simple.basic

import com.elveum.store.demo.effects.Toaster
import com.elveum.store.demo.errors.ErrorFlagRepository
import com.elveum.store.demo.feature.examples.store_simple.basic.UserProfileRepository.UserProfile
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.stateIn
import com.elveum.store.load.StoreResult
import com.elveum.store.load.getOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class SimpleStoreBasicsViewModel @Inject constructor(
    private val errorFlagRepository: ErrorFlagRepository,
    private val userProfileRepository: UserProfileRepository,
    private val toaster: Toaster,
) : AbstractViewModel() {

    val stateFlow: StateFlow<State> = combine(
        userProfileRepository.getUserProfile(),
        errorFlagRepository.getErrorFlag(),
        ::State,
    ).stateIn(State())

    private val currentUserProfile get() = stateFlow.value.userProfile.getOrNull()

    fun toggleLoadErrors() {
        errorFlagRepository.toggleErrorFlag()
    }

    fun reload() {
        userProfileRepository.reload()
    }

    fun updateAge(age: Int) {
        currentUserProfile?.copy(age = age)?.let(::updateProfile)
    }

    fun updateAddress(address: String) {
        currentUserProfile?.copy(address = address)?.let(::updateProfile)
    }

    fun updateBio(bio: String) {
        currentUserProfile?.copy(shortBio = bio)?.let(::updateProfile)
    }

    private fun updateProfile(newProfile: UserProfile) = safeLaunch(toaster) {
        userProfileRepository.update(newProfile)
    }

    data class State(
        val userProfile: StoreResult<UserProfile> = StoreResult.Loading,
        val isErrorsEnabled: Boolean = false,
    )
}
