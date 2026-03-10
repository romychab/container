package com.elveum.container.demo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.elveum.container.demo.navigation.LocalNavController
import com.elveum.container.demo.ui.ExampleDescription
import com.elveum.container.demo.ui.theme.Dimens
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DemoAction(
    val icon: ImageVector,
    val state: DemoActionState = DemoActionState.Default,
    val onClick: () -> Unit,
)

enum class DemoActionState {
    Default,
    Disabled,
    Loading
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScaffold(
    title: String,
    description: String? = null,
    hasBackButton: Boolean = true,
    scrollable: Boolean = true,
    actions: ImmutableList<DemoAction> = persistentListOf(),
    contentPadding: PaddingValues = PaddingValues(horizontal = Dimens.MediumPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (hasBackButton) {
                        val navController = LocalNavController.current
                        IconButton(
                            onClick = { navController.navigateUp() },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    actions.forEach { action ->
                        IconButton(
                            onClick = action.onClick,
                            enabled = action.state != DemoActionState.Disabled,
                        ) {
                            if (action.state == DemoActionState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimens.ToolbarProgressIndicatorSize),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = Dimens.ToolbarProgressIndicatorStrokeWidth,
                                )
                            } else {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                if (description != null) {
                    ExampleDescription(description)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding)
                        .then(if (scrollable) {
                            Modifier.verticalScroll(rememberScrollState())
                        } else {
                            Modifier
                        })
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.MediumSpace, Alignment.CenterVertically),
                ) {
                    content()
                }
            }
        },
    )
}
