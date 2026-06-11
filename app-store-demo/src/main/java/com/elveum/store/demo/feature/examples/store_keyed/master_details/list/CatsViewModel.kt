package com.elveum.store.demo.feature.examples.store_keyed.master_details.list

import com.elveum.store.demo.errors.ErrorFlagRepository
import com.elveum.store.demo.feature.examples.store_keyed.master_details.model.Cat
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.stateIn
import com.elveum.store.load.StoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class CatListViewModel @Inject constructor(
    private val catsRepository: CatsRepository,
    private val errorFlagRepository: ErrorFlagRepository,
) : AbstractViewModel() {

    val stateFlow = combine(
        catsRepository.getCats(),
        errorFlagRepository.getErrorFlag(),
        ::State,
    ).stateIn(State())

    fun toggleErrorFlag() {
        errorFlagRepository.toggleErrorFlag()
    }

    fun tryAgain() {
        catsRepository.tryAgain()
    }

    fun refresh() {
        catsRepository.refresh()
    }

    data class State(
        val catsResult: StoreResult<List<Cat>> = StoreResult.Loading,
        val isErrorsEnabled: Boolean = false,
    )

}
