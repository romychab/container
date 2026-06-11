package com.elveum.store.demo.feature.examples.store_simple.pull_to_refresh

import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.stateIn
import com.elveum.store.load.StoreResult
import com.elveum.store.load.storeMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PullToRefreshSimpleStoreViewModel @Inject constructor(
    private val repository: CatsRepository,
) : AbstractViewModel() {

    val stateFlow: StateFlow<StoreResult<State>> = repository
        .getCats()
        .storeMap(::State)
        .stateIn(StoreResult.Loading)

    fun refresh() {
        repository.refresh()
    }

    data class State(
        val cats: List<CatsRepository.Cat>,
    )

}
