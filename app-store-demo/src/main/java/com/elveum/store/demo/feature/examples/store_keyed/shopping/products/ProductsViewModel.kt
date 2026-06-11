package com.elveum.store.demo.feature.examples.store_keyed.shopping.products

import com.elveum.store.demo.effects.Toaster
import com.elveum.store.demo.feature.examples.store_keyed.shopping.cart.CartRepository
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.Product
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.stateIn
import com.elveum.store.load.StoreResult
import com.elveum.store.load.combineStores
import com.elveum.store.reducers.StoreResultReducer
import com.elveum.store.reducers.storeResultToReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    productsRepository: ProductsRepository,
    private val cartRepository: CartRepository,
    private val toaster: Toaster,
) : AbstractViewModel() {

    val stateFlow = combineStores(
        productsRepository.getProducts(),
        cartRepository.getMinimalCart(),
    ) { products, cartItems ->
        val combinedProducts = products.map { product ->
            val quantity = cartItems
                .firstOrNull { it.productId == product.id }
                ?.quantity ?: 0
            ProductListItem(product, quantity)
        }
        State(combinedProducts)
    }.stateIn(StoreResult.Loading)

    fun addToCart(product: Product) = safeLaunch(toaster) {
        cartRepository.addToCart(product)
    }

    data class State(
        val items: List<ProductListItem>,
    )

    data class ProductListItem(
        val product: Product,
        val quantityInCart: Int,
    )
}
