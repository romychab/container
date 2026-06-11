package com.elveum.store.demo.feature.examples.store_simple.local_remote_suspending

import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.stateIn
import com.elveum.store.load.StoreResult
import com.elveum.store.load.storeMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LocalRemoteSuspendingSimpleStoreViewModel @Inject constructor(
    repository: ArticleRepository,
) : AbstractViewModel() {

    val stateFlow: StateFlow<StoreResult<State>> = repository
        .getArticles()
        .storeMap(::State)
        .stateIn(StoreResult.Loading)

    data class State(
        val articles: List<ArticleRepository.Article>,
    )

}
