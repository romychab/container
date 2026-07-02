package com.elveum.store.demo.feature.examples.store_paged.pagination_local_remote

import com.elveum.store.demo.errors.ErrorFlagRepository
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.stateIn
import com.elveum.store.load.StoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LocalRemotePagedViewModel @Inject constructor(
    private val errorFlagRepository: ErrorFlagRepository,
    private val repository: PagedArticleRepository,
) : AbstractViewModel() {

    val stateFlow = repository
        .getArticles()
        .stateIn(StoreResult.Loading)

    val isErrorFlagEnabledFlow = errorFlagRepository.getErrorFlag()

    fun toggleErrorFlag() {
        errorFlagRepository.toggleErrorFlag()
    }

    fun onItemRendered(index: Int) {
        repository.onItemRendered(index)
    }

}
