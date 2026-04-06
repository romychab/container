package com.elveum.container.demo.feature.examples.reducer_state_pattern

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elveum.container.demo.feature.examples.reducer_state_pattern.ReducerPrivatePublicStatePatternViewModel.FlattenedItem
import com.elveum.container.demo.ui.components.DemoScaffold

@Composable
fun ReducerPrivatePublicStatePatternScreen() = DemoScaffold(
    title = "Private-Public State",
    scrollable = false,
    description = """
        This example shows how to use the private-public state pattern in a ViewModel
        in combination with **Reducer**. This approach allows you to expose only the final
        public screen state while keeping its private implementation inside the ViewModel.

        The private state can be updated both by the origin flow converted to a **Reducer**,
        and by a manual call to **updateState()**.
        
        In this example, the private view-model state holds **a tree structure**, but the 
        public state contains **flattened items** ready to be rendered in the LazyColumn.
    """.trimIndent()
) {
    val viewModel = hiltViewModel<ReducerPrivatePublicStatePatternViewModel>()
    val container by viewModel.stateFlow.collectAsState()
    container.fold(
        onPending = { CircularProgressIndicator() },
        onSuccess = { state ->
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.flattenedItems, key = { it.nodeId }) { item ->
                    TreeItemRow(
                        item = item,
                        onToggle = { viewModel.toggle(item) },
                        onDelete = { viewModel.delete(item) },
                        modifier = Modifier.animateItem(),
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = (item.level * INDENT_DP + ICON_SIZE_DP).dp))
                }
            }
        },
        onError = {}
    )
}

@Composable
private fun TreeItemRow(
    item: FlattenedItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onToggle,
            )
            .padding(start = (item.level * INDENT_DP).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.hasChildren) {
            Icon(
                modifier = Modifier.size(ICON_SIZE_DP.dp),
                imageVector = if (item.isExpanded) {
                    Icons.Default.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Default.KeyboardArrowRight
                },
                contentDescription = if (item.isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Box(modifier = Modifier.size(ICON_SIZE_DP.dp))
        }

        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (item.description.isNotEmpty()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (item.level > 0) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private const val INDENT_DP = 20
private const val ICON_SIZE_DP = 36
