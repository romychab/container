package com.elveum.container.demo.feature.examples.pagination_statuses

import com.elveum.container.demo.errors.ErrorFlagProvider
import com.elveum.container.demo.feature.examples.pagination_statuses.BookRepository.Book
import com.elveum.container.demo.feature.examples.pagination_statuses.BookRepository.PagedResult
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject

class BookDataSource @Inject constructor(
    private val errorFlagProvider: ErrorFlagProvider,
    faker: Faker,
) {

    private val allBooks = List(100) {
        Book(
            id = it + 1,
            title = faker.book().title(),
            author = faker.book().author(),
            description = faker.lorem().sentence(12, 6),
        )
    }

    suspend fun fetchPage(pageKey: Int?): PagedResult {
        delay(2000)
        if (errorFlagProvider.isErrorFlagEnabled()) throw IOException("Network error. Please check your connection.")
        val page = pageKey ?: 0
        val start = page * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, allBooks.size)
        val books = allBooks.subList(start, end)
        val nextKey = if (end < allBooks.size) page + 1 else null
        return PagedResult(books, nextKey)
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
