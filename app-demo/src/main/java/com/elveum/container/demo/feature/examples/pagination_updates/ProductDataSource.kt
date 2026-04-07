package com.elveum.container.demo.feature.examples.pagination_updates

import com.elveum.container.demo.feature.examples.pagination_updates.ProductRepository.PagedResult
import com.elveum.container.demo.feature.examples.pagination_updates.ProductRepository.Product
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject

class ProductDataSource @Inject constructor(
    private val faker: Faker,
) {

    private val allProducts = MutableList(100) {
        ProductImpl(
            id = it + 1,
            title = faker.commerce().productName(),
            description = faker.lorem().sentence(8, 4),
            isLiked = false,
        )
    }

    suspend fun fetchPage(pageKey: Int?): PagedResult {
        delay(2000)
        val page = pageKey ?: 0
        val start = page * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, allProducts.size)
        val products = allProducts.subList(start, end).toList()
        val nextKey = if (end < allProducts.size) page + 1 else null
        return PagedResult(products, nextKey)
    }

    suspend fun toggleLike(product: Product): Product {
        delay(1000)
        val index = allProducts.indexOfFirst { it.id == product.id }
        return if (index != -1) {
            val oldProduct = allProducts[index]
            val updatedProduct = oldProduct.copy(
                isLiked = !oldProduct.isLiked
            )
            allProducts[index] = updatedProduct
            updatedProduct
        } else {
            product
        }
    }

    private data class ProductImpl(
        override val id: Int,
        override val title: String,
        override val description: String,
        override val isLiked: Boolean
    ) : Product

    private companion object {
        const val PAGE_SIZE = 10
    }
}
