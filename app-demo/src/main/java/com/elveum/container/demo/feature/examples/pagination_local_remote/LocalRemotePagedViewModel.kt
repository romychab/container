package com.elveum.container.demo.feature.examples.pagination_local_remote

import com.elveum.container.Container
import com.elveum.container.demo.errors.ErrorFlagRepository
import com.elveum.container.demo.feature.examples.pagination_local_remote.PagedArticleRepository.Article
import com.elveum.container.demo.feature.examples.reducer_owner.AbstractViewModel
import com.elveum.container.reducer.containerToReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LocalRemotePagedViewModel @Inject constructor(
    private val errorFlagRepository: ErrorFlagRepository,
    repository: PagedArticleRepository,
) : AbstractViewModel() {

    private val reducer = repository
        .getArticles()
        .containerToReducer()

    val stateFlow: StateFlow<Container<List<Article>>> = reducer.stateFlow

    val isErrorFlagEnabledFlow = errorFlagRepository.getErrorFlag()

    fun toggleErrorFlag() {
        errorFlagRepository.toggle()
    }

}
