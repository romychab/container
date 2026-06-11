package com.elveum.store.demo.feature.examples.store_simple.local_remote_suspending

import com.elveum.store.demo.feature.examples.store_simple.local_remote_suspending.sources.LocalArticlesSuspendingSource
import com.elveum.store.demo.feature.examples.store_simple.local_remote_suspending.sources.RemoteArticlesSource
import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ArticleRepository @Inject constructor(
    private val localArticlesSource: LocalArticlesSuspendingSource,
    private val remoteArticlesSource: RemoteArticlesSource,
) {

    private val store = StoreFactory.simpleStoreBuilder<List<Article>>()
        .addSuspendingLocalStorage()
        .build(
            onFetch = remoteArticlesSource::fetchArticles,
            onLoadFromStorage = localArticlesSource::loadLocalArticles,
            onSaveToStorage = localArticlesSource::saveLocalArticles,
        )

    fun getArticles(): Flow<StoreResult<List<Article>>> {
        return store.observe()
    }

    data class Article(
        val id: Int,
        val title: String,
    )

}
