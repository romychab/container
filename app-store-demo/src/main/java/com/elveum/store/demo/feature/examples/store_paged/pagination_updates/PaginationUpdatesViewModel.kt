package com.elveum.store.demo.feature.examples.store_paged.pagination_updates

import androidx.lifecycle.viewModelScope
import com.elveum.store.demo.feature.examples.store_paged.pagination_updates.ProductRepository.Product
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.stateIn
import com.elveum.store.load.StoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaginationUpdatesViewModel @Inject constructor(
    private val repository: ProductRepository,
) : AbstractViewModel() {

    val stateFlow = repository
        .getProducts()
        .stateIn(StoreResult.Loading)

    fun toggleLike(product: Product) {
        viewModelScope.launch {
            repository.toggleLike(product)
        }
    }

    fun onItemRendered(index: Int) {
        repository.onItemRendered(index)
    }

}
