package com.elveum.container.demo.feature.examples.pagination_statuses

import com.elveum.container.Container
import com.elveum.container.demo.errors.ErrorFlagRepository
import com.elveum.container.demo.feature.examples.pagination_statuses.BookRepository.Book
import com.elveum.container.demo.feature.examples.reducer_owner.AbstractViewModel
import com.elveum.container.pendingContainer
import com.elveum.container.reducer.stateIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PaginationStatusesViewModel @Inject constructor(
    bookRepository: BookRepository,
    private val errorFlagRepository: ErrorFlagRepository,
) : AbstractViewModel() {

    val booksFlow: StateFlow<Container<List<Book>>> = bookRepository
        .getBooks()
        .stateIn(pendingContainer())

    val isErrorFlagEnabledFlow = errorFlagRepository.getErrorFlag()

    fun toggleErrorFlag() {
        errorFlagRepository.toggle()
    }

}
