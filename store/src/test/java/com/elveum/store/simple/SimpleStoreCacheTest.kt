package com.elveum.store.simple

import com.elveum.store.exceptions.NoCachedDataException
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.load.isFailed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SimpleStoreCacheTest : AbstractSimpleStoreTest() {

    @Test
    fun `GIVEN cached value WHEN re-observe before cache timeout THEN cached value is reused`() = runFlowTest {
        var counter = 0
        val store = storeBuilder()
            .setInMemoryCacheTimeout(1.seconds)
            .build { "value${++counter}" }
        val collector1 = store.observe().startCollecting()
        runCurrent()
        collector1.cancel()

        advanceTimeBy(999) // cache is not expired yet
        val collector2 = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value1"), collector2.lastItem)
        assertEquals(1, counter)
    }

    @Test
    fun `GIVEN cached value WHEN re-observe after cache timeout THEN value is fetched again`() = runFlowTest {
        var counter = 0
        val store = storeBuilder()
            .setInMemoryCacheTimeout(1.seconds)
            .build { "value${++counter}" }
        val collector1 = store.observe().startCollecting()
        runCurrent()
        collector1.cancel()

        advanceTimeBy(1001) // cache is expired
        val collector2 = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value2"), collector2.lastItem)
        assertEquals(2, counter)
    }

    @Test
    fun `GIVEN failed fetch WHEN observe THEN emit failed result`() = runFlowTest {
        val exception = IllegalStateException("fetch failed")
        val store = storeBuilder().build { throw exception }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Failed(exception), collector.lastItem)
    }

    @Test
    fun `GIVEN failed fetch WHEN invalidate THEN data is loaded again`() = runFlowTest {
        var counter = 0
        val store = storeBuilder().build {
            counter++
            if (counter == 1) throw IllegalStateException("fetch failed")
            "value$counter"
        }
        val collector = store.observe().startCollecting()
        runCurrent()
        assertTrue(collector.lastItem.isFailed())

        store.invalidateAsync()
        runCurrent()

        assertResult(StoreResult.Loaded("value2"), collector.lastItem)
    }

    @Test
    fun `GIVEN empty cache WHEN observe in offline mode THEN emit no-cached-data error`() = runFlowTest {
        val store = storeBuilder().build { "value" }
        val offlineRequest = LoadRequest.builder().offlineMode().build()

        val collector = store.observe(offlineRequest).startCollecting()
        runCurrent()

        val lastItem = collector.lastItem
        assertTrue(lastItem.isFailed())
        assertTrue((lastItem as StoreResult.Failed).exception is NoCachedDataException)
    }

    @Test
    fun `GIVEN cached value WHEN invalidate THEN value is re-fetched`() = runFlowTest {
        var counter = 0
        val store = storeBuilder().build { "value${++counter}" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.invalidateAsync()
        runCurrent()

        assertResult(StoreResult.Loaded("value2"), collector.lastItem)
        assertEquals(2, counter)
    }

}
