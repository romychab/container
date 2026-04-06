package com.elveum.container.demo.feature.examples.subject_args

import androidx.compose.ui.graphics.Color
import com.elveum.container.demo.feature.examples.subject_args.StarsRepository.StarFilter
import com.elveum.container.demo.feature.examples.subject_args.StarsRepository.Star
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class StarsDataSource @Inject constructor(
    random: Random,
) {

    private val allStars = List(100) { index ->
        Star(
            id = index + 1L,
            size = STAR_MIN_SIZE + random.nextFloat() * (STAR_MAX_SIZE - STAR_MIN_SIZE),
            color = AllStarColors[random.nextInt(AllStarColors.size)],
            x = random.nextFloat() * STAR_FIELD_SIZE,
            y = random.nextFloat() * STAR_FIELD_SIZE,
        )
    }

    suspend fun fetchStars(filter: StarFilter): List<Star> {
        delay(2000)
        return allStars.filter { star ->
            star.size >= filter.minSize &&
            star.size <= filter.maxSize &&
            star.color in filter.colors
        }
    }

}

const val STAR_MIN_SIZE = 1f
const val STAR_MAX_SIZE = 7f
const val STAR_FIELD_SIZE = 100

val AllStarColors = listOf(
    Color.Yellow,
    Color.White,
    Color.Red,
    Color.Blue,
    Color.Cyan,
    Color.Magenta,
)
