package com.elveum.container.demo.feature.examples.pagination_args

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elveum.container.BackgroundLoadState
import com.elveum.container.demo.feature.examples.pagination_args.PhotoRepository.Photo
import com.elveum.container.demo.feature.examples.pagination_args.PhotoRepository.PhotoCategory
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.theme.Dimens
import com.elveum.container.subject.paging.PageState
import com.elveum.container.subject.paging.nextPageState
import com.elveum.container.subject.paging.onItemRendered

@Composable
fun PaginationArgsScreen() = DemoScaffold(
    title = "Pagination with Args",
    scrollable = false,
    description = """
        Adds a **category filter** on top of basic pagination. The filter is declared as a
        reactive argument via **dependsOnFlow()** inside the page loader, toggling a chip
        restarts paging from the first page automatically.
    """.trimIndent(),
) {
    val viewModel = hiltViewModel<PaginationArgsViewModel>()
    val container by viewModel.photosFlow.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()

    container.fold(
        onPending = { CircularProgressIndicator() },
        onSuccess = { photos ->
            CategoryChipsRow(
                selectedCategories = selectedCategories,
                onToggle = viewModel::toggleCategory,
            )
            val isReloading = backgroundLoadState == BackgroundLoadState.Loading
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .alpha(if (isReloading) 0.4f else 1f)
                        .fillMaxSize(),
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
                if (isReloading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
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
                            colors = listOf(Color.Transparent, photo.category.gradientColor()),
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
private fun CategoryChipsRow(
    selectedCategories: Set<PhotoCategory>,
    onToggle: (PhotoCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.MediumPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpace, Alignment.CenterHorizontally),
    ) {
        PhotoCategory.entries.forEach { category ->
            FilterChip(
                selected = category in selectedCategories,
                onClick = { onToggle(category) },
                label = { Text(category.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = category.chipColor(),
                    selectedLabelColor = Color.White,
                ),
            )
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

private fun PhotoCategory.gradientColor(): Color = when (this) {
    PhotoCategory.Nature -> Color(0xFF1B5E20).copy(alpha = 0.9f)
    PhotoCategory.City -> Color(0xFF0D47A1).copy(alpha = 0.9f)
    PhotoCategory.Art -> Color(0xFFB71C1C).copy(alpha = 0.9f)
}

private fun PhotoCategory.chipColor(): Color = when (this) {
    PhotoCategory.Nature -> Color(0xFF2E7D32)
    PhotoCategory.City -> Color(0xFF1565C0)
    PhotoCategory.Art -> Color(0xFFC62828)
}
