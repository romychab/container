package com.elveum.store.demo.feature.examples.store_paged.pagination_updates

import com.elveum.store.demo.feature.examples.store_paged.pagination_updates.ProductRepository.Product
import com.elveum.store.stores.paged.PagedList
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject

class ProductDataSource @Inject constructor(
    private val faker: Faker,
) {

    private val allProducts = MutableList(100) {
        Product(
            id = it + 1,
            title = faker.commerce().productName(),
            description = faker.lorem().sentence(8, 4),
            isLiked = false,
        )
    }

    suspend fun fetchPage(pageKey: Int): PagedList<Int, Product> {
        delay(2000)
        val start = pageKey * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, allProducts.size)
        val products = allProducts.subList(start, end).toList()
        val nextKey = if (end < allProducts.size) pageKey + 1 else null
        return PagedList(products, nextKey)
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

    private companion object {
        const val PAGE_SIZE = 10
    }
}
