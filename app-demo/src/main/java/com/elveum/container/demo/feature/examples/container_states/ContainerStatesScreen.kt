package com.elveum.container.demo.feature.examples.container_states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elveum.container.Container
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.theme.Dimens

@Composable
fun ContainerStatesScreen() = DemoScaffold(
    title = "Container States",
    description = """
        This example shows a simple usage of **Container<T>** class, without
        any additional tools provided by the library and without loaders. 
        
        Tap on buttons at the bottom to switch between states.        
    """.trimIndent()
) {
    val viewModel: ContainerStatesViewModel = hiltViewModel()
    val container by viewModel.state.collectAsState()

    Text(
        "Current state: Container.${container::class.simpleName}",
        style = MaterialTheme.typography.titleMedium,
    )

    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        // use Container<T>.fold or standard Kotlin WHEN operator to render the state:
        container.fold(
            onPending = { CircularProgressIndicator() },
            onSuccess = { value -> Text(value) },
            onError = { exception ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.MediumSpace, Alignment.CenterVertically),
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error Icon",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Error: ${exception.message}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.SmallPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
    ) {
        StateButton("Loading", selected = container is Container.Pending, onClick = viewModel::showLoading)
        StateButton("Success", selected = container is Container.Success, onClick = viewModel::showSuccess)
        StateButton("Error", selected = container is Container.Error, onClick = viewModel::showError)
    }
}

@Composable
private fun RowScope.StateButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(label)
        }
    }
}
