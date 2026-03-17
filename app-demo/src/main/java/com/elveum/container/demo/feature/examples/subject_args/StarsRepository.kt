package com.elveum.container.demo.feature.examples.subject_args

import androidx.compose.ui.graphics.Color
import com.elveum.container.Container
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.listenReloadable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class StarsRepository @Inject constructor(
    private val startsDataSource: StarsDataSource,
) {

    private val filterFlow = MutableStateFlow(StarFilter())

    private val subject = LazyFlowSubject.create(
        reloadDependenciesPeriodMillis = 100L,
    ) {
        val filter: StarFilter = dependsOnFlow("filter") { filterFlow }
        val stars = startsDataSource.fetchStars(filter)
        emit(stars)
    }

    fun getStars(): Flow<Container<List<Star>>> {
        return subject.listenReloadable()
    }

    fun getFilter(): Flow<StarFilter> = filterFlow

    fun updateFilter(filter: StarFilter) {
        filterFlow.update { filter }
    }

    data class Star(
        val id: Long,
        val size: Float,
        val color: Color,
        val x: Float,
        val y: Float,
    )

    data class StarFilter(
        val minSize: Float = STAR_MIN_SIZE,
        val maxSize: Float = STAR_MAX_SIZE,
        val colors: Set<Color> = AllStarColors.toSet(),
    )

}
