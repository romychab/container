package com.elveum.container.demo.feature.examples.subject_args

import androidx.compose.ui.graphics.Color
import com.elveum.container.containerCombineWith
import com.elveum.container.demo.feature.examples.reducer_owner.AbstractViewModel
import com.elveum.container.demo.feature.examples.subject_args.StarsRepository.Star
import com.elveum.container.demo.feature.examples.subject_args.StarsRepository.StarFilter
import com.elveum.container.getOrNull
import com.elveum.container.pendingContainer
import com.elveum.container.reducer.stateIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ArgsViewModel @Inject constructor(
    private val repository: StarsRepository
) : AbstractViewModel() {

    val stateFlow = repository
        .getStars()
        .containerCombineWith(repository.getFilter(), ::State)
        .stateIn(pendingContainer())

    private val currentFilter get() = stateFlow.value.getOrNull()?.filter

    fun toggleColor(color: Color) {
        val oldFilter = currentFilter ?: return
        val newColors = if (oldFilter.colors.contains(color)) {
            oldFilter.colors - color
        } else {
            oldFilter.colors + color
        }
        val newFilter = oldFilter.copy(colors = newColors)
        repository.updateFilter(newFilter)
    }

    fun setSizes(minSize: Float, maxSize: Float) {
        val oldFilter = currentFilter ?: return
        val newFilter = oldFilter.copy(
            minSize = minSize,
            maxSize = maxSize,
        )
        repository.updateFilter(newFilter)
    }

    data class State(
        val stars: List<Star>,
        val filter: StarFilter,
    )

}
