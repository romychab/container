package com.elveum.container.subject

import com.elveum.container.BackgroundLoadState
import com.elveum.container.Container
import com.elveum.container.Container.Pending
import com.elveum.container.EmptyMetadata
import com.elveum.container.EmptyReloadFunction
import com.elveum.container.LoadConfig
import com.elveum.container.backgroundLoadState
import com.elveum.container.pendingContainer
import com.elveum.container.reloadFunction
import com.elveum.container.successContainer
import com.elveum.container.utils.invokeOn
import com.elveum.container.utils.raw
import com.uandcode.flowtest.runFlowTest
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class LazyFlowSubjectListenIntegrationTest : AbstractLazyFlowSubjectIntegrationTest() {


    @Test
    fun listen_whileLoadingItems_emitsPendingStatus() = runFlowTest {
        val subject = createLazyFlowSubject {
            delay(1000)
        }

        val collectedItems = subject.listen().startCollecting().collectedItems
        runCurrent()

        assertEquals(
            listOf(Pending),
            collectedItems
        )
    }

    @Test
    fun listen_withEmptyLoader_emitsIllegalStateException() = runFlowTest {
        val subject = createLazyFlowSubject { }

        val collectedItems = subject.listen().startCollecting().collectedItems
        runCurrent()

        assertEquals(2, collectedItems.size)
        assertEquals(Pending, collectedItems[0])
        assertTrue((collectedItems[1] as Container.Error).exception is IllegalStateException)
    }

    @Test
    fun listen_emitsItemsProducedByLoader() = runFlowTest {
        val subject = createLazyFlowSubject {
            emit("111")
            delay(1)
            emit("222")
            delay(100)
            emit("333")
        }

        val collectedItems = subject.listen()
            .startCollecting(StandardTestDispatcher(scope.testScheduler))
            .collectedItems
        advanceTimeBy(2)

        assertEquals(
            listOf(Pending, successContainer("111"), successContainer("222")),
            collectedItems.raw()
        )
        advanceTimeBy(100)
        assertEquals(
            listOf(Pending, successContainer("111"), successContainer("222"), successContainer("333")),
            collectedItems.raw()
        )
    }

    @Test
    fun listen_withFailedLoad_emitsException() = runFlowTest {
        val subject = createLazyFlowSubject {
            emit("111")
            delay(1)
            throw IllegalArgumentException()
        }

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(2)

        assertEquals(3, collectedItems.size)
        assertEquals(Pending, collectedItems[0])
        assertEquals(successContainer("111"), collectedItems[1].raw())
        assertTrue((collectedItems[2] as Container.Error).exception is IllegalArgumentException)
    }

    @Test
    fun listen_withinTimeout_holdsLatestLoadedValue() = runFlowTest {
        var index = 0
        val loader: ValueLoader<String> = ValueLoader {
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
            listOf(pendingContainer(), successContainer("v1")),
            jobState1.collectedItems.raw()
        )
        assertEquals(
            listOf(successContainer("v1")),
            jobState2.collectedItems.raw()
        )
        coVerify(exactly = 1) { spyLoader.invokeOn(any()) }
    }

    @Test
    fun listen_afterTimeout_startsLoadingFromScratch() = runFlowTest {
        var index = 0
        val loader: ValueLoader<String> = ValueLoader {
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
            listOf(pendingContainer(), successContainer("v1")),
            jobState1.collectedItems.raw()
        )
        assertEquals(
            listOf(pendingContainer(), successContainer("v2")),
            jobState2.collectedItems.raw()
        )
        coVerify(exactly = 2) { spyLoader.invokeOn(any()) }
    }

    @Test
    fun listen_whichIsCalledTwice_startsLoadingOnlyOnce() = runFlowTest {
        var index = 0
        var loaderCalls = 0
        val loader: ValueLoader<String> = ValueLoader {
            loaderCalls++
            delay(100)
            emit("v${++index}")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems1 = subject.listen().startCollecting().collectedItems
        advanceTimeBy(50)
        val collectedItems2 = subject.listen().startCollecting().collectedItems
        advanceTimeBy(51)

        assertEquals(1, loaderCalls)
        assertEquals(
            listOf(Pending, successContainer("v1")),
            collectedItems1.raw()
        )
        assertEquals(
            listOf(Pending, successContainer("v1")),
            collectedItems2.raw()
        )
    }

    @Test
    fun listen_whichIsCalledTwiceOnSameInstance_startsLoadingOnlyOnce() = runFlowTest {
        var index = 0
        var loaderCalls = 0
        val loader: ValueLoader<String> = ValueLoader {
            loaderCalls++
            delay(100)
            emit("v${++index}")
        }
        val subject = createLazyFlowSubject(loader)

        val flow = subject.listen()
        val collectedItems1 = flow.startCollecting().collectedItems
        advanceTimeBy(50)
        val collectedItems2 = flow.startCollecting().collectedItems
        advanceTimeBy(51)

        assertEquals(1, loaderCalls)
        assertEquals(
            listOf(Pending, successContainer("v1")),
            collectedItems1.raw()
        )
        assertEquals(
            listOf(Pending, successContainer("v1")),
            collectedItems2.raw()
        )
    }

    @Test
    fun listen_withCancellationOfNotAllCollectors_holdsCachedValue() = runFlowTest {
        var index = 0
        val loader: ValueLoader<String> = ValueLoader {
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

        coVerify(exactly = 1) { spyLoader.invokeOn(any()) }
        assertEquals(
            listOf(Pending, successContainer("v1")),
            collectState1.collectedItems.raw()
        )
        assertEquals(
            listOf(successContainer("v1")),
            collectState2.collectedItems.raw()
        )
        assertEquals(
            listOf(successContainer("v1")),
            collectState3.collectedItems.raw()
        )
    }


    @Test
    fun listen_withEnabledConfiguration_appliesConfiguration() = runFlowTest {
        val subject = createLazyFlowSubject {
            delay(10)
            emit("test")
        }

        val noConfigCollector = subject.listen().startCollecting()
        val reloadFunctionEnabledCollector = subject.listen(
            ContainerConfiguration(emitReloadFunction = true)
        ).startCollecting()
        val bgLoadsEnabledCollector = subject.listen(
            ContainerConfiguration(emitBackgroundLoads = true)
        ).startCollecting()
        val allConfigEnabledCollector = subject.listenReloadable().startCollecting()
        advanceTimeBy(11)

        // 1. Assert no reload function in noConfig & bgLoadsConfig:
        assertEquals(EmptyReloadFunction, noConfigCollector.lastItem.metadata.reloadFunction)
        assertEquals(EmptyReloadFunction, bgLoadsEnabledCollector.lastItem.metadata.reloadFunction)

        // 2. Assert reload function exists in reloadFunConfig
        reloadFunctionEnabledCollector.lastItem.metadata.reloadFunction.invoke(LoadConfig.Normal, EmptyMetadata)
        advanceTimeBy(10)
        assertEquals(pendingContainer(), reloadFunctionEnabledCollector.lastItem)
        advanceTimeBy(1)

        // 3. Assert reload function exists in allConfig
        allConfigEnabledCollector.lastItem.metadata.reloadFunction.invoke(LoadConfig.Normal, EmptyMetadata)
        advanceTimeBy(10)
        assertEquals(pendingContainer(), allConfigEnabledCollector.lastItem)
        advanceTimeBy(1)

        // 4. Assert background loading status in all containers
        subject.reloadAsync(config = LoadConfig.SilentLoading)
        advanceTimeBy(10)
        assertEquals(BackgroundLoadState.Idle, noConfigCollector.lastItem.metadata.backgroundLoadState)
        assertEquals(BackgroundLoadState.Idle, reloadFunctionEnabledCollector.lastItem.metadata.backgroundLoadState)
        assertEquals(BackgroundLoadState.Loading, bgLoadsEnabledCollector.lastItem.metadata.backgroundLoadState)
        assertEquals(BackgroundLoadState.Loading, allConfigEnabledCollector.lastItem.metadata.backgroundLoadState)
    }


}