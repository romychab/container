package com.elveum.container.demo.feature.examples.container_of

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elveum.container.Container
import com.elveum.container.demo.feature.examples.container_of.VectorGraphicRepository.VectorGraphic
import com.elveum.container.demo.ui.components.DemoScaffold

@Composable
fun ContainerOfScreen() = DemoScaffold(
    title = "ContainerOf",
    description = """
        This example uses the **containerOf()** function that can wrap any execution
        results into a **Container.Completed<T>** value.

        Any successful result is wrapped into **Container.Success**, and any thrown exception
        is wrapped into **Container.Error**.

        **CancellationException** is handled separately allowing you to use **containerOf()**
        safely with suspending functions.

        Every second load in this example fails with an error.
    """.trimIndent()
) {
    val viewModel = hiltViewModel<ContainerOfViewModel>()
    val container by viewModel.stateFlow.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        container.fold(
            onPending = { CircularProgressIndicator() },
            onError = { exception ->
                Text(
                    text = exception.message ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            onSuccess = { graphic ->
                SpirographCanvas(graphic)
            },
        )
    }

    if (container is Container.Completed) {
        Button(
            onClick = viewModel::load,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text("Load")
        }
    }
}

@Composable
private fun SpirographCanvas(graphic: VectorGraphic) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D1A))
            .drawBehind { drawSpirograph(graphic) },
    )
}

private fun DrawScope.drawSpirograph(graphic: VectorGraphic) {
    val scale = size.minDimension / 2f * 0.88f
    val center = Offset(size.width / 2f, size.height / 2f)

    val path = Path().apply {
        graphic.points.forEachIndexed { i, p ->
            val px = center.x + p.x * scale
            val py = center.y + p.y * scale
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
    }

    drawPath(path, color = graphic.color.copy(alpha = 0.12f), style = Stroke(width = 18.dp.toPx()))
    drawPath(path, color = graphic.color.copy(alpha = 0.30f), style = Stroke(width = 6.dp.toPx()))
    drawPath(path, color = graphic.color.copy(alpha = 0.95f), style = Stroke(width = 1.5.dp.toPx()))
    drawCircle(color = graphic.color, radius = 3.dp.toPx(), center = center)
}
