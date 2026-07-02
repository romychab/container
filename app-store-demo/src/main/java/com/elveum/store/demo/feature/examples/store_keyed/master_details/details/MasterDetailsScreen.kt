package com.elveum.store.demo.feature.examples.store_keyed.master_details.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import com.elveum.container.LoadConfig
import com.elveum.store.demo.navigation.WithContent
import com.elveum.store.demo.navigation.examples.Category
import com.elveum.store.demo.navigation.examples.Example
import com.elveum.store.demo.ui.components.DemoAction
import com.elveum.store.demo.ui.components.DemoActionState
import com.elveum.store.demo.ui.components.DemoScaffold
import com.elveum.store.demo.ui.components.EditableField
import com.elveum.store.demo.ui.components.Heading
import com.elveum.store.demo.ui.theme.Dimens
import com.elveum.store.load.StoreResult
import com.elveum.store.load.hasAnyLoading
import com.elveum.store.load.invalidate
import com.elveum.store.load.isBackgroundLoading
import com.elveum.store.load.isCompleted
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
data class CatDetailsRoute(val catId: Long) : NavKey, WithContent {
    @Composable
    override fun Content() = MasterDetailsScreen(catId)
}

@Composable
fun MasterDetailsScreen(catId: Long) {
    val viewModel = hiltViewModel<CatDetailsViewModel, CatDetailsViewModel.Factory> {
        it.create(catId)
    }
    val state by viewModel.stateFlow.collectAsState()

    val actions = if (state.catResult.isCompleted()) {
        persistentListOf(
            DemoAction(
                icon = Icons.Default.Refresh,
                onClick = { state.catResult.invalidate(LoadConfig.SilentLoading) },
                state = if (state.catResult.isBackgroundLoading()) DemoActionState.Loading else DemoActionState.Default
            )
        )
    } else {
        persistentListOf()
    }
    DemoScaffold(
        title = "Cat Details",
        actions = actions,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.isErrorsEnabled,
                onCheckedChange = { viewModel.toggleErrorFlag() },
            )
            Text("Enable Load Errors")
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.isNonOptimisticUpdate,
                onCheckedChange = { viewModel.toggleOptimisticUpdates() },
            )
            Text("Enable non-optimistic Update")
        }

        when (val finalResult = state.catResult) {
            StoreResult.Loading -> CircularProgressIndicator(Modifier.weight(1f).wrapContentHeight())
            is StoreResult.Loaded -> {
                val cat = finalResult.value
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.MediumSpace, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AsyncImage(
                        model = cat.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape),
                    )

                    EditableField(
                        title = "Name",
                        value = cat.name,
                        isInProgress = state.isUpdateInProgress,
                        onNewValue = { newName ->
                            viewModel.updateCatName(newName)
                        }
                    )

                    Heading("Description")

                    Text(
                        text = cat.description,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            is StoreResult.Failed -> {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Failed to load the cat details.")
                    Button(
                        onClick = { finalResult.invalidate() },
                        enabled = !finalResult.isBackgroundLoading(),
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }

}
