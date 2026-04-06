package com.elveum.container.demo.feature.examples.container_unwrapping

import androidx.compose.ui.graphics.Color
import com.elveum.container.Container
import com.elveum.container.containerOf
import com.elveum.container.unwrap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.random.Random

class MatrixRepository @Inject constructor(
    private val random: Random,
) {

    suspend fun fetchMatrixEffect(): Container<MatrixEffect> = containerOf {
        coroutineScope {
            val deferredCount = async { fetchGlyphsCount() }
            val deferredColor = async { fetchColor() }
            val countContainer = deferredCount.await()
            val colorContainer = deferredColor.await()
            generateMatrixEffect(
                glyphCount = countContainer.unwrap(),
                glyphColor = colorContainer.unwrap(),
            )
        }
    }

    private suspend fun fetchGlyphsCount(): Container<Int> = containerOf {
        delay(900)
        200
    }

    private suspend fun fetchColor(): Container<Color> = containerOf {
        delay(500)
        Color(0xFF00DD44)
    }

    private fun generateMatrixEffect(glyphColor: Color, glyphCount: Int): MatrixEffect {
        val streamCount = glyphCount / 3
        val charPool = (0x30A1..0x30F6).map { it.toChar() }
        val streams = List(streamCount) { i ->
            MatrixEffect.GlyphStream(
                columnFraction = (i + 0.1f + random.nextFloat() * 0.6f) / streamCount,
                phaseOffset = random.nextFloat(),
                cyclesPerLoop = (1 + random.nextInt(3)).toFloat(),
                trailLength = 5 + random.nextInt(12),
            )
        }
        return MatrixEffect(streams = streams, color = glyphColor, charPool = charPool)
    }

    data class MatrixEffect(
        val streams: List<GlyphStream>,
        val color: Color,
        val charPool: List<Char>,
    ) {
        data class GlyphStream(
            val columnFraction: Float,
            val phaseOffset: Float,
            val cyclesPerLoop: Float,
            val trailLength: Int,
        )
    }
}
