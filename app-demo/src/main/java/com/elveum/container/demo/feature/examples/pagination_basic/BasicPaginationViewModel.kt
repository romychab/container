package com.elveum.container.demo.feature.examples.pagination_basic

import com.elveum.container.Container
import com.elveum.container.demo.feature.examples.pagination_basic.PhotoRepository.Photo
import com.elveum.container.demo.feature.examples.reducer_owner.AbstractViewModel
import com.elveum.container.pendingContainer
import com.elveum.container.reducer.stateIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BasicPaginationViewModel @Inject constructor(
    repository: PhotoRepository,
) : AbstractViewModel() {

    val stateFlow: StateFlow<Container<List<Photo>>> = repository
        .getPhotos()
        .stateIn(pendingContainer())

}
