package com.elveum.container.demo.feature.examples.pagination_updates

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.elveum.container.BackgroundLoadState
import com.elveum.container.LoadConfig
import com.elveum.container.demo.feature.examples.pagination_updates.PaginationUpdatesViewModel.UiProduct
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.components.NextPageFooter
import com.elveum.container.demo.ui.components.ProgressIconButton
import com.elveum.container.demo.ui.theme.Dimens
import com.elveum.container.subject.paging.nextPageState
import com.elveum.container.subject.paging.onItemRendered

@Composable
fun PaginationUpdatesScreen() = DemoScaffold(
    title = "Pagination with Updates",
    scrollable = false,
    description = """
        Demonstrates a paged list that supports per-item updates. Each item
        has a **Like** toggle that can be flipped independently of pagination.
    """.trimIndent(),
) {
    val viewModel = hiltViewModel<PaginationUpdatesViewModel>()
    val container by viewModel.stateFlow.collectAsState()

    container.fold(
        onPending = { CircularProgressIndicator() },
        onError = {},
        onSuccess = { state ->
            val products = state.products
            PullToRefreshBox(
                isRefreshing = container.backgroundLoadState == BackgroundLoadState.Loading,
                onRefresh = { reload(LoadConfig.SilentLoading) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = Dimens.SmallPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
                ) {
                    itemsIndexed(
                        items = products,
                        key = { _, product -> product.id },
                    ) { index, product ->
                        LaunchedEffect(index) {
                            metadata.onItemRendered(index)
                        }
                        ProductCard(
                            product = product,
                            onToggleLike = viewModel::toggleLike,
                        )
                    }
                    item {
                        NextPageFooter(pageState = metadata.nextPageState)
                    }
                }
            }
        },
    )
}

@Composable
private fun ProductCard(
    product: UiProduct,
    onToggleLike: (UiProduct) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.MediumPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.TinySpace),
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val icon = if (product.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
            ProgressIconButton(
                imageVector = icon,
                isLoading = product.isToggling,
                contentDescription = "Like",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onToggleLike(product) }
            )
        }
    }
}
