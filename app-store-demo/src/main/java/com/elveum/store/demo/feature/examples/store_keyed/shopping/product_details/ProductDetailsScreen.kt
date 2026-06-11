package com.elveum.store.demo.feature.examples.store_keyed.shopping.product_details

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import com.elveum.store.demo.feature.examples.store_keyed.shopping.cart.CartRoute
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.formatPrice
import com.elveum.store.demo.navigation.LocalNavController
import com.elveum.store.demo.navigation.WithContent
import com.elveum.store.demo.ui.components.DemoAction
import com.elveum.store.demo.ui.components.DemoScaffold
import com.elveum.store.demo.ui.components.Heading
import com.elveum.store.demo.ui.theme.Dimens
import com.elveum.store.load.StoreResult
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
data class ProductDetailsRoute(
    val productId: Long,
    val isFromCart: Boolean,
) : NavKey, WithContent {

    @Composable
    override fun Content() = ProductDetailsScreen(productId, isFromCart)
}

@Composable
fun ProductDetailsScreen(productId: Long, isFromCart: Boolean) {
    val viewModel = hiltViewModel<ProductDetailsViewModel, ProductDetailsViewModel.Factory> {
        it.create(productId)
    }
    val result by viewModel.stateFlow.collectAsState()
    val navController = LocalNavController.current

    DemoScaffold(
        title = "Product Details",
        scrollable = true,
        actions = if (isFromCart) {
            persistentListOf()
        } else {
            persistentListOf(
                DemoAction(
                    icon = Icons.Default.ShoppingCart,
                    onClick = { navController.launch(CartRoute) }
                )
            )
        }
    ) {
        when (val finalResult = result) {
            StoreResult.Loading -> CircularProgressIndicator()
            is StoreResult.Loaded -> {
                val state = finalResult.value
                val product = state.productDetails

                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(Dimens.MediumCorners)),
                )

                Heading("Product Name")
                Text(product.name)

                Heading("Product Price")
                Text(formatPrice(product.price))

                if (state.isInCart) {
                    Heading("Cart Status")
                    Text("The product has been added to cart.")
                    Button(
                        onClick = viewModel::removeFromCart,
                    ) {
                        Text("Remove")
                    }
                }

                Heading("Description")
                Text(product.description)

            }
            is StoreResult.Failed -> {}
        }

    }
}