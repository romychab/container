package com.elveum.store.demo.feature.examples.store_keyed.basic

import com.elveum.store.demo.feature.examples.store_keyed.basic.data.BasicItemsDataSource
import com.elveum.store.demo.feature.examples.store_keyed.basic.model.Item
import com.elveum.store.demo.feature.examples.store_keyed.basic.model.ListItem
import com.elveum.store.StoreFactory
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.load.getOrNull
import com.elveum.store.load.storeListFlatMapLatest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Combines two stores into a single ready-to-use flow:
 *
 * - a [SimpleStore][StoreFactory.simpleStoreBuilder] holds the whole list with
 *   the basic [Item] data;
 * - a [KeyedStore][StoreFactory.keyedStoreBuilder] holds the description of each
 *   item keyed by its ID.
 *
 * [getItems] returns merged [ListItem]s: items appear as soon as the list is
 * loaded (with `description == null`), and each description fills in
 * independently as its own KeyedStore entry finishes loading.
 */
@Singleton
class BasicItemsRepository @Inject constructor(
    private val dataSource: BasicItemsDataSource,
) {

    private val listStore = StoreFactory.simpleStoreBuilder<List<Item>>()
        .build(
            onFetch = dataSource::fetchItems,
        )

    private val descriptionStore = StoreFactory.keyedStoreBuilder<Long, String>()
        .build(
            onFetch = dataSource::fetchDescription,
        )

    fun getItems(): Flow<StoreResult<List<ListItem>>> {
        return listStore
            .observe()
            .storeListFlatMapLatest(
                observer = { item ->
                    descriptionStore.observe(item.id)
                },
                mapper = { item, descriptionResult ->
                    ListItem(item, descriptionResult.getOrNull())
                }
            )
    }

    fun refresh() {
        listStore.invalidateAsync(LoadRequest.Silent)
    }

    fun tryAgain() {
        listStore.invalidateAsync(LoadRequest.Default)
    }

}
