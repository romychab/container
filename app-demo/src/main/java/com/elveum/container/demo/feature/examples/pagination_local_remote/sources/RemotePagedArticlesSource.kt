package com.elveum.container.demo.feature.examples.pagination_local_remote.sources

import com.elveum.container.demo.errors.ErrorFlagProvider
import com.elveum.container.demo.feature.examples.pagination_local_remote.PagedArticleRepository.Article
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.math.min

class RemotePagedArticlesSource @Inject constructor(
    private val faker: Faker,
    private val errorFlagProvider: ErrorFlagProvider,
) {

    private val list = List(100) {
        Article(
            id = it + 1,
            title = faker.book().title(),
            isLocal = false,
        )
    }

    suspend fun fetchArticles(pageIndex: Int, pageSize: Int): List<Article> {
        delay(3000)
        if (errorFlagProvider.isErrorFlagEnabled()) {
            throw RuntimeException("Failed to fetch items from the remote data source.")
        }
        val start = pageIndex * pageSize
        val end = start + pageSize
        val finalEnd = min(end, list.size)
        return list.subList(start, finalEnd).toList()
    }

}
