package com.elveum.store.demo.feature.examples.store_keyed.basic

import com.elveum.store.demo.feature.examples.store_keyed.basic.model.ListItem
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.stateIn
import com.elveum.store.load.StoreResult
import com.elveum.store.load.storeMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BasicItemsViewModel @Inject constructor(
    private val repository: BasicItemsRepository,
) : AbstractViewModel() {

    val stateFlow: StateFlow<StoreResult<State>> = repository
        .getItems()
        .storeMap(::State)
        .stateIn(StoreResult.Loading)

    fun refresh() {
        repository.refresh()
    }

    fun tryAgain() {
        repository.tryAgain()
    }

    data class State(
        val items: List<ListItem>,
    )
}
