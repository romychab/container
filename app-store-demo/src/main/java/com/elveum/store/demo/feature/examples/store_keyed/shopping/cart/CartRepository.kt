package com.elveum.store.demo.feature.examples.store_keyed.shopping.cart

import com.elveum.store.demo.feature.examples.store_keyed.shopping.data.CartDataSource
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.CartItem
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.CartProductItem
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.Product
import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import com.elveum.store.load.getOrNull
import com.elveum.store.load.storeListFlatMapLatest
import com.elveum.store.load.storeMap
import com.elveum.store.stores.base.update
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client-side cart shared between the products grid and the cart screen.
 * Both screens observe the same [items] flow, so any change made on one screen
 * is reflected on the other immediately.
 */
@Singleton
class CartRepository @Inject constructor(
    private val cartDataSource: CartDataSource,
    private val productDetailsObserver: ProductDetailsObserver,
) {

    private val store = StoreFactory.simpleStoreBuilder<List<CartItem>>()
        .build(
            onFetch = cartDataSource::fetchCart,
        )

    fun getMinimalCart(): Flow<StoreResult<List<CartItem>>> {
        return store.observe()
    }

    fun isInCart(productId: Long): Flow<StoreResult<Boolean>> {
        return getMinimalCart().storeMap { cart ->
            cart.any { it.productId == productId }
        }
    }

    fun getFullCart(): Flow<StoreResult<List<CartProductItem>>> {
        return store
            .observe()
            .storeListFlatMapLatest(
                observer = { cartItem -> productDetailsObserver.observeProduct(cartItem.productId) },
                mapper = { cartItem, product ->
                    CartProductItem(cartItem.productId, product.getOrNull(), cartItem.quantity)
                }
            )
    }

    suspend fun addToCart(product: Product) {
        store.optimisticUpdate { cart ->
            val existingProduct = cart.firstOrNull { it.productId == product.id }
            if (existingProduct != null) {
                emit(cart.updateQuantity(product.id, +1))
            } else {
                emit(cart + CartItem(product.id, 1))
            }
            cartDataSource.addToCart(product)
        }
    }

    suspend fun increaseQuantity(productId: Long) {
        store.optimisticUpdate { cart ->
            emit(cart.updateQuantity(productId, +1))
            cartDataSource.increaseQuantity(productId)
        }
    }

    suspend fun decreaseQuantity(productId: Long) {
        store.optimisticUpdate { cart ->
            val item = cart.firstOrNull { it.productId == productId }
            if (item?.quantity == 1) {
                emit(cart.filter { it.productId != productId })
            } else {
                emit(cart.updateQuantity(productId, -1))
            }
            cartDataSource.decreaseQuantity(productId)
        }
    }

    suspend fun removeFromCart(productId: Long) {
        store.optimisticUpdate { cart ->
            emit(cart.filter { it.productId != productId })
            cartDataSource.removeFromCart(productId)
        }
    }

    suspend fun clear() {
        cartDataSource.clear()
        store.update { emptyList() }
    }

    private fun List<CartItem>.updateQuantity(
        productId: Long,
        quantityChange: Int,
    ): List<CartItem> {
        val index = indexOfFirst { it.productId == productId }
        return if (index != -1) {
            toMutableList().apply {
                val oldItem = get(index)
                set(index, oldItem.copy(quantity = oldItem.quantity + quantityChange))
            }
        } else {
            this
        }
    }

    fun tryAgain() {
        store.invalidateAsync()
    }

    interface ProductDetailsObserver {
        fun observeProduct(productId: Long): Flow<StoreResult<Product>>
    }
}
