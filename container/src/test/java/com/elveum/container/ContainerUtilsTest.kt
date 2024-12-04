@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container

import com.elveum.container.utils.JobStatus
import com.elveum.container.utils.runFlowTest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Test

class ContainerUtilsTest {

    @Test
    fun containerOf_whileLoading_emitsPending() = runFlowTest {
        val flow = containerOf {
            delay(1000)
        }

        val collectedItems = flow.startCollectingToList(unconfined = false)
        runCurrent()

        assertEquals(listOf(Container.Pending), collectedItems)
    }

    @Test
    fun containerOf_withSuccess_emitsSuccessAndCompletes() = runFlowTest {
        val flow = containerOf(LocalSourceType) {
            delay(1000)
            "test"
        }

        val collectState = flow.startCollecting(unconfined = false)
        advanceTimeBy(1001)

        assertEquals(
            listOf(Container.Pending, Container.Success("test", LocalSourceType)),
            collectState.collectedItems
        )
        assertEquals(JobStatus.Completed, collectState.jobStatus)
    }

    @Test
    fun containerOf_withError_emitsErrorAndCompletes() = runFlowTest {
        val expectedException = Exception()
        val flow = containerOf(LocalSourceType) {
            delay(1000)
            throw expectedException
        }

        val collectState = flow.startCollecting(unconfined = false)
        advanceTimeBy(1001)

        assertEquals(2, collectState.collectedItems.size)
        assertEquals(Container.Pending, collectState.collectedItems[0])
        assertSame(expectedException, (collectState.collectedItems[1] as Container.Error).exception)
        assertEquals(JobStatus.Completed, collectState.jobStatus)
    }

    @Test
    fun containerOf_isColdFlow() = runFlowTest {
        val loader = mockk<suspend () -> String>(relaxed = true)
        val flow = containerOf(LocalSourceType, loader)

        verify { loader wasNot called }
        flow.startCollecting()
        coVerify(exactly = 1) { loader.invoke() }
    }

    @Test
    fun containerOf_withTwoCollectors_loadsDataTwice() = runFlowTest {
        val loader = mockk<suspend () -> String>(relaxed = true)
        coEvery { loader.invoke() } returns "111" andThen "222"
        val flow = containerOf {
            delay(1000)
            loader()
        }

        val collectedItems1 = flow.startCollectingToList(unconfined = false)
        val collectedItems2 = flow.startCollectingToList(unconfined = false)
        advanceTimeBy(10001)

        coVerify(exactly = 2) { loader.invoke() }
        assertEquals(
            listOf(Container.Pending, Container.Success("111")),
            collectedItems1
        )
        assertEquals(
            listOf(Container.Pending, Container.Success("222")),
            collectedItems2
        )
    }

    // ---------

    @Test
    fun containerOfMany_whileLoading_emitsPending() = runFlowTest {
        val flow = containerOfMany<String> {
            delay(1000)
        }

        val collectedItems = flow.startCollectingToList(unconfined = false)
        runCurrent()

        assertEquals(listOf(Container.Pending), collectedItems)
    }

    @Test
    fun containerOfMany_withEmitCall_emitsSuccessAndDoesNotCompletes() = runFlowTest {
        val flow = containerOfMany {
            delay(1000)
            emit("111", LocalSourceType)
            delay(1000)
        }

        val collectState = flow.startCollecting(unconfined = false)
        advanceTimeBy(1001)

        assertEquals(
            listOf(Container.Pending, Container.Success("111", LocalSourceType)),
            collectState.collectedItems
        )
        assertEquals(JobStatus.Collecting, collectState.jobStatus)
    }

    @Test
    fun containerOfMany_afterLoaderFinish_completes() = runFlowTest {
        val flow = containerOfMany {
            delay(1000)
            emit("111", LocalSourceType)
            delay(1000)
            emit("222", RemoteSourceType)
            delay(1000)
        }

        val collectState = flow.startCollecting(unconfined = false)

        // assert 1
        advanceTimeBy(1001)
        assertEquals(
            listOf(Container.Pending, Container.Success("111", LocalSourceType)),
            collectState.collectedItems
        )
        assertEquals(JobStatus.Collecting, collectState.jobStatus)
        // assert 2
        advanceTimeBy(1000)
        assertEquals(
            listOf(Container.Pending, Container.Success("111", LocalSourceType), Container.Success("222", RemoteSourceType)),
            collectState.collectedItems
        )
        assertEquals(JobStatus.Collecting, collectState.jobStatus)
        // assert 3
        advanceTimeBy(1000)
        assertEquals(
            listOf(Container.Pending, Container.Success("111", LocalSourceType), Container.Success("222", RemoteSourceType)),
            collectState.collectedItems
        )
        assertEquals(JobStatus.Completed, collectState.jobStatus)
    }

    @Test
    fun containerOfMany_withException_emitsErrorAndCompletes() = runFlowTest {
        val expectedException = Exception()
        val flow = containerOfMany<String> {
            delay(1000)
            throw expectedException
        }

        val collectState = flow.startCollecting(unconfined = false)
        advanceTimeBy(1001)

        assertEquals(2, collectState.collectedItems.size)
        assertEquals(Container.Pending, collectState.collectedItems[0])
        assertSame(expectedException, (collectState.collectedItems[1] as Container.Error).exception)
        assertEquals(JobStatus.Completed, collectState.jobStatus)
    }

    @Test
    fun containerOfMany_withEmittedValuesAndException_emitsAllValuesAndErrorAndThenCompletes() = runFlowTest {
        val expectedException = Exception()
        val flow = containerOfMany {
            delay(1000)
            emit("111", LocalSourceType)
            delay(1000)
            throw expectedException
        }

        val collectState = flow.startCollecting(unconfined = false)

        // assert 1
        advanceTimeBy(1001)
        assertEquals(
            listOf(Container.Pending, Container.Success("111", LocalSourceType)),
            collectState.collectedItems
        )
        assertEquals(JobStatus.Collecting, collectState.jobStatus)
        // assert 2
        advanceTimeBy(1000)
        assertEquals(3, collectState.collectedItems.size)
        assertEquals(Container.Pending, collectState.collectedItems[0])
        assertEquals(Container.Success("111", LocalSourceType), collectState.collectedItems[1])
        assertSame(expectedException, (collectState.collectedItems[2] as Container.Error).exception)
        assertEquals(JobStatus.Completed, collectState.jobStatus)
    }

    @Test
    fun containerOfMany_isColdFlow() = runFlowTest {
        var isCalled = false
        val flow = containerOfMany {
            isCalled = true
            delay(1000)
            emit("test")
        }

        assertFalse(isCalled)
        flow.startCollectingToList()
        assertTrue(isCalled)
    }

    @Test
    fun containerOfMany_withTwoCollectors_loadsDataTwice() = runFlowTest {
        var iteration = 0
        val flow = containerOfMany {
            val i = ++iteration
            delay(1000)
            emit("test${i}_1")
            emit("test${i}_2")
        }

        val collectedItems1 = flow.startCollectingToList(unconfined = false)
        val collectedItems2 = flow.startCollectingToList(unconfined = false)
        advanceTimeBy(10001)

        assertEquals(
            listOf(Container.Pending, Container.Success("test1_1"), Container.Success("test1_2"), ),
            collectedItems1
        )
        assertEquals(
            listOf(Container.Pending, Container.Success("test2_1"), Container.Success("test2_2"), ),
            collectedItems2
        )
    }
}