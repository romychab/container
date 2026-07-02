package com.elveum.store.keyed

import com.elveum.store.StoreFactory
import com.elveum.store.base.AbstractStoreTest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.keyed.PagedKeyedStore
import com.elveum.store.stores.paged.PagedList
import org.junit.Test

class PagedKeyedStoreTest : AbstractStoreTest() {

    private fun pagedKeyedStore(): PagedKeyedStore<String, String> = StoreFactory
        .pagedStoreBuilder<Int, String>(initialKey = 0, itemId = { it })
        .setCoroutineScopeFactory(createStoreScopeFactory())
        .setFetchDistance(1)
        .withKeys<String>()
        .build { key, pageKey -> keyedPage(key, pageKey, totalPages = 2) }

    private fun keyedPage(key: String, pageKey: Int, totalPages: Int): PagedList<Int, String> {
        val items = listOf("$key-item${pageKey * 2}", "$key-item${pageKey * 2 + 1}")
        val nextKey = (pageKey + 1).takeIf { it < totalPages }
        return PagedList(items, nextKey)
    }

    @Test
    fun `GIVEN paged keyed store WHEN observe a key THEN the first page is loaded for that key`() = runFlowTest {
        val store = pagedKeyedStore()

        val collector = store.observe("k1").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("k1-item0", "k1-item1")), collector.lastItem)
    }

    @Test
    fun `GIVEN paged keyed store WHEN onItemRendered near the end THEN next page is loaded for that key only`() = runFlowTest {
        val store = pagedKeyedStore()
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()

        store.onItemRendered("k1", 1) // render the last item of the first page
        runCurrent()

        assertResult(
            StoreResult.Loaded(listOf("k1-item0", "k1-item1", "k1-item2", "k1-item3")),
            collector1.lastItem,
        )
        // the other key keeps its own independent pagination
        assertResult(StoreResult.Loaded(listOf("k2-item0", "k2-item1")), collector2.lastItem)
    }
}
