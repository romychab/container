package com.elveum.container.demo.feature.examples.reducer_container

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Singleton
class MathRepository @Inject constructor() {

    fun getMathPoints(): Flow<List<MathPoint>> = flow {
        delay(2_000)
        var frame = 0L
        while (true) {
            emit(generateRoseCurve(frame))
            delay(50)
            frame++
        }
    }

    private fun generateRoseCurve(frame: Long): List<MathPoint> {
        val phase = frame * (2.0 * PI / 160)
        val n = 4.0 + 3.0 * sin(phase)
        val steps = 100
        return (0..steps).map { i ->
            val theta = PI * i / steps
            val r = cos(n * theta)
            MathPoint(
                x = (r * cos(theta)).toFloat(),
                y = (r * sin(theta)).toFloat(),
            )
        }
    }

    data class MathPoint(val x: Float, val y: Float)
}
