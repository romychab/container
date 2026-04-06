package com.elveum.container.demo.feature.examples.pagination_basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elveum.container.demo.feature.examples.pagination_basic.PhotoRepository.Photo
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.theme.Dimens
import com.elveum.container.subject.paging.PageState
import com.elveum.container.subject.paging.nextPageState
import com.elveum.container.subject.paging.onItemRendered

@Composable
fun BasicPaginationScreen() = DemoScaffold(
    title = "Pagination Basics",
    scrollable = false,
    description = """
        Demonstrates key-based pagination with **LazyFlowSubject** and **pageLoader**.
        A nullable initial key triggers the first page load. Each page emits its items
        and the next key. New pages load automatically as items scroll into view.
    """.trimIndent(),
) {
    val viewModel = hiltViewModel<BasicPaginationViewModel>()
    val container by viewModel.stateFlow.collectAsState()
    container.fold(
        onPending = { CircularProgressIndicator() },
        onSuccess = { photos ->
            Text(
                text = "Loaded Count: ${photos.size}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.TinyPadding),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalItemSpacing = Dimens.SmallSpace,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
            ) {
                itemsIndexed(
                    items = photos,
                    key = { _, photo -> photo.id },
                ) { index, photo ->
                    LaunchedEffect(index) {
                        metadata.onItemRendered(index)
                    }
                    PhotoCard(photo = photo)
                }
                item(span = StaggeredGridItemSpan.FullLine) {
                    NextPageFooter(pageState = container.metadata.nextPageState)
                }
            }
        },
        onError = {},
    )
}

@Composable
private fun PhotoCard(photo: Photo) {
    ElevatedCard(
        shape = RoundedCornerShape(Dimens.MediumCorners),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            AsyncImage(
                model = photo.url,
                contentDescription = photo.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f)),
                        )
                    )
                    .padding(horizontal = Dimens.SmallPadding, vertical = Dimens.TinyPadding),
            ) {
                Text(
                    text = photo.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = photo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NextPageFooter(pageState: PageState) {
    if (pageState !is PageState.Pending) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.MediumPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
