package com.elveum.store.demo.feature.examples.store_keyed.shopping.product_details

import com.elveum.store.demo.feature.examples.store_keyed.shopping.cart.CartRepository
import com.elveum.store.demo.feature.examples.store_keyed.shopping.data.ProductsDataSource
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.Product
import com.elveum.store.demo.feature.examples.store_keyed.shopping.model.ProductDetails
import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import com.elveum.store.load.storeMap
import com.uandcode.hilt.autobind.AutoBinds
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
@AutoBinds
class ProductDetailsRepository @Inject constructor(
    private val productsDataSource: ProductsDataSource,
) : CartRepository.ProductDetailsObserver {

    private val productStore = StoreFactory.simpleStoreBuilder<ProductDetails>()
        .withKeys<Long>()
        .setInMemoryCacheTimeout(10.seconds)
        .build(onFetch = productsDataSource::fetchProductById)

    fun getProductById(id: Long): Flow<StoreResult<ProductDetails>> =
        productStore.observe(id)

    override fun observeProduct(productId: Long): Flow<StoreResult<Product>> {
        return getProductById(productId).storeMap { it.product }
    }

}
