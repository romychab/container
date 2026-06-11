package com.elveum.store.demo.feature.examples.store_keyed.shopping.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elveum.store.demo.feature.examples.store_keyed.shopping.cart.CartRoute
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.formatPrice
import com.elveum.store.demo.feature.examples.store_keyed.shopping.product_details.ProductDetailsRoute
import com.elveum.store.demo.feature.examples.store_keyed.shopping.products.ProductsViewModel.ProductListItem
import com.elveum.store.demo.navigation.LocalNavController
import com.elveum.store.demo.ui.components.DemoAction
import com.elveum.store.demo.ui.components.DemoScaffold
import com.elveum.store.demo.ui.theme.Dimens
import com.elveum.store.load.StoreResult
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ProductsScreen() {
    val viewModel = hiltViewModel<ProductsViewModel>()
    val result by viewModel.stateFlow.collectAsState()
    val navController = LocalNavController.current

    DemoScaffold(
        title = "Products",
        scrollable = false,
        description = """
            A product catalog backed by a **SimpleStore** plus product details backed
            by a **KeyedStore** and an in-memory cart shared between screens.
        """.trimIndent(),
        actions = persistentListOf(
            DemoAction(
                icon = Icons.Default.ShoppingCart,
                onClick = { navController.launch(CartRoute) },
            )
        ),
    ) {
        when (val finalResult = result) {
            StoreResult.Loading -> CircularProgressIndicator()
            is StoreResult.Loaded -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = Dimens.SmallPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.MediumSpace),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpace),
                ) {
                    items(finalResult.value.items, key = { it.product.id }) { item ->
                        ProductCard(
                            item = item,
                            onAddToCart = { viewModel.addToCart(item.product) },
                            onLaunchDetails = {
                                val route = ProductDetailsRoute(item.product.id, isFromCart = false)
                                navController.launch(route)
                            }
                        )
                    }
                }
            }
            is StoreResult.Failed -> {}
        }
    }
}

@Composable
private fun ProductCard(
    item: ProductListItem,
    onAddToCart: () -> Unit,
    onLaunchDetails: () -> Unit,
) {
    ElevatedCard(
        onClick = onLaunchDetails,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SmallPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.TinySpace),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = item.product.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(Dimens.MediumCorners)),
                )
                if (item.quantityInCart > 0) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Dimens.SmallPadding),
                    ) {
                        Text("${item.quantityInCart}")
                    }
                }
            }
            Text(
                text = item.product.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatPrice(item.product.price),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                FilledIconButton(
                    onClick = onAddToCart,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add to cart")
                }
            }
        }
    }
}
