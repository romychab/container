package com.elveum.store.keyed

import com.elveum.store.load.StoreResult
import com.elveum.store.stores.keyed.updateIfSuccess
import com.uandcode.flowtest.assertFailure
import kotlinx.coroutines.delay
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class KeyedStoreUpdateTest : AbstractKeyedStoreTest() {

    @Test
    fun `GIVEN loaded key WHEN update THEN new value is emitted to observers of that key`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()


        val oldValue = (store.get("k1") as StoreResult.Loaded).value
        store.updateWith("k1", StoreResult.Loaded("$oldValue-updated"))
        runCurrent()

        // only the updated key is affected
        assertResult(StoreResult.Loaded("value-k1-updated"), collector1.lastItem)
        assertResult(StoreResult.Loaded("value-k2"), collector2.lastItem)
    }

    @Test
    fun `GIVEN loaded key WHEN updateIfSuccess THEN transformed value is emitted for that key`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()


        store.updateIfSuccess("k1") { "$it-updated" }
        runCurrent()

        // only the loaded, targeted key is transformed
        assertResult(StoreResult.Loaded("value-k1-updated"), collector1.lastItem)
        assertResult(StoreResult.Loaded("value-k2"), collector2.lastItem)
    }

    @Test
    fun `GIVEN failed key WHEN updateIfSuccess THEN updater is not invoked and result is unchanged`() = runFlowTest {
        val exception = IllegalStateException("fetch failed")
        val store = storeBuilder().build { key ->
            if (key == "bad") throw exception else "value-$key"
        }
        val collector = store.observe("bad").startCollecting()
        runCurrent()
        assertResult(StoreResult.Failed(exception), collector.lastItem)

        var invoked = false
        store.updateIfSuccess("bad") { invoked = true; "$it-updated" }
        runCurrent()

        assertFalse(invoked)
        assertResult(StoreResult.Failed(exception), collector.lastItem)
    }

    @Test
    fun `GIVEN successful real update WHEN optimisticUpdate by key THEN optimistic value is kept`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }
        val collector = store.observe("k1").startCollecting()
        runCurrent()

        store.optimisticUpdate("k1") {
            emit("optimistic")
            delay(10) // simulate a real update
        }
        runCurrent()

        // optimistic value is visible while the real update is in progress
        assertResult(StoreResult.Loaded("optimistic"), collector.lastItem)

        advanceTimeBy(11)
        // optimistic value is kept after the successful real update
        assertResult(StoreResult.Loaded("optimistic"), collector.lastItem)
    }

    @Test
    fun `GIVEN failed real update WHEN optimisticUpdate by key THEN optimistic value is reverted`() = runFlowTest {
        val exception = IllegalStateException("update failed")
        val store = storeBuilder().build { key -> "value-$key" }
        val collector = store.observe("k1").startCollecting()
        runCurrent()

        val job = executeInBackground {
            store.optimisticUpdate("k1") {
                emit("optimistic")
                delay(10) // simulate a real update
                throw exception
            }
        }
        runCurrent()
        assertResult(StoreResult.Loaded("optimistic"), collector.lastItem)

        advanceTimeBy(11)
        // the optimistic value has been reverted back
        assertResult(StoreResult.Loaded("value-k1"), collector.lastItem)
        job.assertFailure(exception)
    }

    @Test
    fun `GIVEN expired key cache WHEN re-observe key THEN value is fetched again`() = runFlowTest {
        var counter = 0
        val store = storeBuilder()
            .setInMemoryCacheTimeout(1.seconds)
            .build { key -> "value-$key-${++counter}" }
        val collector1 = store.observe("k1").startCollecting()
        runCurrent()
        collector1.cancel()

        advanceTimeBy(1001) // cache of "k1" is expired
        val collector2 = store.observe("k1").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-k1-2"), collector2.lastItem)
    }

    @Test
    fun `GIVEN released key cache WHEN re-observe key before timeout THEN cached value is reused`() = runFlowTest {
        var counter = 0
        val store = storeBuilder()
            .setInMemoryCacheTimeout(1.seconds)
            .build { key -> "value-$key-${++counter}" }
        val collector1 = store.observe("k1").startCollecting()
        runCurrent()
        collector1.cancel()

        advanceTimeBy(999) // cache of "k1" is not expired yet
        val collector2 = store.observe("k1").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-k1-1"), collector2.lastItem)
    }

}
