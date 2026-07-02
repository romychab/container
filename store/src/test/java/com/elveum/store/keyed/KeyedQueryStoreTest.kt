package com.elveum.store.keyed

import com.elveum.store.load.StoreResult
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyedQueryStoreTest : AbstractKeyedStoreTest() {

    private fun queryStore() = storeBuilder()
        .withQuery(initialQuery = "q1")
        .build { key, query -> "$key-$query" }

    @Test
    fun `GIVEN keyed query store WHEN observe a key THEN value is fetched for the initial query`() = runFlowTest {
        val store = queryStore()

        val collector = store.observe("k1").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("k1-q1"), collector.lastItem)
        assertEquals("q1", store.observeQueryFlow("k1").value)
    }

    @Test
    fun `GIVEN keyed query store WHEN submitQuery for one key THEN only that key is re-fetched`() = runFlowTest {
        val store = queryStore()
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()

        store.submitQuery("k1", "q2")
        runCurrent()

        // only the queried key changes
        assertResult(StoreResult.Loaded("k1-q2"), collector1.lastItem)
        assertResult(StoreResult.Loaded("k2-q1"), collector2.lastItem)
        assertEquals("q2", store.observeQueryFlow("k1").value)
        assertEquals("q1", store.observeQueryFlow("k2").value)
    }

    @Test
    fun `GIVEN keyed query store WHEN submitQueryAsync THEN query is updated and value re-fetched`() = runFlowTest {
        val store = queryStore()
        val collector = store.observe("k1").startCollecting()
        runCurrent()

        store.submitQueryAsync("k1", "q2")
        runCurrent()

        assertResult(StoreResult.Loaded("k1-q2"), collector.lastItem)
        assertEquals("q2", store.observeQueryFlow("k1").value)
    }
}
