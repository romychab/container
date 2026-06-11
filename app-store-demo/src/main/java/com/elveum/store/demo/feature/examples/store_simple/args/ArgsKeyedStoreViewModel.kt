package com.elveum.store.demo.feature.examples.store_simple.args

import androidx.compose.ui.graphics.Color
import com.elveum.container.containerCombineWith
import com.elveum.store.demo.feature.examples.store_simple.args.StarsRepository.Star
import com.elveum.store.demo.feature.examples.store_simple.args.StarsRepository.StarFilter
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.getOrNull
import com.elveum.container.pendingContainer
import com.elveum.container.reducer.stateIn
import com.elveum.store.load.StoreResult
import com.elveum.store.load.getOrNull
import com.elveum.store.load.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class ArgsKeyedStoreViewModel @Inject constructor(
    private val repository: StarsRepository
) : AbstractViewModel() {

    val stateFlow = combine(
        repository.getStars(),
        repository.getFilter(),
    ) { result, filter ->
        result.map { State(it, filter) }
    }.stateIn(StoreResult.Loading)

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
