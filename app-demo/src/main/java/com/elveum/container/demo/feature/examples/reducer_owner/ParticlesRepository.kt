package com.elveum.container.demo.feature.examples.reducer_owner

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.random.Random

class ParticlesRepository @Inject constructor(
    private val random: Random,
) {

    fun getParticles(): Flow<List<Particle>> = flow {
        var states = List(PARTICLE_COUNT, ::buildBezierState)
        while (true) {
            emit(states.map { s -> Particle(s.x, s.y, s.color, s.trail) })
            delay(50)
            states = states.map { it.advance(random) }
        }
    }

    private fun buildBezierState(index: Int): BezierState {
        val sx = random.nextFloat(); val sy = random.nextFloat()
        val cx = random.nextFloat(); val cy = random.nextFloat()
        val ex = random.nextFloat(); val ey = random.nextFloat()
        val t = random.nextFloat()
        val mt = 1f - t
        val x = mt * mt * sx + 2f * mt * t * cx + t * t * ex
        val y = mt * mt * sy + 2f * mt * t * cy + t * t * ey
        return BezierState(
            id = index,
            color = PARTICLE_COLORS[index % PARTICLE_COLORS.size],
            x = x.mod(1f), y = y.mod(1f),
            startX = sx, startY = sy,
            ctrlX = cx, ctrlY = cy,
            endX = ex, endY = ey,
            t = t,
            trail = emptyList(),
        )
    }

    private data class BezierState(
        val id: Int,
        val color: Color,
        val x: Float,
        val y: Float,
        val startX: Float, val startY: Float,
        val ctrlX: Float,  val ctrlY: Float,
        val endX: Float,   val endY: Float,
        val t: Float,
        val trail: List<Offset>,
    ) {
        fun advance(random: Random): BezierState {
            val newT = (t + T_STEP).coerceAtMost(1f)
            val mt = 1f - newT
            val rawX = mt * mt * startX + 2f * mt * newT * ctrlX + newT * newT * endX
            val rawY = mt * mt * startY + 2f * mt * newT * ctrlY + newT * newT * endY
            val newX = rawX.mod(1f)
            val newY = rawY.mod(1f)
            val newTrail = (listOf(Offset(x, y)) + trail).take(TRAIL_LENGTH)
            return if (t + T_STEP >= 1f) {
                copy(
                    x = newX, y = newY,
                    startX = endX, startY = endY,
                    ctrlX = random.nextFloat(), ctrlY = random.nextFloat(),
                    endX = random.nextFloat(), endY = random.nextFloat(),
                    t = 0f,
                    trail = newTrail,
                )
            } else {
                copy(x = newX, y = newY, t = t + T_STEP, trail = newTrail)
            }
        }
    }

    data class Particle(
        val x: Float,
        val y: Float,
        val color: Color,
        val trail: List<Offset>,
    )

    private companion object {
        const val PARTICLE_COUNT = 20
        const val TRAIL_LENGTH = 18
        const val T_STEP = 0.012f

        val PARTICLE_COLORS = listOf(
            Color(0xFF4FC3F7),
            Color(0xFFFF7043),
            Color(0xFF66BB6A),
            Color(0xFFAB47BC),
            Color(0xFFEC407A),
            Color(0xFFFFD54F),
            Color(0xFF26C6DA),
            Color(0xFFFFA726),
        )
    }
}
