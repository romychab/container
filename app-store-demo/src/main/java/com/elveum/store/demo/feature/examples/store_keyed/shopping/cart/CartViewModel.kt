package com.elveum.store.demo.feature.examples.store_keyed.shopping.cart

import com.elveum.store.demo.effects.Toaster
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.CartProductItem
import com.elveum.store.demo.ui.AbstractViewModel
import com.elveum.store.reducers.StoreResultReducer
import com.elveum.store.reducers.storeResultToReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val toaster: Toaster,
) : AbstractViewModel() {

    private val reducer: StoreResultReducer<State> = cartRepository.getFullCart()
        .storeResultToReducer(
            initialState = ::State,
            nextState = State::copy,
        )

    val stateFlow = reducer.stateFlow

    fun increaseQuantity(productId: Long) = safeLaunch(toaster) {
        cartRepository.increaseQuantity(productId)
    }

    fun decreaseQuantity(productId: Long) = safeLaunch(toaster) {
        cartRepository.decreaseQuantity(productId)
    }

    fun removeFromCart(productId: Long) = safeLaunch(toaster) {
        cartRepository.removeFromCart(productId)
    }

    fun clear() = safeLaunch(toaster) {
        try {
            reducer.updateState { it.copy(isClearInProgress = true) }
            cartRepository.clear()
        } finally {
            reducer.updateState { it.copy(isClearInProgress = false) }
        }
    }

    data class State(
        val items: List<CartProductItem>,
        val isClearInProgress: Boolean = false,
    ) {
        val totalPrice: Double get() = items.sumOf { it.totalPrice }
        val isEmpty: Boolean get() = items.isEmpty()
    }
}
