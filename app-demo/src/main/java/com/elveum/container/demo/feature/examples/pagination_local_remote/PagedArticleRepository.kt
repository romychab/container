package com.elveum.container.demo.feature.examples.pagination_local_remote

import com.elveum.container.Container
import com.elveum.container.demo.feature.examples.pagination_local_remote.sources.LocalPagedArticlesSource
import com.elveum.container.demo.feature.examples.pagination_local_remote.sources.RemotePagedArticlesSource
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.listenReloadable
import com.elveum.container.subject.paging.pageLoader
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PagedArticleRepository @Inject constructor(
    private val localArticlesSource: LocalPagedArticlesSource,
    private val remoteArticlesSource: RemotePagedArticlesSource,
) {

    private val subject = LazyFlowSubject.create(
        valueLoader = pageLoader<Int, Article>(
            initialKey = 0,
            itemId = Article::id,
            block = { pageIndex ->
                val localArticles = localArticlesSource.fetchLocalArticles(pageIndex, PAGE_SIZE)
                if (localArticles.isNotEmpty()) {
                    emitPage(localArticles)
                    if (localArticles.size == PAGE_SIZE) {
                        emitNextKey(pageIndex + 1)
                    }
                }

                val freshArticles = remoteArticlesSource.fetchArticles(pageIndex, PAGE_SIZE)
                emitPage(freshArticles)
                if (freshArticles.size == PAGE_SIZE) {
                    // it is ok to call the same next key more than once: if the local and
                    // the remote keys are the same - they will be deduplicated.
                    emitNextKey(pageIndex + 1)
                }
                localArticlesSource.saveLocalArticles(pageIndex, PAGE_SIZE, freshArticles)
            }
        )
    )

    fun getArticles(): Flow<Container<List<Article>>> {
        return subject.listenReloadable()
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
