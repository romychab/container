package com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive.sources

import com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive.ArticleRepository.Article
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class LocalArticlesReactiveSource @Inject constructor(
    private val faker: Faker,
) {

    private val savedArticles = MutableStateFlow(
        List(40) {
            Article(
                id = it + 1,
                title = faker.book().title(),
            )
        }
    )

    fun observeLocalArticles(): Flow<List<Article>> {
        return savedArticles
    }

    suspend fun saveLocalArticles(articles: List<Article>) {
        delay(300)
        savedArticles.update { articles }
    }

    suspend fun delete(article: Article) {
        delay(300)
        savedArticles.update { oldList ->
            oldList.filter { it.id != article.id }
        }
    }

}
