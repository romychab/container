package com.elveum.container.demo.feature.examples.reducer_container_flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elveum.container.demo.feature.examples.reducer_container_flow.LogsRepository.LogLevel
import com.elveum.container.demo.feature.examples.reducer_container_flow.LogsRepository.LogMessage
import com.elveum.container.demo.ui.components.DemoScaffold

@Composable
fun ContainerFlowToContainerReducerScreen() = DemoScaffold(
    title = "ContainerFlow to ContainerReducer",
    description = """
        Any Flow<Container<T>> can be converted into ContainerReducer<T> by using
        the **containerToReducer()** extension function.
        
        In this example, random logs are emitted by a repository and rendered on the screen.
        The user can filter logs by log level using checkboxes.
        
        The ViewModel state tracks changes from both sources - from the repository and
        from the user's filter selections.
    """.trimIndent(),
    scrollable = false,
) {
    val viewModel = hiltViewModel<ContainerFlowToContainerReducerViewModel>()
    val container by viewModel.stateFlow.collectAsState()

    container.fold(
        onPending = { CircularProgressIndicator() },
        onSuccess = { state ->
            LogCheckboxes(
                enabledLevels = state.enabledLevels,
                onToggleLevel = viewModel::toggleLogLevel,
            )

            HorizontalDivider()

            val lazyListState = rememberLazyListState()
            LaunchedEffect(state.filteredLogs.firstOrNull()) {
                if (lazyListState.firstVisibleItemIndex < 2) {
                    lazyListState.animateScrollToItem(index = 0)
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    items = state.filteredLogs,
                    key = { it.id },
                ) { log ->
                    LogRow(
                        log = log,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        },
        onError = {}
    )

}

@Composable
private fun LogCheckboxes(
    enabledLevels: Set<LogLevel>,
    onToggleLevel: (LogLevel) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LogLevel.entries.forEach { level ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = level in enabledLevels,
                    onCheckedChange = { onToggleLevel(level) },
                )
                Text(
                    text = level.name,
                    color = level.displayColor,
                )
            }
        }
    }
}

@Composable
private fun LogRow(
    log: LogMessage,
    modifier: Modifier,
) {
    Text(
        text = log.logMessage,
        color = log.level.displayColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

private val LogLevel.displayColor: Color
    get() = when (this) {
        LogLevel.Info -> Color(0xFF4FC3F7)
        LogLevel.Warning -> Color(0xFFFFAB00)
        LogLevel.Error -> Color(0xFFEF5350)
    }
