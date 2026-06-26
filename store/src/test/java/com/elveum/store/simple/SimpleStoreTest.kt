package com.elveum.store.simple

import com.elveum.container.RemoteSourceType
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.load.invalidate
import com.elveum.store.load.isBackgroundLoading
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleStoreTest : AbstractSimpleStoreTest() {

    @Test
    fun `GIVEN store WHEN observe THEN emit loading and then loaded state`() = runFlowTest {
        val store = storeBuilder().build { "value" }

        val collector = store.observe().startCollecting()

        // the initial state is Loading
        assertEquals(StoreResult.Loading, collector.lastItem)
        // the next state is Loaded
        runCurrent()
        assertResult(StoreResult.Loaded("value"), collector.lastItem)
        // the total number of items
        assertEquals(2, collector.count)
    }

    @Test
    fun `GIVEN store WHEN observe multiple times THEN fetcher is called once`() = runFlowTest {
        val fetcher = mockk<suspend () -> String>()
        coEvery { fetcher.invoke() } returns "value"
        val store = storeBuilder().build(fetcher)

        val collector1 = store.observe().startCollecting()
        val collector2 = store.observe().startCollecting()
        runCurrent()

        coVerify(exactly = 1) { fetcher.invoke() }
        assertResult(StoreResult.Loaded("value"), collector1.lastItem)
        assertResult(StoreResult.Loaded("value"), collector2.lastItem)
    }

    @Test
    fun `GIVEN store with delayed load WHEN observe THEN emit data when it is loaded`() = runFlowTest {
        val store = storeBuilder().build {
            delay(100)
            "value"
        }

        val collector = store.observe().startCollecting()

        // state right before the value is loaded:
        advanceTimeBy(100)
        assertEquals(1, collector.count)
        assertResult(StoreResult.Loading, collector.lastItem)
        // state after the value has been loaded:
        advanceTimeBy(1)
        assertEquals(2, collector.count)
        assertResult(StoreResult.Loaded("value"), collector.lastItem)
    }

    @Test
    fun `GIVEN store WHEN observe THEN emit correct metadata`() = runFlowTest {
        val store = storeBuilder().build { "value" }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertEquals(RemoteSourceType, collector.lastItem.sourceType)
        assertFalse(collector.lastItem.isBackgroundLoading())
    }

    @Test
    fun `GIVEN async invalidate WHEN observe THEN emit foreground load`() = runFlowTest {
        var counter = 0
        val store = storeBuilder().build {
            delay(10)
            "value${++counter}"
        }
        val collector = store.observe().startCollecting()
        advanceTimeBy(11) // first load

        store.invalidateAsync()

        advanceTimeBy(10) // almost done second load
        assertResult(StoreResult.Loading, collector.lastItem)
        advanceTimeBy(1) // now loaded
        assertResult(StoreResult.Loaded("value2"), collector.lastItem)
    }

    @Test
    fun `GIVEN silent async invalidate WHEN observe THEN emit background load`() = runFlowTest {
        var counter = 0
        val store = storeBuilder().build {
            delay(10)
            "value${++counter}"
        }
        val collector = store.observe().startCollecting()
        advanceTimeBy(11) // first load

        store.invalidateAsync(LoadRequest.Silent)

        advanceTimeBy(10) // almost done second load
        assertResult(StoreResult.Loaded("value1"), collector.lastItem)
        assertTrue(collector.lastItem.isBackgroundLoading())
        advanceTimeBy(1) // now loaded
        assertResult(StoreResult.Loaded("value2"), collector.lastItem)
    }

    @Test
    fun `GIVEN data loading WHEN currentResult THEN return latest result`() = runFlowTest {
        val store = storeBuilder().build {
            delay(10)
            "value"
        }

        store.observe().startCollecting()

        advanceTimeBy(10)
        assertResult(StoreResult.Loading, store.get())

        advanceTimeBy(1)
        assertResult(StoreResult.Loaded("value"), store.get())
    }

    @Test
    fun `GIVEN loaded result WHEN invalidate via result THEN the origin store reloads`() = runFlowTest {
        var counter = 0
        val store = storeBuilder().build { "value${++counter}" }
        val collector = store.observe().startCollecting()
        runCurrent()
        assertResult(StoreResult.Loaded("value1"), collector.lastItem)

        // trigger a reload through the emitted result's metadata
        collector.lastItem.invalidate()
        runCurrent()

        assertResult(StoreResult.Loaded("value2"), collector.lastItem)
    }

}