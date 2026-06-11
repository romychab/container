package com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive

import com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive.sources.LocalArticlesReactiveSource
import com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive.sources.RemoteArticlesSource
import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ArticleRepository @Inject constructor(
    private val localArticlesSource: LocalArticlesReactiveSource,
    private val remoteArticlesSource: RemoteArticlesSource,
) {

    private val store = StoreFactory.simpleStoreBuilder<List<Article>>()
        .addReactiveLocalStorage()
        .build(
            onFetch = remoteArticlesSource::fetchArticles,
            onSaveToStorage = localArticlesSource::saveLocalArticles,
            onObserveStorage = localArticlesSource::observeLocalArticles,
        )

    fun getArticles(): Flow<StoreResult<List<Article>>> {
        return store.observe()
    }

    suspend fun delete(article: Article) {
        remoteArticlesSource.delete(article)

        // local source is reactive -> it automatically updates the store
        localArticlesSource.delete(article)
    }

    data class Article(
        val id: Int,
        val title: String,
    )

}
