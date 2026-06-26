package com.elveum.store.demo.feature.examples.store_keyed.master_details.details

import com.elveum.store.demo.effects.Toaster
import com.elveum.store.demo.errors.ErrorFlagRepository
import com.elveum.store.demo.feature.examples.store_keyed.master_details.model.CatDetails
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.combineToReducer
import com.elveum.store.load.StoreResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = CatDetailsViewModel.Factory::class)
class CatDetailsViewModel @AssistedInject constructor(
    @Assisted private val catId: Long,
    private val catDetailsRepository: CatDetailsRepository,
    private val errorFlagRepository: ErrorFlagRepository,
    private val toaster: Toaster,
) : AbstractViewModel() {

    private val reducer = combineToReducer(
        catDetailsRepository.getCat(catId),
        errorFlagRepository.getErrorFlag(),
        initialState = ::State,
        nextState = State::copy,
    )

    val stateFlow = reducer.stateFlow

    fun updateCatName(name: String) {
        safeLaunch(toaster) {
            val isNonOptimistic = stateFlow.value.isNonOptimisticUpdate

            try {
                if (isNonOptimistic) reducer.update { it.copy(isUpdateInProgress = true) }
                catDetailsRepository.updateCatName(
                    id = catId,
                    name = name,
                    isNonOptimisticUpdate = isNonOptimistic,
                )
            } finally {
                if (isNonOptimistic) reducer.update { it.copy(isUpdateInProgress = false) }
            }
        }
    }

    fun tryAgain() {
        catDetailsRepository.tryAgain(catId)
    }

    fun refresh() {
        catDetailsRepository.refresh(catId)
    }

    fun toggleErrorFlag() {
        errorFlagRepository.toggleErrorFlag()
    }

    fun toggleOptimisticUpdates() {
        reducer.update { it.copy(isNonOptimisticUpdate = !it.isNonOptimisticUpdate) }
    }

    @AssistedFactory
    interface Factory {
        fun create(catId: Long): CatDetailsViewModel
    }

    data class State(
        val catResult: StoreResult<CatDetails> = StoreResult.Loading,
        val isErrorsEnabled: Boolean = false,
        val isNonOptimisticUpdate: Boolean = false,
        val isUpdateInProgress: Boolean = false,
    )

}