package com.elveum.container.demo.feature.examples.subject_local_remote.sources

import com.elveum.container.demo.feature.examples.subject_local_remote.ArticleRepository.Article
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject

class LocalArticlesSource @Inject constructor(
    private val faker: Faker,
) {

    private var savedArticles = List(40) {
        Article(
            id = it + 1,
            title = faker.book().title(),
        )
    }

    suspend fun fetchLocalArticles(): List<Article> {
        delay(500)
        return savedArticles
    }

    suspend fun saveLocalArticles(articles: List<Article>) {
        delay(300)
        savedArticles = articles
    }

}
