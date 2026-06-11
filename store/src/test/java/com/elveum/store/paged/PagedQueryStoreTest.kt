package com.elveum.store.paged

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.paged.PagedList
import org.junit.Assert.assertEquals
import org.junit.Test

class PagedQueryStoreTest : AbstractPagedStoreTest() {

    private fun queryPage(query: String, pageKey: Int, totalPages: Int): PagedList<Int, String> {
        val items = listOf("$query-item${pageKey * 2}", "$query-item${pageKey * 2 + 1}")
        val nextKey = (pageKey + 1).takeIf { it < totalPages }
        return PagedList(items, nextKey)
    }

    @Test
    fun `GIVEN paged store with query WHEN observe THEN fetcher is called with initial query`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query, pageKey -> queryPage(query, pageKey, totalPages = 2) }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("q1-item0", "q1-item1")), collector.lastItem)
        assertEquals("q1", store.queryFlow.value)
    }

    @Test
    fun `GIVEN two loaded pages WHEN submit new query THEN pagination is reset and re-fetched`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query, pageKey -> queryPage(query, pageKey, totalPages = 2) }
        val collector = store.observe().startCollecting()
        runCurrent()
        store.onItemRendered(1) // load the second page
        runCurrent()
        assertResult(
            StoreResult.Loaded(listOf("q1-item0", "q1-item1", "q1-item2", "q1-item3")),
            collector.lastItem,
        )

        store.submitQueryAsync("q2", LoadRequest.Default)
        runCurrent()

        // only the first page of the new query is loaded
        assertResult(StoreResult.Loaded(listOf("q2-item0", "q2-item1")), collector.lastItem)
        assertEquals("q2", store.queryFlow.value)
    }

    @Test
    fun `GIVEN submitted query WHEN next pages are rendered THEN pages of the new query are loaded`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query, pageKey -> queryPage(query, pageKey, totalPages = 2) }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.submitQueryAsync("q2", LoadRequest.Default)
        runCurrent()
        store.onItemRendered(1) // the last item of the first page
        runCurrent()

        assertResult(
            StoreResult.Loaded(listOf("q2-item0", "q2-item1", "q2-item2", "q2-item3")),
            collector.lastItem,
        )
    }

}
