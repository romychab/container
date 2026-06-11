package com.elveum.store.demo.feature.examples.store_simple.local_remote_suspending

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elveum.container.LocalSourceType
import com.elveum.store.demo.ui.components.DemoScaffold
import com.elveum.store.demo.ui.theme.Dimens
import com.elveum.container.getOrNull
import com.elveum.container.isDataLoading
import com.elveum.store.load.getOrNull
import com.elveum.store.load.hasAnyLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalRemoteSuspendingSimpleStoreScreen() = DemoScaffold(
    title = "Local and Remote Cache",
    scrollable = false,
    description = """
        **SimpleStore** can load values both from local and remote storage.
        
        This example emits local cached articles first, then replaces them with fresh
        remote data.
    """.trimIndent()
) {
    val viewModel: LocalRemoteSuspendingSimpleStoreViewModel = hiltViewModel()
    val result by viewModel.stateFlow.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        result.getOrNull()?.let { state ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
                contentPadding = PaddingValues(
                    vertical = Dimens.SmallPadding,
                ),
            ) {
                itemsIndexed(
                    items = state.articles,
                    key = { _, article -> article.id }
                ) { index, article ->
                    ArticleCard(
                        article = article,
                        isLocal = result.sourceType == LocalSourceType,
                        index = index,
                    )
                }
            }
        }
        if (result.hasAnyLoading()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun ArticleCard(
    article: ArticleRepository.Article,
    isLocal: Boolean,
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
            SourceBadge(isLocal = isLocal)
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
