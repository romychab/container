package com.elveum.store.keyed

import com.elveum.store.StoreFactory
import com.elveum.store.base.AbstractStoreTest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.keyed.PagedKeyedQueryStore
import com.elveum.store.stores.paged.PagedList
import org.junit.Assert.assertEquals
import org.junit.Test

class PagedKeyedQueryStoreTest : AbstractStoreTest() {

    private fun pagedKeyedQueryStore(): PagedKeyedQueryStore<String, String, String> = StoreFactory
        .pagedStoreBuilder<Int, String>(initialKey = 0, itemId = { it })
        .setCoroutineScopeFactory(createStoreScopeFactory())
        .setFetchDistance(1)
        .withQuery(initialQuery = "q1")
        .withKeys<String>()
        .build { key, query, pageKey -> keyedQueryPage(key, query, pageKey, totalPages = 2) }

    private fun keyedQueryPage(
        key: String,
        query: String,
        pageKey: Int,
        totalPages: Int,
    ): PagedList<Int, String> {
        val items = listOf(
            "$key-$query-item${pageKey * 2}",
            "$key-$query-item${pageKey * 2 + 1}",
        )
        val nextKey = (pageKey + 1).takeIf { it < totalPages }
        return PagedList(items, nextKey)
    }

    @Test
    fun `GIVEN paged keyed query store WHEN observe a key THEN the first page is loaded for the initial query`() = runFlowTest {
        val store = pagedKeyedQueryStore()

        val collector = store.observe("k1").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("k1-q1-item0", "k1-q1-item1")), collector.lastItem)
        assertEquals("q1", store.observeQueryFlow("k1").value)
    }

    @Test
    fun `GIVEN paged keyed query store WHEN onItemRendered near the end THEN next page is loaded for that key only`() = runFlowTest {
        val store = pagedKeyedQueryStore()
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()

        store.onItemRendered("k1", 1) // render the last item of the first page
        runCurrent()

        assertResult(
            StoreResult.Loaded(listOf("k1-q1-item0", "k1-q1-item1", "k1-q1-item2", "k1-q1-item3")),
            collector1.lastItem,
        )
        // the other key keeps its own independent pagination
        assertResult(StoreResult.Loaded(listOf("k2-q1-item0", "k2-q1-item1")), collector2.lastItem)
    }

    @Test
    fun `GIVEN paginated key WHEN submitQuery for that key THEN its pagination resets and only that key is re-fetched`() = runFlowTest {
        val store = pagedKeyedQueryStore()
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()

        // advance k1 to its second page
        store.onItemRendered("k1", 1)
        runCurrent()
        assertResult(
            StoreResult.Loaded(listOf("k1-q1-item0", "k1-q1-item1", "k1-q1-item2", "k1-q1-item3")),
            collector1.lastItem,
        )

        // submitting a new query for k1 must reset its pagination back to the first page.
        // NOTE: the fire-and-forget submitQueryAsync is used on purpose. The suspending
        // submitQuery/invalidate variants do not return for a paged store until *all* pages
        // are exhausted, so calling them here on the test coroutine would hang until
        // runTest's wall-clock timeout.
        store.submitQueryAsync("k1", "q2")
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("k1-q2-item0", "k1-q2-item1")), collector1.lastItem)
        assertEquals("q2", store.observeQueryFlow("k1").value)
        // the other key is untouched: same query, same accumulated pages
        assertResult(StoreResult.Loaded(listOf("k2-q1-item0", "k2-q1-item1")), collector2.lastItem)
        assertEquals("q1", store.observeQueryFlow("k2").value)
    }
}
