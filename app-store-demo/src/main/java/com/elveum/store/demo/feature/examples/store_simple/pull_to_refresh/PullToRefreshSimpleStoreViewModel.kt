package com.elveum.store.demo.feature.examples.store_simple.pull_to_refresh

import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.stateIn
import com.elveum.store.demo.errors.ErrorFlagRepository
import com.elveum.store.load.StoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class PullToRefreshSimpleStoreViewModel @Inject constructor(
    repository: CatsRepository,
    private val errorFlagRepository: ErrorFlagRepository,
) : AbstractViewModel() {

    val stateFlow = combine(
        repository.getCats(),
        errorFlagRepository.getErrorFlag(),
        ::State,
    ).stateIn(State())

    fun toggleErrors() {
        errorFlagRepository.toggleErrorFlag()
    }

    data class State(
        val catsResult: StoreResult<List<CatsRepository.Cat>> = StoreResult.Loading,
        val isErrorsEnabled: Boolean = false,
    )

}
