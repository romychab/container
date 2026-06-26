package com.elveum.store.paged

import com.elveum.container.subject.paging.PageState
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.load.isBackgroundLoading
import com.elveum.store.load.nextPageState
import com.elveum.store.stores.base.update
import com.elveum.store.stores.paged.PagedList
import com.uandcode.flowtest.assertFailure
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagedStoreErrorsAndUpdatesTest : AbstractPagedStoreTest() {

    @Test
    fun `GIVEN failed first page WHEN observe THEN emit failed result`() = runFlowTest {
        val exception = IllegalStateException("fetch failed")
        val store = storeBuilder().build { throw exception }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Failed(exception), collector.lastItem)
    }

    @Test
    fun `GIVEN failed next page WHEN retry THEN the failed page is loaded again`() = runFlowTest {
        val exception = IllegalStateException("fetch failed")
        var failNextPage = true
        val store = storeBuilder().build { pageKey ->
            if (pageKey > 0 && failNextPage) throw exception
            page(pageKey, totalPages = 2)
        }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.onItemRendered(1) // trigger the failing next page load
        runCurrent()

        // already loaded items are kept; the error is exposed via nextPageState
        assertResult(StoreResult.Loaded(listOf("item0", "item1")), collector.lastItem)
        val pageState = collector.lastItem.nextPageState
        assertTrue(pageState is PageState.Error)

        // retry the failed page only
        failNextPage = false
        (pageState as PageState.Error).retry()
        runCurrent()

        assertResult(
            StoreResult.Loaded(listOf("item0", "item1", "item2", "item3")),
            collector.lastItem,
        )
    }

    @Test
    fun `GIVEN two loaded pages WHEN invalidate THEN pagination is reset to the first page`() = runFlowTest {
        val fetchedPages = mutableListOf<Int>()
        val store = storeBuilder().build { pageKey ->
            fetchedPages += pageKey
            page(pageKey, totalPages = 2)
        }
        val collector = store.observe().startCollecting()
        runCurrent()
        store.onItemRendered(1) // load the second page
        runCurrent()
        assertResult(
            StoreResult.Loaded(listOf("item0", "item1", "item2", "item3")),
            collector.lastItem,
        )

        store.invalidateAsync()
        runCurrent()

        // only the first page is loaded again
        assertResult(StoreResult.Loaded(listOf("item0", "item1")), collector.lastItem)
        assertEquals(listOf(0, 1, 0), fetchedPages)
    }

    @Test
    fun `GIVEN silent async invalidate WHEN observe THEN old items are kept while reloading`() = runFlowTest {
        var counter = 0
        val store = storeBuilder().build { pageKey ->
            delay(10)
            counter++
            PagedList(listOf("value$counter"), null)
        }
        val collector = store.observe().startCollecting()
        advanceTimeBy(11) // first load

        store.invalidateAsync(LoadRequest.Silent)

        advanceTimeBy(10) // almost done second load
        assertResult(StoreResult.Loaded(listOf("value1")), collector.lastItem)
        assertTrue(collector.lastItem.isBackgroundLoading())
        advanceTimeBy(1) // now loaded
        assertResult(StoreResult.Loaded(listOf("value2")), collector.lastItem)
    }

    @Test
    fun `GIVEN loaded list WHEN update THEN new list is emitted to observers`() = runFlowTest {
        val store = storeBuilder().build { pageKey -> page(pageKey, totalPages = 1) }
        val collector = store.observe().startCollecting()
        runCurrent()

        executeInBackground {
            store.update { oldList -> oldList + "appended" }
        }
        runCurrent()

        assertResult(
            StoreResult.Loaded(listOf("item0", "item1", "appended")),
            collector.lastItem,
        )
    }

    @Test
    fun `GIVEN data loading WHEN get THEN return latest merged list result`() = runFlowTest {
        val store = storeBuilder().build { pageKey ->
            delay(10)
            page(pageKey, totalPages = 1)
        }

        store.observe().startCollecting()

        advanceTimeBy(10)
        assertResult(StoreResult.Loading, store.get())

        advanceTimeBy(1)
        assertResult(StoreResult.Loaded(listOf("item0", "item1")), store.get())
    }

    @Test
    fun `GIVEN loaded list WHEN updateWith THEN new result is emitted and returned by get`() = runFlowTest {
        val store = storeBuilder().build { pageKey -> page(pageKey, totalPages = 1) }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.updateWith(StoreResult.Loaded(listOf("manual")))
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("manual")), collector.lastItem)
        assertResult(StoreResult.Loaded(listOf("manual")), store.get())
    }

    @Test
    fun `GIVEN failed real update WHEN optimisticUpdate THEN optimistic list is reverted`() = runFlowTest {
        val exception = IllegalStateException("update failed")
        val store = storeBuilder().build { pageKey -> page(pageKey, totalPages = 1) }
        val collector = store.observe().startCollecting()
        runCurrent()

        val job = executeInBackground {
            store.optimisticUpdate { oldList ->
                emit(oldList.map { item -> "$item-updated" })
                delay(10) // simulate a real update
                throw exception
            }
        }
        runCurrent()
        assertResult(
            StoreResult.Loaded(listOf("item0-updated", "item1-updated")),
            collector.lastItem,
        )

        advanceTimeBy(11)
        // the optimistic list has been reverted back
        assertResult(StoreResult.Loaded(listOf("item0", "item1")), collector.lastItem)
        job.assertFailure(exception)
    }

}
