package com.elveum.container.demo.feature.examples.reducer_pattern

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elveum.container.demo.feature.examples.reducer_pattern.FlowToReducerViewModel.State
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.theme.Dimens

@Composable
fun FlowToReducerScreen() = DemoScaffold(
    title = "Reducer Basics",
    scrollable = false,
    description = """
        The **Reducer<T>** extends the idea behind standard **stateIn** / **shareIn**
        operators. You can call the **toReducer()** function on any **Flow<T>** to convert
        it into a **Reducer<T>**.
        
        The Reducer creates a state based on data emitted by the origin flow plus manual
        updates. 
        
        In this example, a color field is loaded from a repository, but
        the alpha channel is updated manually in the ViewModel.
    """.trimIndent()
) {
    val viewModel = hiltViewModel<FlowToReducerViewModel>()
    val state by viewModel.stateFlow.collectAsState()

    Text(
        text = "The color field is updated every 1 second by the repository:",
        textAlign = TextAlign.Center,
    )

    ColorFieldCanvas { state }

    Text(
        text = "Update the alpha channel in the state manually using the Slider:",
        textAlign = TextAlign.Center,
    )

    Slider(
        value = state.alpha,
        onValueChange = { alpha ->
            viewModel.setAlpha(alpha)
        },
        valueRange = 0f..1f,
    )

}

@Composable
fun ColorFieldCanvas(stateProvider: () -> State) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(Dimens.MediumCorners))
    ) {
        val state = stateProvider()
        val cells = state.field.cells
        val fieldSize = cells.size
        val cellWidth = size.width / fieldSize
        val cellHeight = size.width / fieldSize
        cells.forEachIndexed { row, list ->
            list.forEachIndexed { column, color ->
                drawRect(
                    color = color,
                    topLeft = Offset(x = cellWidth * column, y = cellHeight * row),
                    size = Size(cellWidth, cellHeight),
                    alpha = state.alpha,
                )
            }
        }
    }
}
