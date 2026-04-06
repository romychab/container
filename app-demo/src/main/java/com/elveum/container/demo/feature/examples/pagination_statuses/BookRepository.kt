package com.elveum.container.demo.feature.examples.pagination_statuses

import com.elveum.container.Container
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.listenReloadable
import com.elveum.container.subject.paging.pageLoader
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BookRepository @Inject constructor(
    private val dataSource: BookDataSource,
) {

    private val subject = LazyFlowSubject.create(
        valueLoader = pageLoader<Int?, Book>(
            initialKey = null,
            itemId = Book::id,
        ) { pageKey ->
            val result = dataSource.fetchPage(pageKey)
            emitPage(result.books)
            if (result.nextPageKey != null) emitNextKey(result.nextPageKey)
        }
    )

    fun getBooks(): Flow<Container<List<Book>>> = subject.listenReloadable()

    data class Book(
        val id: Int,
        val title: String,
        val author: String,
        val description: String,
    )

    data class PagedResult(
        val books: List<Book>,
        val nextPageKey: Int?,
    )
}
