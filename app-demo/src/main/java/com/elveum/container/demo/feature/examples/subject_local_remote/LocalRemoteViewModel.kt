package com.elveum.container.demo.feature.examples.subject_local_remote

import com.elveum.container.Container
import com.elveum.container.LocalSourceType
import com.elveum.container.containerMap
import com.elveum.container.demo.feature.examples.reducer_owner.AbstractViewModel
import com.elveum.container.demo.feature.examples.subject_local_remote.ArticleRepository.Article
import com.elveum.container.reducer.ContainerReducer
import com.elveum.container.reducer.containerToReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LocalRemoteViewModel @Inject constructor(
    repository: ArticleRepository,
) : AbstractViewModel() {

    private val reducer: ContainerReducer<State> = repository
        .getArticles()
        .containerMap { articles ->
            State(
                articles = articles,
                isLocal = sourceType == LocalSourceType,
            )
        }
        .containerToReducer()

    val stateFlow: StateFlow<Container<State>> = reducer.stateFlow

    data class State(
        val articles: List<Article>,
        val isLocal: Boolean,
    )

}
