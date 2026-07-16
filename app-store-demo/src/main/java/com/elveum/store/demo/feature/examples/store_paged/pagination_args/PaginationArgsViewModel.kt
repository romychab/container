package com.elveum.store.demo.feature.examples.store_paged.pagination_args

import com.elveum.store.demo.feature.examples.store_paged.pagination_args.PhotoRepository.Photo
import com.elveum.store.demo.feature.examples.store_paged.pagination_args.PhotoRepository.PhotoCategory
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.stateIn
import com.elveum.store.load.StoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PaginationArgsViewModel @Inject constructor(
    private val repository: PhotoRepository,
) : AbstractViewModel() {

    val photosFlow: StateFlow<StoreResult<List<Photo>>> = repository
        .getPhotos()
        .stateIn(StoreResult.Loading)

    val selectedCategories: StateFlow<Set<PhotoCategory>> = repository.getSelectedCategories()

    fun toggleCategory(category: PhotoCategory) {
        repository.toggleCategory(category)
    }

}
