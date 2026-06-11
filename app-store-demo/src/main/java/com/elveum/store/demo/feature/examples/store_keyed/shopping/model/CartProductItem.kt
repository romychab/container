package com.elveum.store.demo.feature.examples.store_keyed.shopping.model

data class CartProductItem(
    val productId: Long,
    val product: Product?,
    val quantity: Int,
) {
    val totalPrice = (product?.price ?: 0.0) * quantity
}
