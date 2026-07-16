package com.elveum.store.demo.feature.examples.store_paged.pagination_statuses

import com.elveum.container.reducer.stateIn
import com.elveum.store.demo.errors.ErrorFlagRepository
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.store.load.StoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PaginationStatusesViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val errorFlagRepository: ErrorFlagRepository,
) : AbstractViewModel() {

    val booksFlow: StateFlow<StoreResult<List<BookRepository.Book>>> = bookRepository
        .getBooks()
        .stateIn(StoreResult.Loading)

    val isErrorFlagEnabledFlow = errorFlagRepository.getErrorFlag()

    fun toggleErrorFlag() {
        errorFlagRepository.toggleErrorFlag()
    }

}
