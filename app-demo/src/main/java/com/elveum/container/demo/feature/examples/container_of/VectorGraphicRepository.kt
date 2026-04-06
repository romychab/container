package com.elveum.container.demo.feature.examples.container_of

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.elveum.container.Container
import com.elveum.container.containerOf
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class VectorGraphicRepository @Inject constructor() {

    private var loadCount = 1

    fun initialGraphic(): VectorGraphic = generateSpirograph(0)

    suspend fun loadGraphic(): Container<VectorGraphic> = containerOf {
        delay(2000)
        loadCount++
        if (loadCount % 2 == 1) {
            generateSpirograph((loadCount / 2).mod(presets.size))
        } else {
            throw RuntimeException("Failed to load the graphic.")
        }
    }

    private fun generateSpirograph(index: Int): VectorGraphic {
        val (R, r, d) = presets[index]
        val color = presetColors[index]
        val period = 2.0 * PI * r / gcd(R, r)
        val steps = 2000
        val rawPoints = List(steps + 1) { i ->
            val t = i * period / steps
            val x = (R - r) * cos(t) + d * cos((R - r).toDouble() / r * t)
            val y = (R - r) * sin(t) - d * sin((R - r).toDouble() / r * t)
            Offset(x.toFloat(), y.toFloat())
        }
        val maxVal = rawPoints.maxOf { maxOf(abs(it.x), abs(it.y)) }
        val normalizedPoints = rawPoints.map { Offset(it.x / maxVal, it.y / maxVal) }
        return VectorGraphic(points = normalizedPoints, color = color)
    }

    data class VectorGraphic(
        val points: List<Offset>,
        val color: Color,
    )

    private companion object {
        val presets = listOf(
            Triple(5, 3, 5),
            Triple(7, 2, 5),
            Triple(9, 4, 7),
            Triple(8, 3, 6),
            Triple(11, 4, 8),
        )
        val presetColors = listOf(
            Color(0xFF00BCD4),
            Color(0xFFFF7043),
            Color(0xFF66BB6A),
            Color(0xFFAB47BC),
            Color(0xFFEC407A),
        )

        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    }
}
