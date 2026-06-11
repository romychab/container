package com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.elveum.container.Container
import com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive.ArticleRepository.Article
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.ContainerReducer
import com.elveum.container.reducer.containerToReducer
import com.elveum.store.load.StoreResult
import com.elveum.store.reducers.StoreResultReducer
import com.elveum.store.reducers.storeResultToReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalRemoteReactiveSimpleStoreViewModel @Inject constructor(
    private val repository: ArticleRepository,
) : AbstractViewModel() {

    private val reducer: StoreResultReducer<StateImpl> = repository
        .getArticles()
        .storeResultToReducer(
            initialState = ::StateImpl,
            nextState = StateImpl::copy,
        )

    val stateFlow: StateFlow<StoreResult<State>> = reducer.stateFlow

    fun delete(article: UiArticle) {
        viewModelScope.launch {
            try {
                reducer.updateState { it.copy(deleteInProgressIds = it.deleteInProgressIds + article.id) }
                repository.delete(article.origin)
            } finally {
                reducer.updateState { it.copy(deleteInProgressIds = it.deleteInProgressIds - article.id) }
            }
        }
    }

    @Immutable
    interface State {
        val articles: List<UiArticle>
    }

    private data class StateImpl(
        val originArticles: List<Article>,
        val deleteInProgressIds: Set<Int> = emptySet(),
    ) : State {
        override val articles: List<UiArticle> = originArticles.map {
            UiArticle(it, deleteInProgressIds.contains(it.id))
        }
    }

    data class UiArticle(
        val origin: Article,
        val isDeleting: Boolean,
    ) {
        val id: Int = origin.id
        val title: String = origin.title
    }
}
