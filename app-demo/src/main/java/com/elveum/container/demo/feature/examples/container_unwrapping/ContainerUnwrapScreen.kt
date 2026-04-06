package com.elveum.container.demo.feature.examples.container_unwrapping

import android.graphics.Paint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elveum.container.demo.feature.examples.container_unwrapping.MatrixRepository.MatrixEffect
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.theme.Dimens
import kotlin.math.absoluteValue

@Composable
fun ContainerUnwrapScreen() = DemoScaffold(
    title = "Container Unwrapping",
    description = """
        Any container can be unwrapped using the **unwrap()** extension function. This is especially
        useful when combining multiple containers within a **containerOf()** execution block.

        This example fetches the glyph count and color concurrently using **async/await**, then
        unwraps both containers and combines the results into the final Matrix effect.
    """.trimIndent(),
    scrollable = false,
) {
    val viewModel = hiltViewModel<ContainerUnwrapViewModel>()
    val container by viewModel.stateFlow.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        container.fold(
            onPending = { CircularProgressIndicator() },
            onSuccess = { matrixEffect ->
                MatrixCanvas(matrixEffect)
            },
            onError = { exception ->
                Text(
                    text = "Error: ${exception.message}",
                    color = MaterialTheme.colorScheme.error,
                )
            },
        )
    }
}

@Composable
private fun MatrixCanvas(effect: MatrixEffect) {
    val infiniteTransition = rememberInfiniteTransition(label = "matrix")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "time",
    )

    val textPaint = remember {
        Paint().apply {
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
    }

    val colorR = (effect.color.red * 255).toInt()
    val colorG = (effect.color.green * 255).toInt()
    val colorB = (effect.color.blue * 255).toInt()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(Dimens.MediumCorners))
            .background(Color.Black),
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val streamCount = effect.streams.size.coerceAtLeast(1)
        val charSize = size.width / (streamCount / 2)
        textPaint.textSize = charSize * 0.9f

        val fontMetrics = textPaint.fontMetrics
        val charHeight = -fontMetrics.ascent + fontMetrics.descent

        effect.streams.forEach { stream ->
            val x = stream.columnFraction * size.width
            val trailPixels = stream.trailLength * charHeight
            val progress = (stream.phaseOffset + time * stream.cyclesPerLoop).mod(1f)
            val headY = progress * (size.height + trailPixels)

            for (i in 0 until stream.trailLength) {
                val cy = headY - i * charHeight
                if (cy < -charHeight || cy > size.height + charHeight) continue

                val charIdx = ((x.toInt() * 31 + i * 17 + (time * 15).toInt()) * 1337)
                    .absoluteValue % effect.charPool.size
                val char = effect.charPool[charIdx]

                val trailFraction = 1f - i.toFloat() / stream.trailLength

                when (i) {
                    0 -> {
                        textPaint.setShadowLayer(charSize * 0.6f, 0f, 0f, android.graphics.Color.WHITE)
                        textPaint.color = android.graphics.Color.argb(230, 255, 255, 255)
                    }
                    1 -> {
                        textPaint.clearShadowLayer()
                        textPaint.color = android.graphics.Color.argb(210, 180, 255, 160)
                    }
                    else -> {
                        val alpha = (trailFraction * trailFraction * 255).toInt().coerceIn(0, 255)
                        textPaint.clearShadowLayer()
                        textPaint.color = android.graphics.Color.argb(alpha, colorR, colorG, colorB)
                    }
                }
                nativeCanvas.drawText(char.toString(), x, cy, textPaint)
            }
        }
    }
}
