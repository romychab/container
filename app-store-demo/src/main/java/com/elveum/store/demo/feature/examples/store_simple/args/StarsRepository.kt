@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.store.demo.feature.examples.store_simple.args

import androidx.compose.ui.graphics.Color
import com.elveum.container.Container
import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StarsRepository @Inject constructor(
    private val startsDataSource: StarsDataSource,
) {

    private val store = StoreFactory.simpleStoreBuilder<List<Star>>()
        .withQuery(initialQuery = StarFilter())
        .build(onFetch = startsDataSource::fetchStars)

    fun getStars(): Flow<StoreResult<List<Star>>> {
        return store.observe()
    }

    fun getFilter(): Flow<StarFilter> = store.queryFlow

    fun updateFilter(filter: StarFilter) {
        store.submitQueryAsync(filter)
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
