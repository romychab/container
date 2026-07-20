package com.elveum.container.subject

import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import com.elveum.container.utils.raw
import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class LazyFlowSubjectLifecycleIntegrationTest : AbstractLazyFlowSubjectIntegrationTest() {

    @Test
    fun activeCollectorsCount_withoutCollectors_returnsZero() = runFlowTest {
        val subject = createLazyFlowSubject()

        assertEquals(0, subject.activeCollectorsCount)
    }

    @Test
    fun activeCollectorsCount_with1ActiveCollectors_returns1() = runFlowTest {
        val loader: ValueLoader<String> = ValueLoader { emit("111") }
        val subject = createLazyFlowSubject(loader)

        subject.listen().startCollecting()

        assertEquals(1, subject.activeCollectorsCount)
    }

    @Test
    fun activeCollectorsCount_with2Collectors_returns2() = runFlowTest {
        val loader: ValueLoader<String> = ValueLoader { emit("111") }
        val subject = createLazyFlowSubject(loader)

        subject.listen().startCollecting()
        subject.listen().startCollecting()

        assertEquals(2, subject.activeCollectorsCount)
    }

    @Test
    fun activeCollectorsCount_with2CollectorsOnSameFlow_returns2() = runFlowTest {
        val loader: ValueLoader<String> = ValueLoader { emit("111") }
        val subject = createLazyFlowSubject(loader)

        val flow = subject.listen()
        flow.startCollecting()
        flow.startCollecting()

        assertEquals(2, subject.activeCollectorsCount)
    }

    @Test
    fun activeCollectorsCount_afterCancellation_decreasesNumberOfCollectors() = runFlowTest {
        val loader: ValueLoader<String> = ValueLoader { emit("111") }
        val subject = createLazyFlowSubject(loader)

        val state1 = subject.listen().startCollecting() // 1 active collector
        val state2 = subject.listen().startCollecting() // 2 active collectors

        state1.cancel() // 1 active collector again
        assertEquals(1, subject.activeCollectorsCount)
        state2.cancel() // 0 active collectors
        assertEquals(0, subject.activeCollectorsCount)
    }

    @Test
    fun whenActive_executesBlockOnLaunchAndCancelsAfterTimeout() = runFlowTest {
        var job: Job? = null
        var rootJobRunning = false
        val subject = createLazyFlowSubject { awaitCancellation() }
            .whenActive {
                rootJobRunning = true
                try {
                    job = launch { awaitCancellation() }
                    awaitCancellation()
                } finally {
                    rootJobRunning = false
                }
            }

        // get flow does not execute whenActive block:
        val flow = subject.listen()
        runCurrent()
        assertNull(job)
        assertFalse(rootJobRunning)

        // start collecting -> executes whenActive block:
        val collector = flow.startCollecting()
        runCurrent()
        assertNotNull(job)
        assertTrue(job!!.isActive)
        assertTrue(rootJobRunning)

        // cancel collecting -> does not stop whenActive block before timeout:
        collector.cancel()
        advanceTimeBy(cacheTimeout - 1)
        assertFalse(job.isCancelled)
        assertTrue(rootJobRunning)

        // after timeout - the block is cancelled:
        advanceTimeBy(2)
        assertTrue(job.isCancelled)
        assertFalse(rootJobRunning)
    }

    @Test
    fun whenActive_executedOncePerSession() = runFlowTest {
        var execCount = 0
        val subject = createLazyFlowSubject { awaitCancellation() }
            .whenActive { execCount++ }

        // run 2 collectors:
        val collector1 = subject.listen().startCollecting()
        runCurrent()
        val collector2 = subject.listen().startCollecting()
        runCurrent()
        // block executed only once:
        assertEquals(1, execCount)

        // cancel 1 collector, add third collector -> still 1 execution
        collector1.cancel()
        advanceTimeBy(cacheTimeout + 1)
        val collector3 = subject.listen().startCollecting()
        runCurrent()
        assertEquals(1, execCount)

        // cancel all collectors, then add fresh collector -> 2nd execution
        collector2.cancel()
        collector3.cancel()
        advanceTimeBy(cacheTimeout + 1)
        subject.listen().startCollecting()
        runCurrent()
        assertEquals(2, execCount)
    }

    @Test
    fun `spy without listeners emits Pending`() = runFlowTest {
        val subject = createLazyFlowSubject {
            delay(10)
            emit("1")
        }

        val collector = subject.spy().startCollecting()
        advanceTimeBy(11)

        assertEquals(
            listOf(pendingContainer()),
            collector.collectedItems
        )
    }

    @Test
    fun `spy with listeners emits output value`() = runFlowTest {
        val subject = createLazyFlowSubject {
            delay(10)
            emit("1")
            delay(cacheTimeout + 1)
            emit("2")
        }

        val spyCollector = subject.spy().startCollecting()
        advanceTimeBy(5)
        val realCollector = subject.listen().startCollecting()

        // nothing is emitted while loading:
        advanceTimeBy(10)
        assertEquals(
            listOf(pendingContainer()),
            spyCollector.collectedItems
        )

        // success item is emitted when real collector is attached:
        advanceTimeBy(1)
        assertEquals(2, spyCollector.count)
        assertEquals(successContainer("1"), spyCollector.lastItem.raw())

        realCollector.cancel()

        // spy collector is not detached before cache timeout expires:
        advanceTimeBy(cacheTimeout)
        assertEquals(2, spyCollector.count)

        // spy collector is detached after cache timeout expires:
        advanceTimeBy(1)
        assertEquals(
            listOf(
                pendingContainer(),
                successContainer("1"),
                pendingContainer(),
            ),
            spyCollector.collectedItems.raw()
        )
    }

}