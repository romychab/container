package com.elveum.container.demo.feature.examples.reducer_pattern

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.random.Random

class ColorFieldRepository @Inject constructor(
    private val random: Random,
) {

    fun getColorField(): Flow<ColorField> {
        return flow {
            while (true) {
                emit(buildColorField())
                delay(1000)
            }
        }
    }

    private fun buildColorField(): ColorField {
        return ColorField(
            cells = List(SIZE) {
                List(SIZE) {
                    Color(-random.nextInt(0xFFFFFF))
                }.toImmutableList()
            }.toImmutableList()
        )
    }

    data class ColorField(
        val cells: List<List<Color>> = persistentListOf(),
    )
}

private const val SIZE = 8
