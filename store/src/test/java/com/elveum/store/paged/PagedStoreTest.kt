package com.elveum.store.paged

import com.elveum.container.subject.paging.PageState
import com.elveum.container.subject.paging.TotalPagedItemsCountMetadata
import com.elveum.container.subject.paging.totalPagedItemsCount
import com.elveum.store.load.StoreResult
import com.elveum.store.load.nextPageState
import com.elveum.store.load.onItemRendered
import com.elveum.store.stores.paged.PagedList
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Test

class PagedStoreTest : AbstractPagedStoreTest() {

    @Test
    fun `GIVEN store WHEN observe THEN emit loading and then the first page`() = runFlowTest {
        val store = storeBuilder().build { pageKey -> page(pageKey, totalPages = 2) }

        val collector = store.observe().startCollecting()

        // the initial state is Loading
        assertEquals(StoreResult.Loading, collector.lastItem)
        // the next state contains items of the first page only
        runCurrent()
        assertResult(StoreResult.Loaded(listOf("item0", "item1")), collector.lastItem)
    }

    @Test
    fun `GIVEN loaded first page WHEN last item is rendered THEN next page is loaded and merged`() = runFlowTest {
        val store = storeBuilder().build { pageKey -> page(pageKey, totalPages = 2) }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.onItemRendered(1) // the last item of the first page
        runCurrent()

        assertResult(
            StoreResult.Loaded(listOf("item0", "item1", "item2", "item3")),
            collector.lastItem,
        )
    }

    @Test
    fun `GIVEN loaded first page WHEN item far from the end is rendered THEN next page is not loaded`() = runFlowTest {
        val fetchedPages = mutableListOf<Int>()
        val store = storeBuilder().build { pageKey ->
            fetchedPages += pageKey
            PagedList(listOf("a$pageKey", "b$pageKey", "c$pageKey"), pageKey + 1)
        }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.onItemRendered(0) // fetch distance is 1, so the first item must not trigger a load
        runCurrent()

        assertEquals(listOf(0), fetchedPages)
        assertResult(StoreResult.Loaded(listOf("a0", "b0", "c0")), collector.lastItem)
    }

    @Test
    fun `GIVEN last page is loaded WHEN its last item is rendered THEN no more pages are fetched`() = runFlowTest {
        val fetchedPages = mutableListOf<Int>()
        val store = storeBuilder().build { pageKey ->
            fetchedPages += pageKey
            page(pageKey, totalPages = 1) // single page, nextKey = null
        }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.onItemRendered(1) // the last item of the last page
        runCurrent()

        assertEquals(listOf(0), fetchedPages)
        assertResult(StoreResult.Loaded(listOf("item0", "item1")), collector.lastItem)
        assertEquals(PageState.Idle, collector.lastItem.nextPageState)
    }

    @Test
    fun `GIVEN next page load in progress WHEN observe THEN next page state is pending`() = runFlowTest {
        val store = storeBuilder().build { pageKey ->
            if (pageKey > 0) delay(10)
            page(pageKey, totalPages = 2)
        }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.onItemRendered(1) // trigger the next page load

        advanceTimeBy(10) // almost done next page load
        assertResult(StoreResult.Loaded(listOf("item0", "item1")), collector.lastItem)
        assertEquals(PageState.Pending, collector.lastItem.nextPageState)
        advanceTimeBy(1) // now loaded
        assertResult(
            StoreResult.Loaded(listOf("item0", "item1", "item2", "item3")),
            collector.lastItem,
        )
        assertEquals(PageState.Idle, collector.lastItem.nextPageState)
    }

    @Test
    fun `GIVEN duplicated items between pages WHEN pages are merged THEN duplicates are removed`() = runFlowTest {
        val store = storeBuilder().build { pageKey ->
            when (pageKey) {
                0 -> PagedList(listOf("a", "b", "c"), 1)
                else -> PagedList(listOf("c", "d"), null) // "c" is duplicated
            }
        }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.onItemRendered(2)
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("a", "b", "c", "d")), collector.lastItem)
    }

    @Test
    fun `GIVEN page with attached metadata WHEN observe THEN metadata is propagated to the result`() = runFlowTest {
        val store = storeBuilder().build { pageKey ->
            PagedList(
                items = listOf("item0", "item1"),
                nextKey = null,
                metadata = TotalPagedItemsCountMetadata(totalPagedItemsCount = 100),
            )
        }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("item0", "item1")), collector.lastItem)
        assertEquals(100, collector.lastItem.metadata.totalPagedItemsCount)
    }

    @Test
    fun `GIVEN page built with totalCount constructor WHEN observe THEN total count is propagated to the result`() = runFlowTest {
        val store = storeBuilder().build { pageKey ->
            PagedList(
                items = listOf("item0", "item1"),
                nextKey = null,
                totalCount = 100,
            )
        }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("item0", "item1")), collector.lastItem)
        assertEquals(100, collector.lastItem.metadata.totalPagedItemsCount)
    }

    @Test
    fun `GIVEN pages with different metadata WHEN pages are merged THEN the latest page metadata wins`() = runFlowTest {
        val store = storeBuilder().build { pageKey ->
            PagedList(
                items = listOf("item${pageKey * 2}", "item${pageKey * 2 + 1}"),
                nextKey = (pageKey + 1).takeIf { it < 2 },
                metadata = TotalPagedItemsCountMetadata(totalPagedItemsCount = pageKey),
            )
        }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.onItemRendered(1) // trigger loading of the second page
        runCurrent()

        assertResult(
            StoreResult.Loaded(listOf("item0", "item1", "item2", "item3")),
            collector.lastItem,
        )
        // the second page reported total count = 1, which replaces the first page's count = 0
        assertEquals(1, collector.lastItem.metadata.totalPagedItemsCount)
    }

    @Test
    fun `GIVEN loaded first page WHEN onItemRendered via result THEN next page is loaded and merged`() = runFlowTest {
        val store = storeBuilder().build { pageKey -> page(pageKey, totalPages = 2) }
        val collector = store.observe().startCollecting()
        runCurrent()

        // report the rendered item through the emitted result's metadata
        collector.lastItem.onItemRendered(1)
        runCurrent()

        assertResult(
            StoreResult.Loaded(listOf("item0", "item1", "item2", "item3")),
            collector.lastItem,
        )
    }

}
