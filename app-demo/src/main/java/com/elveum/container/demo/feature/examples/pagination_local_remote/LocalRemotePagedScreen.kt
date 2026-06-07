package com.elveum.container.demo.feature.examples.pagination_local_remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elveum.container.BackgroundLoadState
import com.elveum.container.LoadConfig
import com.elveum.container.demo.feature.examples.pagination_local_remote.PagedArticleRepository.Article
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.components.NextPageFooter
import com.elveum.container.demo.ui.theme.Dimens
import com.elveum.container.subject.paging.nextPageState
import com.elveum.container.subject.paging.onItemRendered

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalRemotePagedScreen() = DemoScaffold(
    title = "Local and Remote Cache",
    scrollable = false,
    description = """
        Paged list where each item is fetched from a local cache and remote source. Firstly,
        items from the cache are displayed, and after a small delay fresh items come from
        the remote source.
    """.trimIndent()
) {
    val viewModel: LocalRemotePagedViewModel = hiltViewModel()
    val container by viewModel.stateFlow.collectAsState()
    val failEnabled by viewModel.isErrorFlagEnabledFlow.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.TinySpace),
    ) {
        Checkbox(
            checked = failEnabled,
            onCheckedChange = { viewModel.toggleErrorFlag() },
        )
        Text(
            text = "Simulate failures",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    PullToRefreshBox(
        isRefreshing = container.backgroundLoadState == BackgroundLoadState.Loading,
        onRefresh = { container.reload(LoadConfig.SilentLoading) },
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        container.fold(
            onPending = {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            },
            onError = { exception ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpace, Alignment.CenterVertically),
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Failed to load articles",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = exception.message ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = ::reload) {
                        Text("Try Again")
                    }
                }
            },
            onSuccess = { articles ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = Dimens.SmallPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
                ) {
                    itemsIndexed(
                        items = articles,
                        key = { _, book -> book.id },
                    ) { index, article ->
                        LaunchedEffect(index) {
                            metadata.onItemRendered(index)
                        }
                        ArticleCard(
                            article = article,
                            index = index,
                        )
                    }
                    item {
                        NextPageFooter(pageState = metadata.nextPageState)
                    }
                }
            },
        )
    }

}

@Composable
private fun ArticleCard(
    article: Article,
    index: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.MediumPadding, vertical = Dimens.MediumPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SmallPadding),
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = Dimens.TinyPadding)
                    .alignByBaseline(),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Source badge
            SourceBadge(isLocal = article.isLocal)
        }
    }
}

@Composable
private fun SourceBadge(isLocal: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.TinySpace),
    ) {
        Icon(
            imageVector = if (isLocal) Icons.Default.Storage else Icons.Default.Cloud,
            contentDescription = if (isLocal) "Local" else "Remote",
            tint = if (isLocal) LocalColor else RemoteColor,
        )
        Text(
            text = if (isLocal) "Local" else "Remote",
            style = MaterialTheme.typography.labelSmall,
            color = if (isLocal) LocalColor else RemoteColor,
        )
    }
}

private val LocalColor = Color(0xFFFF8F00)
private val RemoteColor = Color(0xFF1E88E5)
