package com.elveum.container.demo.feature.examples.subject_errors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elveum.container.BackgroundLoadState
import com.elveum.container.Container
import com.elveum.container.LoadConfig
import com.elveum.container.demo.feature.examples.subject_errors.GalleryRepository.GalleryImage
import com.elveum.container.demo.ui.components.DemoAction
import com.elveum.container.demo.ui.components.DemoActionState
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.theme.Dimens
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ErrorHandlingScreen() {
    val viewModel: ErrorHandlingViewModel = hiltViewModel()
    val container by viewModel.stateFlow.collectAsState()
    val isRefreshInProgress = container.backgroundLoadState == BackgroundLoadState.Loading ||
            container is Container.Pending
    val actions = persistentListOf(
        DemoAction(
            icon = Icons.Default.Refresh,
            state = if (isRefreshInProgress) DemoActionState.Loading else DemoActionState.Default,
            onClick = {
                container.reload(LoadConfig.SilentLoading)
            }
        )
    )

    DemoScaffold(
        title = "Error Handling",
        scrollable = false,
        actions = actions,
        description = """
            When loading fails, the Container enters the **Error** state. Calling **reload()** on
            any completed Container restarts the loader. 
            
            This example places the reload trigger in the top action bar. The action is enabled
            only when **backgroundLoadState** is **Idle**, preventing concurrent reload attempts.
            
            Every second load in this example fails with an error.
        """.trimIndent()
    ) {

        container.fold(
            onPending = { CircularProgressIndicator() },
            onError = { exception ->
                Text(
                    text = "${exception.message}",
                )
                Button(
                    onClick = ::reload,
                    enabled = backgroundLoadState == BackgroundLoadState.Idle,
                ) {
                    Text("Try Again")
                }
            },
            onSuccess = { state ->
                val alpha = if (backgroundLoadState == BackgroundLoadState.Loading) 0.7f else 1f
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.alpha(alpha),
                    contentPadding = PaddingValues(vertical = Dimens.SmallPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.MediumSpace),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpace),
                ) {
                    items(state.images) { image ->
                        GalleryCard(image)
                    }
                }
            },
        )
    }
}

@Composable
private fun GalleryCard(image: GalleryImage) {
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
            Text(
                text = image.name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0x88000000))
                    .padding(Dimens.TinyPadding)
            )
        }
    }
}

private val ImageShape = RoundedCornerShape(Dimens.MediumCorners)
