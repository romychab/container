package com.elveum.store.demo.feature.examples.store_keyed.shopping.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.CartProductItem
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.formatPrice
import com.elveum.store.demo.feature.examples.store_keyed.shopping.product_details.ProductDetailsRoute
import com.elveum.store.demo.navigation.LocalNavController
import com.elveum.store.demo.navigation.WithContent
import com.elveum.store.demo.ui.components.DemoAction
import com.elveum.store.demo.ui.components.DemoActionState
import com.elveum.store.demo.ui.components.DemoScaffold
import com.elveum.store.demo.ui.theme.Dimens
import com.elveum.store.load.StoreResult
import com.elveum.store.load.getOrNull
import com.elveum.store.load.invalidate
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable

@Serializable
data object CartRoute : NavKey, WithContent {
    @Composable
    override fun Content() = CartScreen()
}

@Composable
fun CartScreen() {
    val viewModel = hiltViewModel<CartViewModel>()
    val result by viewModel.stateFlow.collectAsState()

    val actions = mutableListOf<DemoAction>()
    val hasItemsInCart = result.getOrNull()?.items?.isNotEmpty() == true
    val isClearInProgress = result.getOrNull()?.isClearInProgress == true
    if (hasItemsInCart) {
        actions += DemoAction(
            icon = Icons.Default.Delete,
            onClick = viewModel::clear,
            state = if (isClearInProgress) DemoActionState.Loading else DemoActionState.Default
        )
    }
    val navController = LocalNavController.current
    DemoScaffold(
        title = "Cart",
        scrollable = false,
        actions = actions.toImmutableList(),
    ) {
        when (val finalResult = result) {
            StoreResult.Loading -> CircularProgressIndicator()
            is StoreResult.Loaded -> {
                val state = finalResult.value
                if (state.isEmpty) {
                    Text("Your cart is empty")
                    return@DemoScaffold
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
                    contentPadding = PaddingValues(vertical = Dimens.SmallPadding)
                ) {
                    items(state.items, key = { it.productId }) { item ->
                        CartRow(
                            item = item,
                            onIncrease = { viewModel.increaseQuantity(item.productId) },
                            onDecrease = { viewModel.decreaseQuantity(item.productId) },
                            onRemove = { viewModel.removeFromCart(item.productId) },
                            onLaunchDetails = {
                                val route = ProductDetailsRoute(item.productId, isFromCart = true)
                                navController.launch(route)
                            }
                        )
                    }
                }

                HorizontalDivider()
                Text(
                    text = "Total: ${formatPrice(state.totalPrice)}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = Dimens.SmallPadding),
                )
            }
            is StoreResult.Failed -> {
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
                    text = finalResult.exception.message ?: "Unknown error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = finalResult::invalidate) {
                    Text("Try Again")
                }
            }
        }
    }
}

@Composable
private fun CartRow(
    item: CartProductItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onLaunchDetails: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.SmallPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
            ) {
                if (item.product != null) {
                    AsyncImage(
                        model = item.product.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(Dimens.SmallPadding))
                            .clickable { onLaunchDetails() },
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.LightGray)
                            .clip(RoundedCornerShape(Dimens.SmallPadding)),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.product?.name ?: "ID #${item.productId}",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${formatPrice(item.product?.price ?: 0.0)} each",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpace),
            ) {
                IconButton(onClick = onDecrease) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                }
                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onIncrease) {
                    Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                }
                Text(
                    text = formatPrice(item.totalPrice),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
