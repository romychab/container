package com.elveum.store.demo.feature.examples.store_keyed.shopping.model

data class ProductDetails(
    val product: Product,
    val description: String,
) {
    val id: Long = product.id
    val name: String = product.name
    val price: Double = product.price
    val imageUrl: String = product.imageUrl
}
