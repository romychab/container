package com.elveum.container.demo.feature.examples.reducer_owner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elveum.container.demo.feature.examples.reducer_owner.ParticlesRepository.Particle
import com.elveum.container.demo.feature.examples.reducer_owner.ReducerOwnerViewModel.State
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.theme.Dimens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI = (2.0 * PI).toFloat()

@Composable
fun ReducerOwnerScreen() = DemoScaffold(
    title = "Reducer Owner",
    scrollable = false,
    description = """
        Creating **Reducer<T>** instances is much simpler with the **ReducerOwner** interface.

        Implement **ReducerOwner** in an abstract ViewModel to specify the coroutine scope
        and **SharingStarted** strategy once. The following functions - **toReducer()**, **stateIn()**, 
        and **shareIn()** reuse the provided values automatically.
    """.trimIndent()
) {
    val viewModel = hiltViewModel<ReducerOwnerViewModel>()
    val state by viewModel.stateFlow.collectAsState()

    CometsCanvas { state }

    Text(
        text = "Particle size:",
        textAlign = TextAlign.Center,
    )
    Slider(
        value = state.particleSize,
        onValueChange = viewModel::setParticleSize,
        valueRange = 0f..1f,
    )
}

@Composable
private fun CometsCanvas(
    stateProvider: () -> State,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label = "bgAngle",
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(Dimens.MediumCorners)),
    ) {
        val state = stateProvider()
        val gradientCx = size.width * (0.5f + 0.3f * cos(bgAngle))
        val gradientCy = size.height * (0.5f + 0.3f * sin(bgAngle))
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0D2137), Color(0xFF020810)),
                center = Offset(gradientCx, gradientCy),
                radius = size.width * 1.1f,
            )
        )

        val baseRadius = size.width * 0.013f * (0.4f + state.particleSize * 1.2f)

        state.particles.forEach { particle ->
            drawComet(particle, baseRadius)
        }
    }
}

private fun DrawScope.drawComet(
    particle: Particle,
    headRadius: Float,
) {
    val headX = particle.x * size.width
    val headY = particle.y * size.height
    val head = Offset(headX, headY)
    val trailSize = particle.trail.size.coerceAtLeast(1)

    particle.trail.forEachIndexed { i, trailPos ->
        val fraction = 1f - (i + 1).toFloat() / trailSize
        val alpha = fraction * fraction * 0.75f
        val radius = headRadius * fraction * 0.65f
        if (radius > 0.5f) {
            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = radius,
                center = Offset(trailPos.x * size.width, trailPos.y * size.height),
            )
        }
    }

    drawCircle(particle.color.copy(alpha = 0.12f), radius = headRadius * 4.5f, center = head)
    drawCircle(particle.color.copy(alpha = 0.35f), radius = headRadius * 2.0f, center = head)
    drawCircle(particle.color.copy(alpha = 0.90f), radius = headRadius, center = head)
    drawCircle(Color.White.copy(alpha = 0.85f), radius = headRadius * 0.4f, center = head)
}
