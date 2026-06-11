package com.elveum.store.demo.feature.examples.store_keyed.shopping.data

import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.CartItem
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.Product
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartDataSource @Inject constructor() {

    private val cart = mutableListOf<CartItem>()

    suspend fun fetchCart(): List<CartItem> {
        delay(2000)
        return cart.toList()
    }

    suspend fun addToCart(product: Product) {
        delay(500)
        val existing = cart.firstOrNull { it.productId == product.id }
        if (existing == null) {
            cart += CartItem(product.id, quantity = 1)
        } else {
            replaceQuantity(product.id, existing.quantity + 1)
        }
    }

    suspend fun increaseQuantity(productId: Long) {
        delay(500)
        val item = cart.firstOrNull { it.productId == productId } ?: return
        replaceQuantity(productId, item.quantity + 1)
    }

    suspend fun decreaseQuantity(productId: Long) {
        delay(500)
        val item = cart.firstOrNull { it.productId == productId } ?: return
        if (item.quantity <= 1) {
            cart.removeAll { it.productId == productId }
        } else {
            replaceQuantity(productId, item.quantity - 1)
        }
    }

    suspend fun removeFromCart(productId: Long) {
        delay(500)
        cart.removeAll { it.productId == productId }
    }

    suspend fun clear() {
        delay(500)
        cart.clear()
    }

    private fun replaceQuantity(productId: Long, quantity: Int) {
        cart.indexOfFirst { it.productId == productId }
            .takeIf { it != -1 }
            ?.let { index ->
                cart[index] = cart[index].copy(quantity = quantity)
            }
    }
}