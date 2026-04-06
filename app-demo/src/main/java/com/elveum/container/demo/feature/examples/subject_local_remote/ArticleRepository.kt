package com.elveum.container.demo.feature.examples.subject_local_remote

import com.elveum.container.Container
import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.container.demo.feature.examples.subject_local_remote.sources.LocalArticlesSource
import com.elveum.container.demo.feature.examples.subject_local_remote.sources.RemoteArticlesSource
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.listenReloadable
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ArticleRepository @Inject constructor(
    private val localArticlesSource: LocalArticlesSource,
    private val remoteArticlesSource: RemoteArticlesSource,
) {

    private val subject = LazyFlowSubject.create {
        val localArticles = localArticlesSource.fetchLocalArticles()
        if (localArticles.isNotEmpty()) {
            emit(localArticles, LocalSourceType)
        }
        val remoteArticles = remoteArticlesSource.fetchArticles()
        emit(remoteArticles, RemoteSourceType)
        localArticlesSource.saveLocalArticles(remoteArticles)
    }

    fun getArticles(): Flow<Container<List<Article>>> {
        return subject.listenReloadable()
    }

    data class Article(
        val id: Int,
        val title: String,
    )

}
