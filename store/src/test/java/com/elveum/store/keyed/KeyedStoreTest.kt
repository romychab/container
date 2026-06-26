package com.elveum.store.keyed

import com.elveum.container.RemoteSourceType
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.load.isBackgroundLoading
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyedStoreTest : AbstractKeyedStoreTest() {

    @Test
    fun `GIVEN store WHEN observe by key THEN emit loading and then loaded state`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }

        val collector = store.observe("k1").startCollecting()

        // the initial state is Loading
        assertEquals(StoreResult.Loading, collector.lastItem)
        // the next state is Loaded
        runCurrent()
        assertResult(StoreResult.Loaded("value-k1"), collector.lastItem)
        assertEquals(RemoteSourceType, collector.lastItem.sourceType)
    }

    @Test
    fun `GIVEN store WHEN observe different keys THEN each key is fetched and cached separately`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }

        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-k1"), collector1.lastItem)
        assertResult(StoreResult.Loaded("value-k2"), collector2.lastItem)
    }

    @Test
    fun `GIVEN store WHEN observe the same key multiple times THEN fetcher is called once`() = runFlowTest {
        val fetcher = mockk<suspend (String) -> String>()
        coEvery { fetcher.invoke("k1") } returns "value-k1"
        val store = storeBuilder().build(fetcher)

        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k1").startCollecting()
        runCurrent()

        coVerify(exactly = 1) { fetcher.invoke("k1") }
        assertResult(StoreResult.Loaded("value-k1"), collector1.lastItem)
        assertResult(StoreResult.Loaded("value-k1"), collector2.lastItem)
    }

    @Test
    fun `GIVEN two observed keys WHEN invalidate one key THEN only that key is re-fetched`() = runFlowTest {
        val counters = mutableMapOf<String, Int>()
        val store = storeBuilder().build { key ->
            val counter = (counters[key] ?: 0) + 1
            counters[key] = counter
            "value-$key-$counter"
        }
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()

        store.invalidateAsync("k1")
        runCurrent()

        assertResult(StoreResult.Loaded("value-k1-2"), collector1.lastItem)
        assertResult(StoreResult.Loaded("value-k2-1"), collector2.lastItem)
        assertEquals(2, counters["k1"])
        assertEquals(1, counters["k2"])
    }

    @Test
    fun `GIVEN silent async invalidate by key WHEN observe THEN emit background load`() = runFlowTest {
        var counter = 0
        val store = storeBuilder().build { key ->
            delay(10)
            "value-$key-${++counter}"
        }
        val collector = store.observe("k1").startCollecting()
        advanceTimeBy(11) // first load

        store.invalidateAsync("k1", LoadRequest.Silent)

        advanceTimeBy(10) // almost done second load
        assertResult(StoreResult.Loaded("value-k1-1"), collector.lastItem)
        assertTrue(collector.lastItem.isBackgroundLoading())
        advanceTimeBy(1) // now loaded
        assertResult(StoreResult.Loaded("value-k1-2"), collector.lastItem)
    }

    @Test
    fun `GIVEN failed fetch for one key WHEN observe THEN other keys are not affected`() = runFlowTest {
        val exception = IllegalStateException("fetch failed")
        val store = storeBuilder().build { key ->
            if (key == "bad") throw exception
            "value-$key"
        }

        val badCollector = store.observe("bad").startCollecting()
        val goodCollector = store.observe("good").startCollecting()
        runCurrent()

        assertResult(StoreResult.Failed(exception), badCollector.lastItem)
        assertResult(StoreResult.Loaded("value-good"), goodCollector.lastItem)
    }

    @Test
    fun `GIVEN data loading WHEN get by key THEN return latest result`() = runFlowTest {
        val store = storeBuilder().build { key ->
            delay(10)
            "value-$key"
        }

        store.observe("k1").startCollecting()

        advanceTimeBy(10)
        assertResult(StoreResult.Loading, store.get("k1"))

        advanceTimeBy(1)
        assertResult(StoreResult.Loaded("value-k1"), store.get("k1"))
    }

    @Test
    fun `GIVEN separate keys WHEN get THEN each key returns its own result`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }
        store.observe("k1").startCollecting()
        store.observe("k2").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-k1"), store.get("k1"))
        assertResult(StoreResult.Loaded("value-k2"), store.get("k2"))
    }

    @Test
    fun `GIVEN loaded key WHEN updateWith THEN new result is emitted and returned by get`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()

        store.updateWith("k1", StoreResult.Loaded("manual"))
        runCurrent()

        // only the updated key is affected
        assertResult(StoreResult.Loaded("manual"), collector1.lastItem)
        assertResult(StoreResult.Loaded("manual"), store.get("k1"))
        assertResult(StoreResult.Loaded("value-k2"), collector2.lastItem)
        assertResult(StoreResult.Loaded("value-k2"), store.get("k2"))
    }

    @Test
    fun `GIVEN loaded key WHEN updateWith failed result THEN failure is emitted to observers`() = runFlowTest {
        val exception = IllegalStateException("manual failure")
        val store = storeBuilder().build { key -> "value-$key" }
        val collector = store.observe("k1").startCollecting()
        runCurrent()

        store.updateWith("k1", StoreResult.Failed(exception))
        runCurrent()

        assertResult(StoreResult.Failed(exception), collector.lastItem)
        assertResult(StoreResult.Failed(exception), store.get("k1"))
    }

}
