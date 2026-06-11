package com.elveum.store.demo.feature.examples.store_keyed.master_details.list

import com.elveum.store.demo.feature.examples.store_keyed.master_details.model.Cat
import kotlinx.coroutines.flow.Flow

interface CatEvents {
    fun observeCatEvents(): Flow<CatUpdatedEvent>

    data class CatUpdatedEvent(val cat: Cat)
}
