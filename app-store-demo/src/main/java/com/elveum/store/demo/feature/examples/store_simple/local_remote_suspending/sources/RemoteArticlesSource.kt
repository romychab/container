package com.elveum.store.demo.feature.examples.store_simple.local_remote_suspending.sources

import com.elveum.store.demo.feature.examples.store_simple.local_remote_suspending.ArticleRepository
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject

class RemoteArticlesSource @Inject constructor(
    private val faker: Faker,
) {

    suspend fun fetchArticles(): List<ArticleRepository.Article> {
        delay(3000)
        return List(40) {
            ArticleRepository.Article(
                id = it + 1,
                title = faker.book().title(),
            )
        }
    }

}
