package com.elveum.store.demo.feature.examples.store_paged.pagination_updates

import com.elveum.store.StoreFactory
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val dataSource: ProductDataSource,
) {

    private val store = StoreFactory.pagedStoreBuilder(0, Product::id)
        .build(
            onFetch = dataSource::fetchPage,
        )

    fun getProducts(): Flow<StoreResult<List<Product>>> = store.observe()

    suspend fun toggleLike(product: Product) {
        store.optimisticUpdate { oldList ->
            val newList = oldList.indexOfFirst { it.id == product.id }
                .takeIf { it != -1 }
                ?.let { index ->
                    oldList.toMutableList().apply {
                        set(index, product.copy(isLiked = !product.isLiked))
                    }
                }
                ?: oldList
            emit(newList)
            dataSource.toggleLike(product)
        }
    }

    fun onItemRendered(index: Int) {
        store.onItemRendered(index)
    }

    data class Product(
        val id: Int,
        val title: String,
        val description: String,
        val isLiked: Boolean,
    )

}
