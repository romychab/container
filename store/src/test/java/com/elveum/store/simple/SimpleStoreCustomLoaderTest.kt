package com.elveum.store.simple

import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.delay
import org.junit.Test

class SimpleStoreCustomLoaderTest : AbstractSimpleStoreTest() {

    @Test
    fun `GIVEN custom loader emitting cached then remote WHEN observe THEN both values are emitted in order`() = runFlowTest {
        val store = storeBuilder().buildCustom {
            emit("cached", LocalSourceType)
            delay(10)
            emit("remote", RemoteSourceType, isLastValue = true)
        }

        val collector = store.observe().startCollecting()
        runCurrent()
        // the cached value is emitted first, while the remote load is still in progress
        assertResult(StoreResult.Loaded("cached"), collector.lastItem)

        advanceTimeBy(11)
        // then the remote value replaces it
        assertResult(StoreResult.Loaded("remote"), collector.lastItem)
    }

    @Test
    fun `GIVEN custom loader emitting a single value WHEN observe THEN that value is emitted`() = runFlowTest {
        val store = storeBuilder().buildCustom {
            emit("only", RemoteSourceType, isLastValue = true)
        }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("only"), collector.lastItem)
    }

    @Test
    fun `GIVEN simple query custom loader WHEN observe THEN the query reaches the loader`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .buildCustom { query -> emit("value-$query", isLastValue = true) }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-q1"), collector.lastItem)
    }
}
