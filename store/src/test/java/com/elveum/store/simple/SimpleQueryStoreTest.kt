package com.elveum.store.simple

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.load.isBackgroundLoading
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleQueryStoreTest : AbstractSimpleStoreTest() {

    @Test
    fun `GIVEN store with query WHEN observe THEN fetcher is called with initial query`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query -> "value-$query" }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-q1"), collector.lastItem)
        assertEquals("q1", store.queryFlow.value)
    }

    @Test
    fun `GIVEN observed store WHEN submit new query THEN data is re-fetched with new query`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query -> "value-$query" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.submitQueryAsync("q2")
        runCurrent()

        assertResult(StoreResult.Loaded("value-q2"), collector.lastItem)
        assertEquals("q2", store.queryFlow.value)
    }

    @Test
    fun `GIVEN submitted query with silent request WHEN re-fetch is in progress THEN old content is kept`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query ->
                delay(10)
                "value-$query"
            }
        val collector = store.observe(LoadRequest.Silent).startCollecting()
        advanceTimeBy(11) // first load

        store.submitQueryAsync("q2") // observer keeps content while the new query loads

        advanceTimeBy(10) // almost done second load
        assertResult(StoreResult.Loaded("value-q1"), collector.lastItem)
        assertTrue(collector.lastItem.isBackgroundLoading())
        advanceTimeBy(1) // now loaded
        assertResult(StoreResult.Loaded("value-q2"), collector.lastItem)
    }

    @Test
    fun `GIVEN submitted query with only silent load WHEN re-fetch is in progress THEN old content is not kept`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query ->
                delay(10)
                "value-$query"
            }
        val loadRequest = LoadRequest.builder()
            .keepContentOnLoad()
            .build()
        val collector = store.observe(loadRequest).startCollecting()
        advanceTimeBy(11) // first load

        store.submitQueryAsync("q2") // observer keeps content while the new query loads

        advanceTimeBy(10) // almost done second load
        assertResult(StoreResult.Loading, collector.lastItem)
        advanceTimeBy(1) // now loaded
        assertResult(StoreResult.Loaded("value-q2"), collector.lastItem)
    }

    @Test
    fun `GIVEN submitted query with non-silent request WHEN re-fetch is in progress THEN loading state is emitted`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query ->
                delay(10)
                "value-$query"
            }
        val collector = store.observe().startCollecting()
        advanceTimeBy(11) // first load

        store.submitQueryAsync("q2")

        advanceTimeBy(10) // almost done second load
        assertResult(StoreResult.Loading, collector.lastItem)
        advanceTimeBy(1) // now loaded
        assertResult(StoreResult.Loaded("value-q2"), collector.lastItem)
    }

    @Test
    fun `GIVEN query with debounce WHEN queries are submitted quickly THEN only the last one is fetched`() = runFlowTest {
        val fetchedQueries = mutableListOf<String>()
        val store = storeBuilder()
            .withQuery(initialQuery = "initial", debounceMillis = 100)
            .build { query ->
                fetchedQueries += query
                "value-$query"
            }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.submitQueryAsync("a")
        advanceTimeBy(50) // debounce is not finished yet
        store.submitQueryAsync("ab")
        advanceTimeBy(101) // debounce is finished

        assertResult(StoreResult.Loaded("value-ab"), collector.lastItem)
        assertEquals(listOf("initial", "ab"), fetchedQueries)
    }

}
