package com.elveum.store.demo.feature.examples.store_paged.pagination_statuses

import com.elveum.store.StoreFactory
import com.elveum.store.load.LoadRequest
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

    fun refresh() {
        store.invalidateAsync(LoadRequest.Silent)
    }

    fun tryAgain() {
        store.invalidateAsync()
    }

    fun onItemRendered(index: Int) {
        store.onItemRendered(index)
    }

    data class Book(
        val id: Int,
        val title: String,
        val author: String,
        val description: String,
    )

}
