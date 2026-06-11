package com.elveum.store.demo.feature.examples.store_paged.pagination_local_remote

import com.elveum.store.demo.feature.examples.store_paged.pagination_local_remote.sources.LocalPagedArticlesSource
import com.elveum.store.demo.feature.examples.store_paged.pagination_local_remote.sources.RemotePagedArticlesSource
import com.elveum.store.StoreFactory
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PagedArticleRepository @Inject constructor(
    private val localArticlesSource: LocalPagedArticlesSource,
    private val remoteArticlesSource: RemotePagedArticlesSource,
) {

    private val store = StoreFactory.pagedStoreBuilder(initialKey = 0, Article::id)
        .addSuspendingLocalStorage()
        .build(
            onFetch = remoteArticlesSource::fetchArticles,
            onSaveToStorage = { pageKey, list -> localArticlesSource.saveLocalArticles(pageKey, list) },
            onLoadFromStorage = { pageKey -> localArticlesSource.fetchLocalArticles(pageKey) },
        )

    fun getArticles(): Flow<StoreResult<List<Article>>> {
        return store.observe()
    }

    fun refresh() {
        store.invalidateAsync(LoadRequest.Silent)
    }

    fun tryAgain() {
        store.invalidateAsync(LoadRequest.Default)
    }

    fun onItemRendered(index: Int) {
        store.onItemRendered(index)
    }

    data class Article(
        val id: Int,
        val title: String,
        val isLocal: Boolean,
    )

    companion object {
        const val PAGE_SIZE = 20
    }
}
