package com.elveum.store.demo.feature.examples.store_keyed.master_details.list


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elveum.store.demo.feature.examples.store_keyed.master_details.details.CatDetailsRoute
import com.elveum.store.demo.feature.examples.store_keyed.master_details.model.Cat
import com.elveum.store.demo.navigation.ExampleRoute
import com.elveum.store.demo.navigation.LocalNavController
import com.elveum.store.demo.ui.components.DemoAction
import com.elveum.store.demo.ui.components.DemoActionState
import com.elveum.store.demo.ui.components.DemoScaffold
import com.elveum.store.demo.ui.theme.Dimens
import com.elveum.store.load.StoreResult
import com.elveum.store.load.isBackgroundLoading
import com.elveum.store.load.isCompleted
import kotlinx.collections.immutable.persistentListOf

@Composable
fun MasterListScreen() {
    val viewModel = hiltViewModel<CatListViewModel>()
    val state by viewModel.stateFlow.collectAsState()
    val navController = LocalNavController.current

    val actions = if (state.catsResult.isCompleted()) {
        persistentListOf(
            DemoAction(
                icon = Icons.Default.Refresh,
                onClick = viewModel::refresh,
                state = if (state.catsResult.isBackgroundLoading()) DemoActionState.Loading else DemoActionState.Default
            )
        )
    } else {
        persistentListOf()
    }


    DemoScaffold(
        title = "Master Details",
        scrollable = false,
        description = """
            **KeyedStore** usage. The first one manages item details. The
            second one manages the list of items.
        """.trimIndent(),
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
        when (val finalResult = state.catsResult) {
            StoreResult.Loading -> { CircularProgressIndicator(Modifier.weight(1f).wrapContentHeight()) }
            is StoreResult.Loaded -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(finalResult.value) { cat ->
                        CatCard(
                            cat = cat,
                            onLaunchDetails = {
                                navController.launch(CatDetailsRoute(cat.id))
                            }
                        )
                    }
                }
            }
            is StoreResult.Failed -> {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Failed to load cats.")
                    Button(
                        onClick = viewModel::tryAgain,
                        enabled = !finalResult.isBackgroundLoading(),
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
private fun CatCard(
    cat: Cat,
    onLaunchDetails: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.padding(vertical = Dimens.SmallPadding),
        onClick = onLaunchDetails,
    ) {
        Row(
            modifier = Modifier.padding(all = Dimens.SmallPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
        ) {
            AsyncImage(
                model = cat.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
            )
            Text(
                text = cat.name,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
