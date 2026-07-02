package com.elveum.store.demo.feature.examples.store_simple.combined

import com.elveum.container.reducer.Reducer
import com.elveum.container.reducer.combineToReducer
import com.elveum.store.demo.effects.Toaster
import com.elveum.store.demo.errors.ErrorFlagRepository
import com.elveum.store.demo.feature.examples.store_simple.combined.GalleryRepository.GalleryImage
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.store.load.StoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@HiltViewModel
class CombinedSimpleStoreViewModel @Inject constructor(
    private val galleryRepository: GalleryRepository,
    private val errorFlagRepository: ErrorFlagRepository,
    private val toaster: Toaster,
) : AbstractViewModel() {

    private val reducer: Reducer<State> = combineToReducer(
        galleryRepository.getGallery(),
        galleryRepository.getQuery().take(1),
        errorFlagRepository.getErrorFlag(),
        errorFlagRepository.getKeepContentOnErrorFlag(),
        initialState = ::State,
        nextState = State::copy,
    )

    val stateFlow: StateFlow<State> = reducer.stateFlow

    fun toggleLike(image: GalleryImage) = safeLaunch(toaster) {
        galleryRepository.toggleLike(image)
    }

    fun toggleErrors() {
        errorFlagRepository.toggleErrorFlag()
    }

    fun toggleKeepContentOnError() {
        errorFlagRepository.toggleKeepContentOnErrorFlag()
    }

    fun setQuery(query: String) = safeLaunch(toaster) {
        galleryRepository.setQuery(query)
    }

    data class State(
        val images: StoreResult<List<GalleryImage>> = StoreResult.Loading,
        val initialQuery: String = "",
        val isErrorsEnabled: Boolean = false,
        val isKeepContentOnError: Boolean = false,
    )

}