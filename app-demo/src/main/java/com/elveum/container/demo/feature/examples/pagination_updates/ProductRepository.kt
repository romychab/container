package com.elveum.container.demo.feature.examples.pagination_updates

import com.elveum.container.Container
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.listenReloadable
import com.elveum.container.subject.paging.pageLoader
import com.elveum.container.subject.updateIfSuccess
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val dataSource: ProductDataSource,
) {

    private val subject = LazyFlowSubject.create(
        valueLoader = pageLoader<Int?, Product>(
            initialKey = null,
            itemId = Product::id,
        ) { pageKey ->
            val result = dataSource.fetchPage(pageKey)
            emitPage(result.products)
            if (result.nextPageKey != null) emitNextKey(result.nextPageKey)
        }
    )

    fun getProducts(): Flow<Container<List<Product>>> = subject.listenReloadable()

    suspend fun toggleLike(product: Product) {
        val updatedProduct = dataSource.toggleLike(product)
        subject.updateIfSuccess { list ->
            list.indexOfFirst { it.id == product.id }
                .takeIf { it != -1 }
                ?.let { index ->
                    list.toMutableList().apply {
                        set(index, updatedProduct)
                    }
                }
                ?: list
        }
    }

    interface Product {
        val id: Int
        val title: String
        val description: String
        val isLiked: Boolean
    }

    data class PagedResult(
        val products: List<Product>,
        val nextPageKey: Int?,
    )
}
