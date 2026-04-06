package com.elveum.container.demo.feature.examples.pagination_statuses

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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elveum.container.BackgroundLoadState
import com.elveum.container.LoadConfig
import com.elveum.container.demo.feature.examples.pagination_statuses.BookRepository.Book
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.components.NextPageFooter
import com.elveum.container.demo.ui.theme.Dimens
import com.elveum.container.subject.paging.nextPageState
import com.elveum.container.subject.paging.onItemRendered

@Composable
fun PaginationStatusesScreen() {
    val viewModel = hiltViewModel<PaginationStatusesViewModel>()
    val container by viewModel.booksFlow.collectAsState()
    val failEnabled by viewModel.isErrorFlagEnabledFlow.collectAsState()

    DemoScaffold(
        title = "Pagination Statuses",
        scrollable = false,
        description = """
            Combines pull-to-refresh, incremental paging, and error handling with **pageLoader**.

            Enable **Simulate failures** to force errors. Initial-load errors show an inline
            error screen. Next-page errors appear in the footer with a **Retry** button.
        """.trimIndent(),
    ) {
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
                            text = "Failed to load books",
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
                onSuccess = { books ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = Dimens.SmallPadding),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
                    ) {
                        itemsIndexed(
                            items = books,
                            key = { _, book -> book.id },
                        ) { index, book ->
                            LaunchedEffect(index) {
                                metadata.onItemRendered(index)
                            }
                            BookCard(book = book)
                        }
                        item {
                            NextPageFooter(pageState = container.metadata.nextPageState)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun BookCard(book: Book) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.MediumPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.TinySpace),
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "by ${book.author}",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = book.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
