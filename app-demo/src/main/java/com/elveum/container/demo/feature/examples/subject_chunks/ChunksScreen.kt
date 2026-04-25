package com.elveum.container.demo.feature.examples.subject_chunks

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elveum.container.LoadConfig
import com.elveum.container.demo.feature.examples.subject_chunks.MosaicRepository.MosaicData
import com.elveum.container.demo.ui.components.DemoAction
import com.elveum.container.demo.ui.components.DemoActionState
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.theme.Dimens
import com.elveum.container.isDataLoading
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ChunksScreen() {
    val viewModel = hiltViewModel<ChunksViewModel>()
    val container by viewModel.stateFlow.collectAsStateWithLifecycle()

    val actions = persistentListOf(
        DemoAction(
            icon = Icons.Default.Refresh,
            state = if (container.isDataLoading()) DemoActionState.Loading else DemoActionState.Default,
            onClick = { container.reload(LoadConfig.SilentLoading) },
        )
    )

    val infiniteTransition = rememberInfiniteTransition(label = "sheen")
    val sheenPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sheenPosition",
    )

    DemoScaffold(
        title = "Parallel Loading",
        scrollable = false,
        actions = actions,
        description = """
            This example demonstrates parallel data loading using **LazyFlowSubject**.

            Mosaic metadata is fetched first (2 seconds). Then all **144 tiles** are
            requested concurrently. Each tile appears the moment its data arrives,
            revealing the pixel art one piece at a time.
        """.trimIndent(),
    ) {
        container.fold(
            onPending = { CircularProgressIndicator() },
            onSuccess = { data ->
                Text(
                    text = "${data.info.title} - ${data.loadedCount} / ${data.totalTiles} tiles",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                LinearProgressIndicator(
                    progress = { data.loadedCount.toFloat() / data.totalTiles },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
                MosaicCanvas(
                    data = data,
                    modifier = Modifier.weight(1f),
                    sheenProvider = { sheenPosition },
                )
            },
            onError = {},
        )
    }
}

@Composable
private fun MosaicCanvas(
    data: MosaicData,
    modifier: Modifier,
    sheenProvider: () -> Float,
) {
    val cols = data.info.cols
    val rows = data.info.rows
    Canvas(
        modifier = modifier
            .aspectRatio(cols.toFloat() / rows.toFloat())
            .clip(RoundedCornerShape(Dimens.MediumCorners)),
    ) {
        val cellW = size.width / cols
        val cellH = size.height / rows
        val gap = 2.dp.toPx()
        val cornerRadius = CornerRadius(3.dp.toPx())

        // Background
        drawRect(data.info.backgroundColor)

        // Tiles
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val tile = data.getTile(row, col) ?: continue
                drawRoundRect(
                    color = tile,
                    topLeft = Offset(col * cellW + gap / 2f, row * cellH + gap / 2f),
                    size = Size(cellW - gap, cellH - gap),
                    cornerRadius = cornerRadius,
                )
            }
        }

        // Subtle grid hint for unloaded tiles
        if (data.loadedCount < data.totalTiles) {
            val gridColor = Color.White.copy(alpha = 0.05f)
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    if (data.getTile(row, col) == null) {
                        drawRoundRect(
                            color = gridColor,
                            topLeft = Offset(col * cellW + gap / 2f, row * cellH + gap / 2f),
                            size = Size(cellW - gap, cellH - gap),
                            cornerRadius = cornerRadius,
                        )
                    }
                }
            }
        }

        // Diagonal sheen: 45 degrees band sweeping top-left -> bottom-right.
        val t = sheenProvider()
        val d = size.height  // band extent along X; equals H for a 45° diagonal
        val sweepX = t * (size.width + 3f * d) - 2f * d
        val sheenBrush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.50f),
                Color.White.copy(alpha = 0.20f),
                Color.Transparent,
            ),
            start = Offset(sweepX, 0f),
            end = Offset(sweepX + d, size.height),
        )
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val isForegroundTile = data.getTile(row, col).let {
                    it != null && it != data.info.backgroundColor
                }
                if (isForegroundTile) {
                    drawRoundRect(
                        brush = sheenBrush,
                        topLeft = Offset(col * cellW + gap / 2f, row * cellH + gap / 2f),
                        size = Size(cellW - gap, cellH - gap),
                        cornerRadius = cornerRadius,
                    )
                }
            }
        }
    }
}
