package com.elveum.container.demo.feature.examples.reducer_container

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elveum.container.demo.feature.examples.reducer_container.FlowToContainerReducerViewModel.State
import com.elveum.container.demo.feature.examples.reducer_container.FlowToContainerReducerViewModel.StrokeWidth
import com.elveum.container.demo.ui.components.DemoScaffold

@Composable
fun FlowToContainerReducerScreen() = DemoScaffold(
    title = "ContainerReducer",
    description = """
        **ContainerReducer<T>** is a subtype of Reducer that handles values encapsulated into
        Container<T> out of the box. 
        
        To convert any Flow<T> into ContainerReducer<T>, call the **toContainerReducer()** extension
        on any Flow<T>. While the origin flow is loading data, **pendingContainer()** is automatically emitted by the reducer.

        The example also shows the usage of the **updateState()** function to update state values without triggering a reload.
    """.trimIndent(),
) {
    val viewModel = hiltViewModel<FlowToContainerReducerViewModel>()
    val container by viewModel.state.collectAsState()

    container.fold(
        onPending = { CircularProgressIndicator() },
        onSuccess = { state ->
            RoseCanvas { state }
            RoseStylePanel(
                currentColor = state.color,
                currentStrokeWidth = state.strokeWidth,
                onColorChanged = viewModel::setColor,
                onStrokeChanged = viewModel::setStrokeWidth,
            )
        },
        onError = {}
    )

}

@Composable
private fun RoseCanvas(
    stateProvider: () -> State,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val state = stateProvider()
        if (state.points.size < 2) return@Canvas
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = minOf(cx, cy) * 0.9f

        val path = Path().apply {
            val first = state.points.first()
            moveTo(cx + first.x * scale, cy - first.y * scale)
            state.points.drop(1).forEach { pt ->
                lineTo(cx + pt.x * scale, cy - pt.y * scale)
            }
        }

        drawPath(
            path = path,
            color = state.color,
            style = Stroke(
                width = state.strokeWidth.dp.dp.toPx(),
                cap = StrokeCap.Round,
            ),
        )
    }
}

@Composable
private fun RoseStylePanel(
    currentColor: Color,
    currentStrokeWidth: StrokeWidth,
    onColorChanged: (Color) -> Unit,
    onStrokeChanged: (StrokeWidth) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Colors.forEach { color ->
            val isSelected = currentColor == color
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        }
                        else {
                            Modifier
                        }
                    )
                    .clickable { onColorChanged(color) }
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StrokeWidth.entries.forEach { width ->
            val isSelected = currentStrokeWidth == width
            if (isSelected) {
                Button(onClick = { onStrokeChanged(width) }) {
                    Text(width.name)
                }
            } else {
                OutlinedButton(onClick = { onStrokeChanged(width) }) {
                    Text(width.name)
                }
            }
        }
    }
}

private val Colors = listOf(
    Color(0xFFFFAB00), // Amber
    Color(0xFF69F0AE), // Green
    Color.Cyan,
    Color.Magenta,
    Color.DarkGray,
)
