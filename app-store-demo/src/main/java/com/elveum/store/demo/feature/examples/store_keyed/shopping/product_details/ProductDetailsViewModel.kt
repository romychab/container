package com.elveum.store.demo.feature.examples.store_keyed.shopping.product_details

import com.elveum.store.demo.effects.Toaster
import com.elveum.store.demo.feature.examples.store_keyed.shopping.cart.CartRepository
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.ProductDetails
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.container.reducer.stateIn
import com.elveum.store.load.StoreResult
import com.elveum.store.load.combineStores
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel(assistedFactory = ProductDetailsViewModel.Factory::class)
class ProductDetailsViewModel @AssistedInject constructor(
    @Assisted private val productId: Long,
    productDetailsRepository: ProductDetailsRepository,
    private val cartRepository: CartRepository,
    private val toaster: Toaster,
) : AbstractViewModel() {

    val stateFlow: StateFlow<StoreResult<State>> = combineStores(
        productDetailsRepository.getProductById(productId),
        cartRepository.isInCart(productId),
        ::State,
    ).stateIn(StoreResult.Loading)

    fun removeFromCart() = safeLaunch(toaster) {
        cartRepository.removeFromCart(productId)
    }

    @AssistedFactory
    interface Factory {
        fun create(catId: Long): ProductDetailsViewModel
    }

    data class State(
        val productDetails: ProductDetails,
        val isInCart: Boolean,
    )

}