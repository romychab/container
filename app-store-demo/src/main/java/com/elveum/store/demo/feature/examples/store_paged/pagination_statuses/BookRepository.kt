package com.elveum.store.demo.feature.examples.store_paged.pagination_statuses

import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BookRepository @Inject constructor(
    private val dataSource: BookDataSource,
) {

    private val store = StoreFactory.pagedStoreBuilder<Int, Book>(
        initialKey = 0,
        itemId = Book::id
    ).build(onFetch = dataSource::fetchPage)

    fun getBooks(): Flow<StoreResult<List<Book>>> = store.observe()

    data class Book(
        val id: Int,
        val title: String,
        val author: String,
        val description: String,
    )

}
