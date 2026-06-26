package com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elveum.store.demo.feature.examples.store_simple.local_remote_reactive.LocalRemoteReactiveSimpleStoreViewModel.UiArticle
import com.elveum.store.demo.ui.components.DemoScaffold
import com.elveum.store.demo.ui.theme.Dimens
import com.elveum.store.load.getOrNull
import com.elveum.store.load.hasAnyLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalRemoteReactiveSimpleStoreScreen() = DemoScaffold(
    title = "Reactive Local Cache",
    scrollable = false,
    description = """
        **SimpleStore** supports reactive local storages.
        
        Whenever you update data in the local storage, the store is updated
        immediately.
    """.trimIndent()
) {
    val viewModel: LocalRemoteReactiveSimpleStoreViewModel = hiltViewModel()
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
                items(
                    items = state.articles,
                    key = { article -> article.id }
                ) { article ->
                    ArticleCard(
                        modifier = Modifier.animateItem(),
                        article = article,
                        onDelete = { viewModel.delete(article) }
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
    modifier: Modifier,
    article: UiArticle,
    onDelete: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (article.isDeleting) {
                CircularProgressIndicator(Modifier.size(Dimens.SmallIconSize))
            } else {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(Dimens.SmallIconSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
