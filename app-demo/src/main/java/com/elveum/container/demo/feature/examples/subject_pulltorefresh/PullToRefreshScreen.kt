package com.elveum.container.demo.feature.examples.subject_pulltorefresh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elveum.container.BackgroundLoadState
import com.elveum.container.LoadConfig
import com.elveum.container.demo.feature.examples.subject_pulltorefresh.CatsRepository.Cat
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.theme.Dimens

@Composable
fun PullToRefreshScreen() = DemoScaffold(
    title = "Pull to Refresh",
    scrollable = false,
    description = """
        LazyFlowSubject supports silent reloading: calling reload with **LoadConfig.SilentLoading**
        restarts the loader without replacing the current result with a Pending state.
        
        The **backgroundLoadState** metadata indicates whether a background load is in progress,
        which drives the pull-to-refresh indicator without hiding the existing content.
    """.trimIndent()
) {
    val viewModel: PullToRefreshViewModel = hiltViewModel()
    val container by viewModel.stateFlow.collectAsState()

    PullToRefreshBox(
        isRefreshing = container.backgroundLoadState == BackgroundLoadState.Loading,
        onRefresh = { container.reload(LoadConfig.SilentLoading) },
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        container.fold(
            onPending = { CircularProgressIndicator(Modifier.align(Alignment.Center)) },
            onSuccess = { state ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.cats) { cat ->
                        CatCard(cat)
                    }
                }
            },
            onError = {},
        )
    }
}

@Composable
private fun CatCard(cat: Cat) {
    ElevatedCard(
        modifier = Modifier.padding(vertical = Dimens.SmallPadding)
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
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = cat.name,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = cat.description,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
