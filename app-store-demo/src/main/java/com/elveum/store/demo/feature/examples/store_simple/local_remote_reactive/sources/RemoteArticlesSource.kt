package com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive.sources

import com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive.ArticleRepository
import com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive.ArticleRepository.Article
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject

class RemoteArticlesSource @Inject constructor(
    private val faker: Faker,
) {

    private val articles = MutableList(40) {
        Article(
            id = it + 1,
            title = faker.book().title(),
        )
    }

    suspend fun fetchArticles(): List<Article> {
        delay(3000)
        return articles.toList()
    }

    suspend fun delete(article: Article) {
        delay(2000)
        articles.indexOfFirst { it.id == article.id }.takeIf { it != -1 }
            ?.let { articles.removeAt(it) }
    }
}
