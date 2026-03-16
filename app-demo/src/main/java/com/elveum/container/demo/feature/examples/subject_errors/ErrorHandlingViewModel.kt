package com.elveum.container.demo.feature.examples.subject_errors

import com.elveum.container.Container
import com.elveum.container.demo.feature.examples.reducer_owner.AbstractViewModel
import com.elveum.container.demo.feature.examples.subject_errors.GalleryRepository.GalleryImage
import com.elveum.container.reducer.ContainerReducer
import com.elveum.container.reducer.containerToReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ErrorHandlingViewModel @Inject constructor(
    repository: GalleryRepository,
) : AbstractViewModel() {

    private val reducer: ContainerReducer<State> = repository
        .getGallery()
        .containerToReducer(
            initialState = ::State,
            nextState = State::copy,
        )

    val stateFlow: StateFlow<Container<State>> = reducer.stateFlow

    data class State(
        val images: List<GalleryImage>,
    )

}
