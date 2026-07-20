package com.elveum.store.paged

import com.elveum.container.ContainerMetadata
import com.elveum.container.get
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.paged.PagedList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the optional [ContainerMetadata] argument that can be attached to
 * invalidation requests of [com.elveum.store.stores.paged.PagedStore] and query
 * requests of [com.elveum.store.stores.paged.PagedQueryStore].
 */
class PagedStoreMetadataTest : AbstractPagedStoreTest() {

    @Test
    fun `GIVEN observed store WHEN invalidateAsync with metadata THEN metadata is attached to the emitted result`() = runFlowTest {
        val store = storeBuilder().build { pageKey -> page(pageKey, totalPages = 2) }
        val collector = store.observe().startCollecting()
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestMetadata>())

        store.invalidateAsync(TestMetadata("m1"))
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("item0", "item1")), collector.lastItem)
        assertEquals("m1", collector.lastItem.metadata.get<TestMetadata>()?.value)
    }

    @Test
    fun `GIVEN observed query store WHEN invalidateAsync with metadata THEN metadata is attached to the emitted result`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query, pageKey -> queryPage(query, pageKey, totalPages = 2) }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.invalidateAsync(TestMetadata("m1"))
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("q1-item0", "q1-item1")), collector.lastItem)
        assertEquals("m1", collector.lastItem.metadata.get<TestMetadata>()?.value)
    }

    @Test
    fun `GIVEN observed query store WHEN submitQueryAsync with metadata THEN metadata is attached to the emitted result`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query, pageKey -> queryPage(query, pageKey, totalPages = 2) }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.submitQueryAsync("q2", TestMetadata("m1"))
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("q2-item0", "q2-item1")), collector.lastItem)
        assertEquals("m1", collector.lastItem.metadata.get<TestMetadata>()?.value)
    }

    private fun queryPage(query: String, pageKey: Int, totalPages: Int): PagedList<Int, String> {
        val items = listOf("$query-item${pageKey * 2}", "$query-item${pageKey * 2 + 1}")
        val nextKey = (pageKey + 1).takeIf { it < totalPages }
        return PagedList(items, nextKey)
    }

    private data class TestMetadata(val value: String) : ContainerMetadata

}
