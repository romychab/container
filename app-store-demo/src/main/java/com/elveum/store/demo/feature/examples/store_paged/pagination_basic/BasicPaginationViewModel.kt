package com.elveum.store.demo.feature.examples.store_paged.pagination_basic

import com.elveum.store.demo.feature.examples.store_paged.pagination_basic.PhotoRepository.Photo
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.stateIn
import com.elveum.store.load.StoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BasicPaginationViewModel @Inject constructor(
    private val repository: PhotoRepository,
) : AbstractViewModel() {

    val stateFlow: StateFlow<StoreResult<List<Photo>>> = repository
        .getPhotos()
        .stateIn(StoreResult.Loading)

    fun onItemRendered(index: Int) {
        repository.onItemRendered(index)
    }
}
