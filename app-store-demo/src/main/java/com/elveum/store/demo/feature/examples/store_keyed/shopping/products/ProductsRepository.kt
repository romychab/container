package com.elveum.store.demo.feature.examples.store_keyed.shopping.products

import com.elveum.store.demo.feature.examples.store_keyed.shopping.data.ProductsDataSource
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.Product
import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductsRepository @Inject constructor(
    private val productsDataSource: ProductsDataSource,
) {

    private val store = StoreFactory.simpleStoreBuilder<List<Product>>()
        .build(
            onFetch = productsDataSource::fetchProducts,
        )

    fun getProducts(): Flow<StoreResult<List<Product>>> = store.observe()

}
