package com.elveum.store.demo.feature.examples.store_keyed.master_details.details

import com.elveum.store.StoreFactory
import com.elveum.store.demo.feature.examples.store_keyed.master_details.data.CatsDataSource
import com.elveum.store.demo.feature.examples.store_keyed.master_details.list.CatEvents
import com.elveum.store.demo.feature.examples.store_keyed.master_details.list.CatEvents.CatUpdatedEvent
import com.elveum.store.demo.feature.examples.store_keyed.master_details.model.CatDetails
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.keyed.updateIfSuccess
import com.uandcode.hilt.autobind.AutoBinds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@AutoBinds
class CatDetailsRepository @Inject constructor(
    private val catsDataSource: CatsDataSource,
) : CatEvents {

    private val store = StoreFactory.simpleStoreBuilder<CatDetails>()
        .withKeys<Long>()
        .build(onFetch = catsDataSource::fetchCatDetails)

    private val catEvents = MutableSharedFlow<CatUpdatedEvent>()

    fun getCat(id: Long): Flow<StoreResult<CatDetails>> {
        return store.observe(id)
    }

    suspend fun updateCatName(id: Long, name: String, isNonOptimisticUpdate: Boolean) {
        if (isNonOptimisticUpdate) {
            nonOptimisticUpdate(id, name)
        } else {
            optimisticUpdate(id, name)
        }
    }

    override fun observeCatEvents(): Flow<CatUpdatedEvent> {
        return catEvents
    }

    fun invalidate(catId: Long) {
        store.invalidateAsync(catId)
    }

    private suspend fun optimisticUpdate(id: Long, name: String) {
        store.optimisticUpdate(id) { old ->
            val entity = old.copy(cat = old.cat.copy(name = name))
            emit(entity)
            catsDataSource.updateCatName(id, name)
            catEvents.emit(CatUpdatedEvent(entity.cat))
        }
    }

    private suspend fun nonOptimisticUpdate(id: Long, name: String) {
        catsDataSource.updateCatName(id, name)
        store.updateIfSuccess(id) { old ->
            old.copy(cat = old.cat.copy(name = name))
                .also { catEvents.emit(CatUpdatedEvent(it.cat)) }
        }
    }

}
