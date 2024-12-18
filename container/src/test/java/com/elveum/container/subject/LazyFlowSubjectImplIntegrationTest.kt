
package com.elveum.container.subject

import com.elveum.container.Container.Error
import com.elveum.container.Container.Pending
import com.elveum.container.Container.Success
import com.elveum.container.LoadTrigger
import com.elveum.container.subject.factories.CoroutineScopeFactory
import com.elveum.container.utils.FlowTest
import com.elveum.container.utils.JobStatus
import com.elveum.container.utils.runFlowTest
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Test.None

class LazyFlowSubjectImplIntegrationTest {

    private val cacheTimeout = 1000L

    @Test
    fun listen_whileLoadingItems_emitsPendingStatus() = runFlowTest {
        val subject = createLazyFlowSubject {
            delay(1000)
        }

        val collectedItems = subject.listen().startCollectingToList()
        runCurrent()

        assertEquals(
            listOf(Pending),
            collectedItems
        )
    }

    @Test
    fun listen_withEmptyLoader_emitsIllegalStateException() = runFlowTest {
        val subject = createLazyFlowSubject { }

        val collectedItems = subject.listen().startCollectingToList()
        runCurrent()

        assertEquals(2, collectedItems.size)
        assertEquals(Pending, collectedItems[0])
        assertTrue((collectedItems[1] as Error).exception is IllegalStateException)
    }

    @Test
    fun listen_emitsItemsProducedByLoader() = runFlowTest {
        val subject = createLazyFlowSubject {
            emit("111")
            emit("222")
            delay(100)
            emit("333")
        }

        val collectedItems = subject.listen().startCollectingToList(unconfined = false)
        runCurrent()

        assertEquals(
            listOf(Pending, Success("111"), Success("222")),
            collectedItems
        )
        advanceTimeBy(101)
        assertEquals(
            listOf(Pending, Success("111"), Success("222"), Success("333")),
            collectedItems
        )
    }

    @Test
    fun listen_withFailedLoad_emitsException() = runFlowTest {
        val subject = createLazyFlowSubject {
            emit("111")
            throw IllegalArgumentException()
        }

        val collectedItems = subject.listen().startCollectingToList()
        runCurrent()

        assertEquals(3, collectedItems.size)
        assertEquals(Pending, collectedItems[0])
        assertEquals(Success("111"), collectedItems[1])
        assertTrue((collectedItems[2] as Error).exception is IllegalArgumentException)
    }

    @Test
    fun listen_withinTimeout_holdsLatestLoadedValue() = runFlowTest {
        var index = 0
        val loader: ValueLoader<String> = {
            emit("v${++index}")
        }
        val spyLoader = spyk(loader)
        val subject = createLazyFlowSubject(spyLoader)

        val jobState1 = subject.listen().startCollecting()
        runCurrent()
        jobState1.cancel()
        advanceTimeBy(millis = cacheTimeout - 1)
        val jobState2 = subject.listen().startCollecting()
        runCurrent()

        assertEquals(
            listOf(Pending, Success("v1")),
            jobState1.collectedItems
        )
        assertEquals(
            listOf(Success("v1")),
            jobState2.collectedItems
        )
        coVerify(exactly = 1) { spyLoader.invoke(any()) }
    }

    @Test
    fun listen_afterTimeout_startsLoadingFromScratch() = runFlowTest {
        var index = 0
        val loader: ValueLoader<String> = {
            emit("v${++index}")
        }
        val spyLoader = spyk(loader)
        val subject = createLazyFlowSubject(spyLoader)

        val jobState1 = subject.listen().startCollecting()
        runCurrent()
        jobState1.cancel()
        advanceTimeBy(millis = cacheTimeout + 1)
        val jobState2 = subject.listen().startCollecting()
        runCurrent()

        assertEquals(
            listOf(Pending, Success("v1")),
            jobState1.collectedItems
        )
        assertEquals(
            listOf(Pending, Success("v2")),
            jobState2.collectedItems
        )
        coVerify(exactly = 2) { spyLoader.invoke(any()) }
    }

    @Test
    fun listen_whichIsCalledTwice_startsLoadingOnlyOnce() = runFlowTest {
        var index = 0
        val loader: ValueLoader<String> = {
            delay(100)
            emit("v${++index}")
        }
        val spyLoader = spyk(loader)
        val subject = createLazyFlowSubject(spyLoader)

        val collectedItems1 = subject.listen().startCollectingToList()
        advanceTimeBy(50)
        val collectedItems2 = subject.listen().startCollectingToList()
        advanceTimeBy(51)

        coVerify(exactly = 1) { spyLoader.invoke(any()) }
        assertEquals(
            listOf(Pending, Success("v1")),
            collectedItems1
        )
        assertEquals(
            listOf(Pending, Success("v1")),
            collectedItems2
        )
    }

    @Test
    fun listen_whichIsCalledTwiceOnSameInstance_startsLoadingOnlyOnce() = runFlowTest {
        var index = 0
        val loader: ValueLoader<String> = {
            delay(100)
            emit("v${++index}")
        }
        val spyLoader = spyk(loader)
        val subject = createLazyFlowSubject(spyLoader)

        val flow = subject.listen()
        val collectedItems1 = flow.startCollectingToList()
        advanceTimeBy(50)
        val collectedItems2 = flow.startCollectingToList()
        advanceTimeBy(51)

        coVerify(exactly = 1) { spyLoader.invoke(any()) }
        assertEquals(
            listOf(Pending, Success("v1")),
            collectedItems1
        )
        assertEquals(
            listOf(Pending, Success("v1")),
            collectedItems2
        )
    }

    @Test
    fun listen_withCancellationOfNotAllCollectors_holdsCachedValue() = runFlowTest {
        var index = 0
        val loader: ValueLoader<String> = {
            emit("v${++index}")
        }
        val spyLoader = spyk(loader)
        val subject = createLazyFlowSubject(spyLoader)

        val collectState1 = subject.listen().startCollecting()
        runCurrent()
        val collectState2 = subject.listen().startCollecting()
        runCurrent()
        collectState2.cancel()
        val collectState3 = subject.listen().startCollecting()
        runCurrent()

        coVerify(exactly = 1) { spyLoader.invoke(any()) }
        assertEquals(
            listOf(Pending, Success("v1")),
            collectState1.collectedItems
        )
        assertEquals(
            listOf(Success("v1")),
            collectState2.collectedItems
        )
        assertEquals(
            listOf(Success("v1")),
            collectState3.collectedItems
        )
    }

    @Test
    fun newLoad_startsNewLoadWithPendingStatus() = runFlowTest {
        val loader1: ValueLoader<String> = { emit("111") }
        val loader2: ValueLoader<String> = {
            delay(100)
            emit("222")
        }
        val spyLoader2 = spyk(loader2)
        val subject = createLazyFlowSubject(loader1)

        val collectedItems = subject.listen().startCollectingToList()
        runCurrent()
        subject.newLoad(silently = false, spyLoader2)
        runCurrent()

        coVerify(exactly = 1) { spyLoader2.invoke(any()) }
        assertEquals(
            listOf(Pending, Success("111"), Pending),
            collectedItems
        )
    }

    @Test
    fun newLoad_loadsNewValue() = runFlowTest {
        val loader1: ValueLoader<String> = { emit("111") }
        val loader2: ValueLoader<String> = {
            delay(100)
            emit("222")
        }
        val spyLoader2 = spyk(loader2)
        val subject = createLazyFlowSubject(loader1)

        val collectedItems = subject.listen().startCollectingToList()
        runCurrent()
        subject.newLoad(silently = false, spyLoader2)
        runCurrent()
        advanceTimeBy(101)

        coVerify(exactly = 1) { spyLoader2.invoke(any()) }
        assertEquals(
            listOf(Pending, Success("111"), Pending, Success("222")),
            collectedItems
        )
    }

    @Test
    fun newLoad_withSilentMode_loadsNewValueWithoutPendingStatus() = runFlowTest {
        val loader1: ValueLoader<String> = { emit("111") }
        val loader2: ValueLoader<String> = {
            delay(100)
            emit("222")
        }
        val spyLoader2 = spyk(loader2)
        val subject = createLazyFlowSubject(loader1)

        val collectedItems = subject.listen().startCollectingToList()
        runCurrent()
        subject.newLoad(silently = true, spyLoader2)
        runCurrent()
        advanceTimeBy(101)

        coVerify(exactly = 1) { spyLoader2.invoke(any()) }
        assertEquals(
            listOf(Pending, Success("111"), Success("222")),
            collectedItems
        )
    }

    @Test
    fun newLoad_cancelsPreviousLoad() = runFlowTest {
        val loader1: ValueLoader<String> = {
            delay(100)
            emit("111")
        }
        val loader2: ValueLoader<String> = {
            delay(100)
            emit("222")
        }
        val spyLoader1 = spyk(loader1)
        val spyLoader2 = spyk(loader2)
        val subject = createLazyFlowSubject(spyLoader1)

        val collectedItems = subject.listen().startCollectingToList()
        advanceTimeBy(50)
        subject.newLoad(silently = false, spyLoader2)
        advanceTimeBy(101)

        coVerifyOrder {
            spyLoader1.invoke(any())
            spyLoader2.invoke(any())
        }
        assertEquals(
            listOf(Pending, Success("222")),
            collectedItems
        )
    }

    @Test
    fun newLoad_returnsFlowThatEmitsAllValuesOfNewUpload() = runFlowTest {
        val loader1: ValueLoader<String> = {
            emit("11")
            emit("12")
        }
        val loader2: ValueLoader<String> = {
            emit("21")
            emit("22")
        }
        val loader3: ValueLoader<String> = {
            emit("31")
            emit("32")
        }
        val subject = createLazyFlowSubject(loader1)

        subject.listen().startCollecting()
        runCurrent()
        val state1 = subject.newLoad(silently = false, loader2).startCollecting()
        runCurrent()
        val state2 = subject.newLoad(silently = false, loader3).startCollecting()
        runCurrent()

        assertEquals(listOf("21", "22"), state1.collectedItems)
        assertEquals(listOf("31", "32"), state2.collectedItems)
        assertEquals(JobStatus.Completed, state1.jobStatus)
        assertEquals(JobStatus.Completed, state2.jobStatus)
    }

    @Test
    fun newLoad_withCancelledLoad_returnsFlowThatFailsWithException() = runFlowTest {
        val defaultLoader: ValueLoader<String> = {
            emit("1")
        }
        val loader1: ValueLoader<String> = {
            emit("21")
            delay(100)
            emit("22")
        }
        val loader2: ValueLoader<String> = {
            emit("3")
        }
        val subject = createLazyFlowSubject(defaultLoader)

        subject.listen().startCollecting()
        runCurrent()
        val state1 = subject.newLoad(silently = false, loader1).startCollecting()
        advanceTimeBy(50)
        val state2 = subject.newLoad(silently = false, loader2).startCollecting()
        runCurrent()

        assertEquals(listOf("21"), state1.collectedItems)
        assertEquals(listOf("3"), state2.collectedItems)
        assertTrue(state1.jobStatus is JobStatus.Cancelled)
        assertEquals(JobStatus.Completed, state2.jobStatus)
    }

    @Test
    fun newLoad_withFailedLoad_returnsFlowThatAlsoFails() = runFlowTest {
        val defaultLoader: ValueLoader<String> = {
            emit("1")
        }
        val loader1: ValueLoader<String> = {
            emit("2")
            throw IllegalArgumentException()
        }
        val subject = createLazyFlowSubject(defaultLoader)

        subject.listen().startCollecting()
        runCurrent()
        val state = subject.newLoad(silently = false, loader1).startCollecting()
        runCurrent()

        assertEquals(listOf("2"), state.collectedItems)
        assertTrue((state.jobStatus as JobStatus.Failed).error is IllegalArgumentException)
    }

    @Test
    fun newLoad_withFailedLoad_emitsErrorToFlowReturnedByListenMethod() = runFlowTest {
        val loader1: ValueLoader<String> = { delay(100) }
        val loader2: ValueLoader<String> = {
            delay(10)
            emit("222")
            delay(10)
            throw IllegalArgumentException()
        }
        val subject = createLazyFlowSubject(loader1)

        val collectedItems = subject.listen().startCollectingToList()
        runCurrent()
        subject.newLoad(silently = false, loader2)
        advanceTimeBy(11)

        assertEquals(
            listOf(Pending, Success("222")),
            collectedItems
        )
        advanceTimeBy(10)
        assertEquals(3, collectedItems.size)
        assertTrue((collectedItems[2] as Error).exception is IllegalArgumentException)
    }

    @Test
    fun newLoad_afterNewLoad_usesLastLoader() = runFlowTest {
        val loader1: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val loader2: ValueLoader<String> = {
            delay(10)
            emit("222")
        }
        val subject = createLazyFlowSubject(loader1)

        val state1 = subject.listen().startCollecting()
        advanceTimeBy(11)
        subject.newLoad(valueLoader = loader2)
        advanceTimeBy(11)
        state1.cancel()
        advanceTimeBy(cacheTimeout + 1)
        val state2 = subject.listen().startCollecting()
        advanceTimeBy(11)

        assertEquals(
            listOf(Pending, Success("111"), Pending, Success("222")),
            state1.collectedItems
        )
        assertEquals(
            listOf(Pending, Success("222")),
            state2.collectedItems
        )
    }

    @Test(expected = None::class)
    fun reload_withoutPrevLoader_doesNothing() = runFlowTest {
        val subject = createLazyFlowSubject()

        val state = subject.listen().startCollecting()
        runCurrent()
        subject.reload()
        runCurrent()

        assertEquals(
            listOf(Pending),
            state.collectedItems,
        )
    }

    @Test
    fun reload_withPrevLoader_executesLoaderAgain() = runFlowTest {
        val subject = createLazyFlowSubject {
            if (loadTrigger == LoadTrigger.NewLoad) {
                emit("load")
            } else if (loadTrigger == LoadTrigger.Reload) {
                delay(10)
                emit("reload")
            }
        }

        val state = subject.listen().startCollecting()
        runCurrent()
        subject.reload()
        advanceTimeBy(11)

        assertEquals(
            listOf(Pending, Success("load"), Pending, Success("reload")),
            state.collectedItems,
        )
    }

    @Test
    fun updateWith_cancelsLoadingAndEmitsNewValueImmediately() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollectingToList()
        advanceTimeBy(6)
        subject.updateWith(Success("222"))
        advanceTimeBy(6)

        assertEquals(
            listOf(Pending, Success("222")),
            collectedItems,
        )
    }

    @Test
    fun updateWith_emitsNewValue() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollectingToList()
        advanceTimeBy(11)
        subject.updateWith(Success("222"))
        runCurrent()

        assertEquals(
            listOf(Pending, Success("111"), Success("222")),
            collectedItems,
        )
    }

    @Test
    fun updateWithMapper_cancelsLoadingAndEmitsNewValueImmediately() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollectingToList()
        advanceTimeBy(6)
        subject.updateWith { Success("222") }
        advanceTimeBy(6)

        assertEquals(
            listOf(Pending, Success("222")),
            collectedItems,
        )
    }

    @Test
    fun updateWithMapper_emitsNewValue() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollectingToList()
        advanceTimeBy(11)
        subject.updateWith { Success("222") }
        runCurrent()

        assertEquals(
            listOf(Pending, Success("111"), Success("222")),
            collectedItems,
        )
    }

    @Test
    fun updateWithMapper_withReturningSameValue_doesNotReEmitIt() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollectingToList()
        advanceTimeBy(11)
        subject.updateWith { it }
        runCurrent()

        assertEquals(
            listOf(Pending, Success("111")),
            collectedItems,
        )
    }

    @Test
    fun updateWith_updatesValueImmediately() = runFlowTest {
        val subject = createLazyFlowSubject()

        subject.listen().startCollecting()
        subject.updateWith(Success("123"))

        assertEquals(Success("123"), subject.currentValue)
    }

    @Test
    fun updateWith_withoutListeners_doesNotUpdateValueImmediately() = runFlowTest {
        val subject = createLazyFlowSubject()

        subject.updateWith(Success("123"))

        assertEquals(Pending, subject.currentValue)
    }

    @Test
    fun activeCollectorsCount_withoutCollectors_returnsZero() = runFlowTest {
        val subject = createLazyFlowSubject()

        assertEquals(0, subject.activeCollectorsCount)
    }

    @Test
    fun activeCollectorsCount_with1ActiveCollectors_returns1() = runFlowTest {
        val loader: ValueLoader<String> = { emit("111") }
        val subject = createLazyFlowSubject(loader)

        subject.listen().startCollecting()

        assertEquals(1, subject.activeCollectorsCount)
    }

    @Test
    fun activeCollectorsCount_with2Collectors_returns2() = runFlowTest {
        val loader: ValueLoader<String> = { emit("111") }
        val subject = createLazyFlowSubject(loader)

        subject.listen().startCollecting()
        subject.listen().startCollecting()

        assertEquals(2, subject.activeCollectorsCount)
    }

    @Test
    fun activeCollectorsCount_with2CollectorsOnSameFlow_returns2() = runFlowTest {
        val loader: ValueLoader<String> = { emit("111") }
        val subject = createLazyFlowSubject(loader)

        val flow = subject.listen()
        flow.startCollecting()
        flow.startCollecting()

        assertEquals(2, subject.activeCollectorsCount)
    }

    @Test
    fun activeCollectorsCount_afterCancellation_decreasesNumberOfCollectors() = runFlowTest {
        val loader: ValueLoader<String> = { emit("111") }
        val subject = createLazyFlowSubject(loader)

        val state1 = subject.listen().startCollecting() // 1 active collector
        val state2 = subject.listen().startCollecting() // 2 active collectors

        state1.cancel() // 1 active collector again
        assertEquals(1, subject.activeCollectorsCount)
        state2.cancel() // 0 active collectors
        assertEquals(0, subject.activeCollectorsCount)
    }

    @Test
    fun isValueLoading_withoutLoader_alwaysReturnsFalse() = runFlowTest {
        val subject = createLazyFlowSubject()

        subject.listen().startCollecting()
        advanceTimeBy(1)

        assertFalse(subject.isValueLoading().value)
    }

    @Test
    fun isValueLoading_withLoaderWhichIsLoadingValue_returnsTrue() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        subject.listen().startCollecting()
        advanceTimeBy(1)

        assertTrue(subject.isValueLoading().value)
    }

    @Test
    fun isValueLoading_withCancelledCollector_returnsFalseImmediately() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val state = subject.listen().startCollecting()
        advanceTimeBy(1)
        state.cancel()

        assertFalse(subject.isValueLoading().value)
    }

    @Test
    fun isValueLoading_withCancelledAndRestoredCollector_returnsTrueImmediately() = runFlowTest {
        val loader: ValueLoader<String> = {
            delay(cacheTimeout * 2)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val state = subject.listen().startCollecting()
        advanceTimeBy(1)
        state.cancel()
        advanceTimeBy(cacheTimeout - 5)
        subject.listen().startCollecting()
        advanceTimeBy(1)

        assertTrue(subject.isValueLoading().value)
    }

    @Test
    fun isValueLoading_isNotAffectedBySilentFlag() = runFlowTest {
        val loadTime = 10L
        val loader: ValueLoader<String> = {
            delay(loadTime)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)
        subject.listen().startCollecting()
        advanceTimeBy(loadTime + 1)

        subject.newAsyncLoad(silently = true, loader)
        advanceTimeBy(1)

        assertTrue(subject.isValueLoading().value)
    }

    @Test
    fun isValueLoading_whenLoadStatusIsChanged_emitsNewValue() = runFlowTest {
        val loadTime = 10L
        val loader: ValueLoader<String> = {
            delay(loadTime)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)
        subject.listen().startCollecting()

        val collectedItems = subject.isValueLoading().startCollectingToList()

        // initial state
        assertFalse(collectedItems.last())
        // loading is started
        advanceTimeBy(1)
        assertTrue(collectedItems.last())
        // loading is finished
        advanceTimeBy(loadTime)
        assertFalse(collectedItems.last())
    }

    private fun FlowTest.createLazyFlowSubject(
        loader: ValueLoader<String>? = null,
    ): LazyFlowSubjectImpl<String> {
        val coroutineScopeFactory = mockk<CoroutineScopeFactory>()

        every { coroutineScopeFactory.createScope() } answers {
            TestScope(testScope().testScheduler)
        }
        return LazyFlowSubjectImpl<String>(
            coroutineScopeFactory,
            cacheTimeout,
        ).apply {
            if (loader != null) {
                newAsyncLoad(silently = false, loader)
            }
        }
    }
}
