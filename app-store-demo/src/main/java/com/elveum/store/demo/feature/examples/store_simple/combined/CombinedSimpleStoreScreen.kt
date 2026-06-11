package com.elveum.store.demo.feature.examples.store_simple.combined

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elveum.container.BackgroundLoadState
import com.elveum.store.demo.feature.examples.store_simple.combined.GalleryRepository.GalleryImage
import com.elveum.store.demo.ui.components.DemoAction
import com.elveum.store.demo.ui.components.DemoActionState
import com.elveum.store.demo.ui.components.DemoScaffold
import com.elveum.store.demo.ui.theme.Dimens
import com.elveum.store.load.StoreResult
import com.elveum.store.load.hasAnyLoading
import kotlinx.collections.immutable.persistentListOf


@Composable
fun CombinedSimpleStoreScreen() {
    val viewModel: CombinedSimpleStoreViewModel = hiltViewModel()
    val state by viewModel.stateFlow.collectAsState()
    val isRefreshInProgress = state.images.hasAnyLoading()
    val actions = persistentListOf(
        DemoAction(
            icon = Icons.Default.Refresh,
            state = if (isRefreshInProgress) DemoActionState.Loading else DemoActionState.Default,
            onClick = viewModel::refresh,
        )
    )

    DemoScaffold(
        title = "Combined SimpleStore",
        scrollable = false,
        actions = actions,
        description = """
            Load, refresh, cache data. Display cached data first. Handle errors. Keep existing
            data while loading and after subsequent errors. Optimistic updates (toggle likes)
            for each image in the list.
        """.trimIndent()
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.isErrorsEnabled,
                onCheckedChange = { viewModel.toggleErrors() },
            )
            Text("Enable Errors")
            Checkbox(
                checked = state.isKeepContentOnError,
                onCheckedChange = { viewModel.toggleKeepContentOnError() },
            )
            Text("Keep Content On Error")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Search by name: ")
            var value by rememberSaveable { mutableStateOf(state.initialQuery) }
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    viewModel.setQuery(it)
                }
            )
        }

        when (val images = state.images) {
            StoreResult.Loading -> { CircularProgressIndicator(Modifier.weight(1f).wrapContentHeight()) }
            is StoreResult.Loaded -> {
                val alpha = if (images.backgroundLoadState == BackgroundLoadState.Loading) 0.7f else 1f
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.alpha(alpha).weight(1f),
                    contentPadding = PaddingValues(vertical = Dimens.SmallPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.MediumSpace),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpace),
                ) {
                    items(images.value) { image ->
                        GalleryCard(
                            image = image,
                            onToggleLike = { viewModel.toggleLike(image) },
                        )
                    }
                }
            }
            is StoreResult.Failed -> {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${images.exception.message}",
                )
                Button(
                    onClick = viewModel::tryAgain,
                    enabled = images.backgroundLoadState == BackgroundLoadState.Idle,
                ) {
                    Text("Try Again")
                }
                Spacer(Modifier.weight(1f))
            }
        }

    }
}

@Composable
private fun GalleryCard(
    image: GalleryImage,
    onToggleLike: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4 / 3f)
            .dropShadow(
                shape = ImageShape,
                shadow = Shadow(radius = 4.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ImageShape)
        ) {
            AsyncImage(
                model = image.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0x88000000))
                    .padding(vertical = Dimens.TinyPadding, horizontal = Dimens.SmallPadding)
            ) {
                Text(
                    text = image.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                if (image.isLocal) {
                    Text(
                        text = "cached",
                        color = Color.White,
                        fontSize = 12.sp,
                    )
                } else {
                    IconButton(
                        onClick = onToggleLike,
                        modifier = Modifier.size(Dimens.TinyIconSize)
                    ) {
                        Icon(
                            imageVector = if (image.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            tint = Color.White,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

private val ImageShape = RoundedCornerShape(Dimens.MediumCorners)
