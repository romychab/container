package com.elveum.container.demo.feature.examples.subject_local_remote.sources

import com.elveum.container.demo.feature.examples.subject_local_remote.ArticleRepository.Article
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject

class RemoteArticlesSource @Inject constructor(
    private val faker: Faker,
) {

    suspend fun fetchArticles(): List<Article> {
        delay(3000)
        return List(40) {
            Article(
                id = it + 1,
                title = faker.book().title(),
            )
        }
    }

}
