package com.elveum.container.demo.feature.examples.pagination_local_remote.sources

import com.elveum.container.demo.errors.ErrorFlagProvider
import com.elveum.container.demo.feature.examples.pagination_local_remote.PagedArticleRepository.Article
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.math.min

class LocalPagedArticlesSource @Inject constructor(
    private val faker: Faker,
    private val errorFlagProvider: ErrorFlagProvider,
) {

    private val list = MutableList(100) {
        Article(
            id = it + 1,
            title = faker.book().title(),
            isLocal = true,
        )
    }

    suspend fun fetchLocalArticles(pageIndex: Int, pageSize: Int): List<Article> {
        delay(500)
        if (errorFlagProvider.isErrorFlagEnabled()) {
            throw RuntimeException("Failed to fetch items from the local data source.")
        }
        val start = pageIndex * pageSize
        val end = start + pageSize
        if (start >= list.size) return emptyList()
        val finalEnd = min(end, list.size)
        return list.subList(start, finalEnd).toList()
    }

    suspend fun saveLocalArticles(pageIndex: Int, pageSize: Int, articles: List<Article>) {
        delay(300)
        val start = pageIndex * pageSize
        articles.forEachIndexed { index, article ->
            val localArticle = article.copy(isLocal = true)
            val replaceIndex = start + index
            if (replaceIndex < list.size) {
                list[replaceIndex] = localArticle
            } else {
                list.add(article)
            }
        }
    }

}
