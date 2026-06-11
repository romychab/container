package com.elveum.store.demo.feature.examples.store_paged.pagination_local_remote.sources

import com.elveum.store.demo.errors.ErrorFlagProvider
import com.elveum.store.demo.feature.examples.store_paged.pagination_local_remote.PagedArticleRepository.Article
import com.elveum.store.demo.feature.examples.store_paged.pagination_local_remote.PagedArticleRepository.Companion.PAGE_SIZE
import com.elveum.store.stores.paged.PagedList
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

    suspend fun fetchLocalArticles(pageIndex: Int): PagedList<Int, Article> {
        delay(500)
        if (errorFlagProvider.isErrorFlagEnabled()) {
            throw RuntimeException("Failed to fetch items from the local data source.")
        }
        val start = pageIndex * PAGE_SIZE
        val end = start + PAGE_SIZE
        val finalEnd = min(end, list.size)
        val chunk = list.subList(start, finalEnd).toList()
        val nextKey = if (chunk.size == PAGE_SIZE) pageIndex + 1 else null
        return PagedList(chunk, nextKey)
    }

    suspend fun saveLocalArticles(pageIndex: Int, articles: PagedList<Int, Article>) {
        delay(300)
        val start = pageIndex * PAGE_SIZE
        articles.items.forEachIndexed { index, article ->
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
