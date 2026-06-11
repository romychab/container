package com.elveum.store.demo.feature.examples.store_keyed.master_details.list

import com.elveum.store.demo.feature.examples.store_keyed.master_details.data.CatsDataSource
import com.elveum.store.demo.feature.examples.store_keyed.master_details.model.Cat
import com.elveum.store.StoreFactory
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.update
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatsRepository @Inject constructor(
    private val catsDataSource: CatsDataSource,
    private val catEvents: CatEvents,
) {

    private val store = StoreFactory.simpleStoreBuilder<List<Cat>>()
        .build(
            onFetch = catsDataSource::fetchCats
        )
        .whenActive {
            catEvents.observeCatEvents().collect { event ->
                val updatedCat = event.cat
                update { oldCatList ->
                    oldCatList.map { if (it.id == updatedCat.id) updatedCat else it }
                }
            }
        }

    fun getCats(): Flow<StoreResult<List<Cat>>> = store.observe()

    fun tryAgain() {
        store.invalidateAsync()
    }

    fun refresh() {
        store.invalidateAsync(LoadRequest.Silent)
    }

}

