package com.elveum.store.simple

import com.elveum.store.load.StoreResult
import com.elveum.store.stores.base.updateIfSuccess
import com.uandcode.flowtest.assertCompleted
import com.uandcode.flowtest.assertExecuting
import com.uandcode.flowtest.assertFailure
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SimpleStoreUpdateTest : AbstractSimpleStoreTest() {

    @Test
    fun `GIVEN loaded value WHEN updateWith from current value THEN new value is emitted to observers`() = runFlowTest {
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()


        val oldValue = (store.get() as StoreResult.Loaded).value
        store.updateWith(StoreResult.Loaded("$oldValue-updated"))
        runCurrent()

        assertResult(StoreResult.Loaded("value-updated"), collector.lastItem)
    }

    @Test
    fun `GIVEN loaded value WHEN updateWith THEN new result is emitted and returned by get`() = runFlowTest {
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.updateWith(StoreResult.Loaded("manual"))
        runCurrent()

        assertResult(StoreResult.Loaded("manual"), collector.lastItem)
        assertResult(StoreResult.Loaded("manual"), store.get())
    }

    @Test
    fun `GIVEN loaded value WHEN updateWith failed result THEN failure is emitted to observers`() = runFlowTest {
        val exception = IllegalStateException("manual failure")
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.updateWith(StoreResult.Failed(exception))
        runCurrent()

        assertResult(StoreResult.Failed(exception), collector.lastItem)
        assertResult(StoreResult.Failed(exception), store.get())
    }

    @Test
    fun `GIVEN loaded value WHEN updateIfSuccess THEN transformed value is emitted to observers`() = runFlowTest {
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()


        store.updateIfSuccess { "$it-updated" }
        runCurrent()

        assertResult(StoreResult.Loaded("value-updated"), collector.lastItem)
    }

    @Test
    fun `GIVEN failed value WHEN updateIfSuccess THEN updater is not invoked and result is unchanged`() = runFlowTest {
        val exception = IllegalStateException("load failed")
        val store = storeBuilder().build { throw exception }
        val collector = store.observe().startCollecting()
        runCurrent()
        assertResult(StoreResult.Failed(exception), collector.lastItem)

        var invoked = false

        store.updateIfSuccess { invoked = true; "$it-updated" }
        runCurrent()

        assertFalse(invoked)
        assertResult(StoreResult.Failed(exception), collector.lastItem)
    }

    @Test
    fun `GIVEN successful real update WHEN optimisticUpdate THEN optimistic value is kept`() = runFlowTest {
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.optimisticUpdate {
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
    fun `GIVEN failed real update WHEN optimisticUpdate THEN optimistic value is reverted`() = runFlowTest {
        val exception = IllegalStateException("update failed")
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()

        val job = executeInBackground {
            store.optimisticUpdate {
                emit("optimistic")
                delay(10) // simulate a real update
                throw exception
            }
        }
        runCurrent()
        assertResult(StoreResult.Loaded("optimistic"), collector.lastItem)

        advanceTimeBy(11)
        // the optimistic value has been reverted back
        assertResult(StoreResult.Loaded("value"), collector.lastItem)
        job.assertFailure(exception)
    }

    @Test
    fun `GIVEN observers WHEN suspend invalidate THEN it waits for reload completion`() = runFlowTest {
        var counter = 0
        val store = storeBuilder().build {
            delay(10)
            "value${++counter}"
        }
        val collector = store.observe().startCollecting()
        advanceTimeBy(11) // first load

        val job = executeInBackground {
            store.invalidate()
        }

        advanceTimeBy(10) // almost done second load
        job.assertExecuting()
        advanceTimeBy(1) // now loaded
        job.assertCompleted(Unit)
        assertResult(StoreResult.Loaded("value2"), collector.lastItem)
    }

    @Test
    fun `GIVEN whenActive block WHEN store becomes active and inactive THEN block is started and cancelled`() = runFlowTest {
        var active = false
        val store = storeBuilder()
            .setInMemoryCacheTimeout(1.seconds)
            .build { "value" }
            .whenActive {
                active = true
                try {
                    awaitCancellation()
                } finally {
                    active = false
                }
            }
        assertFalse(active)

        // the first observer activates the store
        val collector = store.observe().startCollecting()
        runCurrent()
        assertTrue(active)

        // the block is still active while the cache is alive
        collector.cancel()
        advanceTimeBy(999)
        assertTrue(active)

        // the block is cancelled when the cache is released
        advanceTimeBy(2)
        assertFalse(active)
    }

    @Test
    fun `GIVEN whenActive updating from events WHEN event arrives THEN cached value is updated`() = runFlowTest {
        val events = kotlinx.coroutines.flow.MutableSharedFlow<String>()
        val store = storeBuilder()
            .build { "value" }
            .whenActive {
                events.collect { event ->
                    updateWith(StoreResult.Loaded(event))
                }
            }
        val collector = store.observe().startCollecting()
        runCurrent()

        events.emit("external-update")
        runCurrent()

        assertResult(StoreResult.Loaded("external-update"), collector.lastItem)
    }

}
