package com.elveum.store.demo.feature.examples.store_simple.pull_to_refresh

import com.elveum.store.StoreFactory
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatsRepository @Inject constructor(
    private val catsDataSource: CatsDataSource,
) {

    private val store = StoreFactory.simpleStoreBuilder<List<Cat>>()
        .build(
            onFetch = catsDataSource::fetchCats
        )

    fun getCats(): Flow<StoreResult<List<Cat>>> = store.observe()

    fun refresh() {
        store.invalidateAsync(LoadRequest.Silent)
    }

    data class Cat(
        val id: Long,
        val name: String,
        val imageUrl: String,
        val description: String,
    )
}

