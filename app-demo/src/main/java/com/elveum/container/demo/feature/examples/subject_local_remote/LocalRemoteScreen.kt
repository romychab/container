package com.elveum.container.demo.feature.examples.subject_local_remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.elveum.container.demo.feature.examples.subject_local_remote.ArticleRepository.Article
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.theme.Dimens
import com.elveum.container.getOrNull
import com.elveum.container.isDataLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalRemoteScreen() = DemoScaffold(
    title = "Local and Remote Cache",
    scrollable = false,
    description = """
        A LazyFlowSubject loader can emit multiple values in sequence before completing.
        
        This example emits local cached articles first, then replaces them with fresh
        remote data.

        Each emission carries a **SourceType** metadata value (LocalSourceType
        or RemoteSourceType), so the screen can indicate the origin of the displayed data.
    """.trimIndent()
) {
    val viewModel: LocalRemoteViewModel = hiltViewModel()
    val container by viewModel.stateFlow.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        container.getOrNull()?.let { state ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    vertical = Dimens.SmallPadding,
                ),
            ) {
                itemsIndexed(
                    items = state.articles,
                    key = { _, article -> article.id }
                ) { index, article ->
                    ArticleCard(
                        article = article,
                        isLocal = state.isLocal,
                        index = index,
                    )
                }
            }
        }
        if (container.isDataLoading()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun ArticleCard(
    article: Article,
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
