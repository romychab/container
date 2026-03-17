package com.elveum.container.demo.feature.examples.subject_args

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elveum.container.BackgroundLoadState
import com.elveum.container.demo.feature.examples.subject_args.StarsRepository.Star
import com.elveum.container.demo.feature.examples.subject_args.StarsRepository.StarFilter
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.theme.Dimens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArgsScreen() = DemoScaffold(
    title = "Reactive Arguments",
    description = """
        LazyFlowSubject loaders support reactive arguments via **dependsOnFlow()**. When
        a dependency flow emits a new value, the loader re-executes automatically with
        the updated argument.

        This example re-fetches the star field whenever the color or size filter changes.
    """.trimIndent(),
) {
    val viewModel = hiltViewModel<ArgsViewModel>()
    val container by viewModel.stateFlow.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(5_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "time",
    )

    container.fold(
        onPending = { CircularProgressIndicator() },
        onSuccess = { filteredStars ->
            val stars = filteredStars.stars
            val filter = filteredStars.filter
            val isLoading = backgroundLoadState == BackgroundLoadState.Loading

            StarField(
                isLoading = isLoading,
                stars = stars,
                time = { time }
            )

            StarFiltersPanel(
                filter = filter,
                onToggleColor = viewModel::toggleColor,
                onChangeSizes = viewModel::setSizes,
            )
        },
        onError = {},
    )
}

@Composable
private fun StarField(
    isLoading: Boolean,
    stars: List<Star>,
    time: () -> Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isLoading) 0.7f else 1f)
        ) {
            val cornerSize = Dimens.MediumPadding.toPx()
            drawRoundRect(
                Color.Black,
                cornerRadius = CornerRadius(cornerSize, cornerSize)
            )
            val padding = Dimens.MediumPadding.toPx()
            val cellSize = (size.width - padding * 2) / STAR_FIELD_SIZE
            stars.forEach { star ->
                val cx = padding + star.x * cellSize + cellSize / 2f
                val cy = padding + star.y * cellSize + cellSize / 2f
                val r = star.size * cellSize * 0.18f
                val phase = ((star.id - 1) * GOLDEN_ANGLE).mod(TWO_PI)
                val speed = 0.3f + (star.id % 7) * 0.2f
                val blink =
                    (sin((time() * speed + phase).toDouble()).toFloat() + 1f) / 2f
                val alpha = 0.25f + 0.75f * blink
                drawStar(cx, cy, r, star.color, alpha)
            }
        }
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun StarFiltersPanel(
    filter: StarFilter,
    onToggleColor: (Color) -> Unit,
    onChangeSizes: (minSize: Float, maxSize: Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = "Colors")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AllStarColors.forEach { color ->
                val isSelected = color in filter.colors
                Surface(
                    shape = CircleShape,
                    color = color,
                    modifier = Modifier
                        .size(32.dp)
                        .alpha(if (isSelected) 1f else 0.4f),
                    onClick = { onToggleColor(color) },
                    border = if (isSelected) {
                        BorderStroke(2.dp, Color.Black)
                    } else {
                        BorderStroke(1.dp, Color.LightGray)
                    },
                ) {}
            }
        }

        Text(
            text = "Size: ${"%.1f".format(filter.minSize)} – ${"%.1f".format(filter.maxSize)}",
        )
        RangeSlider(
            value = filter.minSize..filter.maxSize,
            onValueChange = { range ->
                onChangeSizes(range.start, range.endInclusive)
            },
            valueRange = STAR_MIN_SIZE..STAR_MAX_SIZE,
        )
    }
}

private fun DrawScope.drawStar(cx: Float, cy: Float, r: Float, color: Color, alpha: Float) {
    val center = Offset(cx, cy)
    drawCircle(color.copy(alpha = 0.07f * alpha), radius = r * 7f, center = center)
    drawCircle(color.copy(alpha = 0.25f * alpha), radius = r * 3.2f, center = center)
    drawCircle(color.copy(alpha = 0.55f * alpha), radius = r * 1.6f, center = center)
    val outerR = r * 2.8f
    val innerR = r * 0.85f
    val path = Path().apply {
        for (i in 0 until 8) {
            val angle = -PI / 2.0 + i * PI / 4.0
            val radius = if (i % 2 == 0) outerR else innerR
            val px = cx + cos(angle).toFloat() * radius
            val py = cy + sin(angle).toFloat() * radius
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(path, color.copy(alpha = 0.9f * alpha))
    drawCircle(Color.White.copy(alpha = 0.9f * alpha), radius = r * 0.5f, center = center)
}

private const val TWO_PI = (2.0 * PI).toFloat()
private const val GOLDEN_ANGLE = 2.3999632f
