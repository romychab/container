package com.elveum.container.demo.feature.examples.pagination_args

import com.elveum.container.Container
import com.elveum.container.demo.feature.examples.pagination_args.PhotoRepository.Photo
import com.elveum.container.demo.feature.examples.pagination_args.PhotoRepository.PhotoCategory
import com.elveum.container.demo.feature.examples.reducer_owner.AbstractViewModel
import com.elveum.container.pendingContainer
import com.elveum.container.reducer.stateIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PaginationArgsViewModel @Inject constructor(
    private val repository: PhotoRepository,
) : AbstractViewModel() {

    val photosFlow: StateFlow<Container<List<Photo>>> = repository
        .getPhotos()
        .stateIn(pendingContainer())

    val selectedCategories: StateFlow<Set<PhotoCategory>> = repository.getSelectedCategories()

    fun toggleCategory(category: PhotoCategory) {
        repository.toggleCategory(category)
    }

}
