package com.elveum.store.demo.feature.examples.store_simple.local_remote_suspending.sources

import com.elveum.store.demo.feature.examples.store_simple.local_remote_suspending.ArticleRepository
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject

class LocalArticlesSuspendingSource @Inject constructor(
    private val faker: Faker,
) {

    private var savedArticles = List(40) {
        ArticleRepository.Article(
            id = it + 1,
            title = faker.book().title(),
        )
    }

    suspend fun loadLocalArticles(): List<ArticleRepository.Article> {
        delay(500)
        return savedArticles.toList()
    }

    suspend fun saveLocalArticles(articles: List<ArticleRepository.Article>) {
        delay(300)
        savedArticles = articles.toList()
    }

}
