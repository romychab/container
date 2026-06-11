package com.elveum.store.demo.feature.examples.store_paged.pagination_statuses

import com.elveum.store.demo.errors.ErrorFlagProvider
import com.elveum.store.demo.feature.examples.store_paged.pagination_statuses.BookRepository.Book
import com.elveum.store.stores.paged.PagedList
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

    suspend fun fetchPage(pageKey: Int): PagedList<Int, Book> {
        delay(2000)
        if (errorFlagProvider.isErrorFlagEnabled()) throw IOException("Network error. Please check your connection.")
        val start = pageKey * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, allBooks.size)
        val books = allBooks.subList(start, end)
        val nextKey = if (end < allBooks.size) pageKey + 1 else null
        return PagedList(books, nextKey)
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
