package com.elveum.store.demo.feature.examples.store_keyed.basic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elveum.store.demo.feature.examples.store_keyed.basic.model.ListItem
import com.elveum.store.demo.ui.components.DemoScaffold
import com.elveum.store.demo.ui.theme.Dimens
import com.elveum.store.load.StoreResult
import com.elveum.store.load.isBackgroundLoading

@Composable
fun BasicListScreen() = DemoScaffold(
    title = "Basic List",
    scrollable = false,
    description = """
        A **KeyedStore** loads each item's description separately and in parallel while
        a **SimpleStore** holds the list with basic data. The repository merges
        both into a single flow, so descriptions appear as soon as they arrive.
    """.trimIndent(),
) {
    val viewModel = hiltViewModel<BasicItemsViewModel>()
    val result by viewModel.stateFlow.collectAsState()
    when (val finalResult = result) {
        StoreResult.Loading -> CircularProgressIndicator()
        is StoreResult.Loaded -> {
            PullToRefreshBox(
                isRefreshing = result.isBackgroundLoading(),
                onRefresh = viewModel::refresh,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(finalResult.value.items, key = { it.id }) { item ->
                        ItemCard(item)
                    }
                }
            }
        }
        is StoreResult.Failed -> {
            Text("Failed to load items")
            Button(
                onClick = viewModel::tryAgain,
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun ItemCard(item: ListItem) {
    ElevatedCard(
        modifier = Modifier.padding(vertical = Dimens.SmallPadding),
    ) {
        Row(
            modifier = Modifier.padding(all = Dimens.SmallPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(Dimens.MediumCorners)),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.TinySpace),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Description(item.description)
            }
        }
    }
}

@Composable
private fun Description(description: String?) {
    if (description == null) {
        Text(
            text = "Loading description…",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
