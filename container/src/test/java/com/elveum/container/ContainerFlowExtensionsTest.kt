package com.elveum.container

import com.uandcode.flowtest.runFlowTest
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerFlowExtensionsTest {

    @Test
    fun test_containerMap() = runFlowTest {
        val inputFlow = MutableStateFlow<Container<String>>(pendingContainer())

        val collectedItems = inputFlow
            .containerMap {
                "mapped-$it"
            }
            .startCollecting()

        // map pending
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // map error
        val expectedError = errorContainer(
            IllegalStateException(),
            RemoteSourceType,
            isLoadingInBackground = true,
            reloadFunction = mockk(),
        )
        inputFlow.value = expectedError
        assertEquals(expectedError, collectedItems.lastItem)
        // map success
        val reloadFunction = mockk<ReloadFunction>()
        val inputSuccess = successContainer(
            "value",
            FakeSourceType,
            isLoadingInBackground = true,
            reloadFunction = reloadFunction,
        )
        val expectedSuccess = successContainer(
            "mapped-value",
            FakeSourceType,
            isLoadingInBackground = true,
            reloadFunction = reloadFunction,
        )
        inputFlow.value = inputSuccess
        assertEquals(expectedSuccess, collectedItems.lastItem)
    }

    @Test
    fun test_containerStateMap() = runFlowTest {
        val inputFlow = MutableStateFlow<Container<String>>(pendingContainer())

        val outputStateFlow = inputFlow
            .containerStateMap {
                "mapped-$it"
            }
        val collectedItems = outputStateFlow.startCollecting()

        // map pending
        assertEquals(pendingContainer(), collectedItems.lastItem)
        assertEquals(pendingContainer(), outputStateFlow.value)
        // map error
        val expectedError = errorContainer(
            IllegalStateException(),
            RemoteSourceType,
            isLoadingInBackground = true,
            reloadFunction = mockk(),
        )
        inputFlow.value = expectedError
        assertEquals(expectedError, collectedItems.lastItem)
        assertEquals(expectedError, outputStateFlow.value)
        // map success
        val reloadFunction = mockk<ReloadFunction>()
        val inputSuccess = successContainer(
            "value",
            FakeSourceType,
            isLoadingInBackground = true,
            reloadFunction = reloadFunction,
        )
        val expectedSuccess = successContainer(
            "mapped-value",
            FakeSourceType,
            isLoadingInBackground = true,
            reloadFunction = reloadFunction,
        )
        inputFlow.value = inputSuccess
        assertEquals(expectedSuccess, collectedItems.lastItem)
        assertEquals(expectedSuccess, outputStateFlow.value)
    }

    @Test
    fun test_containerMapLatest() = runFlowTest {
        val inputFlow = MutableStateFlow<Container<String>>(pendingContainer())

        val collectedItems = inputFlow
            .containerMapLatest {
                "mapped-$it"
            }
            .startCollecting()

        // map pending
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // map error
        val expectedError = errorContainer(
            IllegalStateException(),
            RemoteSourceType,
            isLoadingInBackground = true,
            reloadFunction = mockk(),
        )
        inputFlow.value = expectedError
        assertEquals(expectedError, collectedItems.lastItem)
        // map success
        val reloadFunction = mockk<ReloadFunction>()
        val inputSuccess = successContainer(
            "value",
            FakeSourceType,
            isLoadingInBackground = true,
            reloadFunction = reloadFunction,
        )
        val expectedSuccess = successContainer(
            "mapped-value",
            FakeSourceType,
            isLoadingInBackground = true,
            reloadFunction = reloadFunction,
        )
        inputFlow.value = inputSuccess
        assertEquals(expectedSuccess, collectedItems.lastItem)
    }

    @Test
    fun test_containerMapLatest_withCancellation() = runFlowTest {
        val inputFlow = MutableStateFlow<Container<String>>(pendingContainer())

        val outputFlow = inputFlow.containerMapLatest {
            delay(100)
            "mapped-$it"
        }
        val collectedItems = outputFlow.startCollecting(
            StandardTestDispatcher(scope.testScheduler)
        )

        // initial state
        runCurrent()
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // new value (not cancelled)
        inputFlow.value = successContainer("v1")
        advanceTimeBy(99) // almost mapped
        assertEquals(pendingContainer(), collectedItems.lastItem)
        advanceTimeBy(2) // mapping completed
        assertEquals(successContainer("mapped-v1"), collectedItems.lastItem)
        // new value (cancelled)
        inputFlow.value = successContainer("v2")
        advanceTimeBy(99) // almost mapped
        assertEquals(successContainer("mapped-v1"), collectedItems.lastItem)
        inputFlow.value = successContainer("v3") // not waiting for completion, emit a new value
        advanceTimeBy(101) // mapping completed
        assertEquals(successContainer("mapped-v3"), collectedItems.lastItem)

        assertEquals(
            listOf(
                pendingContainer(),
                successContainer("mapped-v1"),
                successContainer("mapped-v3"),
            ),
            collectedItems.collectedItems,
        )
    }

    @Test
    fun test_containerFilter() = runFlowTest {
        val inputFlow = MutableStateFlow<Container<Int>>(pendingContainer())

        val collectedItems = inputFlow
            .containerFilter {
                it % 2 == 0
            }
            .startCollecting()

        // pending -> collected
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // error -> collected
        val exception = IllegalStateException()
        inputFlow.value = errorContainer(exception)
        assertTrue(collectedItems.lastItem.exceptionOrNull() is IllegalStateException)
        // success (filtered) -> collected
        inputFlow.value = successContainer(2)
        assertEquals(successContainer(2), collectedItems.lastItem)
        // success (not filtered) -> dropped
        inputFlow.value = successContainer(3)
        assertEquals(successContainer(2), collectedItems.lastItem)
        // assert all collected items
        assertEquals(
            listOf(
                pendingContainer(),
                errorContainer(exception),
                successContainer(2)
            ),
            collectedItems.collectedItems,
        )
    }

    @Test
    fun test_containerFilterNot() = runFlowTest {
        val inputFlow = MutableStateFlow<Container<Int>>(pendingContainer())

        val collectedItems = inputFlow
            .containerFilterNot {
                it % 2 == 0
            }
            .startCollecting()

        // pending -> collected
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // error -> collected
        val exception = IllegalStateException()
        inputFlow.value = errorContainer(exception)
        assertTrue(collectedItems.lastItem.exceptionOrNull() is IllegalStateException)
        // success (not filtered) -> dropped
        inputFlow.value = successContainer(2)
        assertEquals(errorContainer(exception), collectedItems.lastItem)
        // success (filtered) -> collected
        inputFlow.value = successContainer(3)
        assertEquals(successContainer(3), collectedItems.lastItem)
        // assert all collected items
        assertEquals(
            listOf(
                pendingContainer(),
                errorContainer(exception),
                successContainer(3)
            ),
            collectedItems.collectedItems,
        )
    }

    @Test
    fun test_containerUpdate() = runFlowTest {
        val reloadFunction = mockk<ReloadFunction>()
        val inputFlow = MutableStateFlow<Container<Int>>(successContainer(0))

        val updatedSource = inputFlow
            .containerUpdate(source = RemoteSourceType)
            .startCollecting()
        val updatedIsLoading = inputFlow
            .containerUpdate(isLoadingInBackground = true)
            .startCollecting()
        val updatedReloadFunction = inputFlow
            .containerUpdate(reloadFunction = reloadFunction)
            .startCollecting()

        assertEquals(
            successContainer(0, RemoteSourceType, isLoadingInBackground = false, EmptyReloadFunction),
            updatedSource.lastItem
        )
        assertEquals(
            successContainer(0, UnknownSourceType, isLoadingInBackground = true, EmptyReloadFunction),
            updatedIsLoading.lastItem
        )
        assertEquals(
            successContainer(0, UnknownSourceType, isLoadingInBackground = false, reloadFunction),
            updatedReloadFunction.lastItem
        )
    }

    @Test
    fun test_containerUpdate_withBlock()  = runFlowTest {
        val expectedValue = "value"
        val expectedException = IllegalStateException()
        val initialIsLoading = true
        val expectedIsLoading = false
        val initialReloadFunction: ReloadFunction = mockk()
        val expectedReloadFunction: ReloadFunction = mockk()
        val initialSource = LocalSourceType
        val expectedSource = RemoteSourceType
        val inputFlow = MutableSharedFlow<Container<String>>()

        val outputFlow = inputFlow
            .containerUpdate {
                assertEquals(initialIsLoading, isLoadingInBackground)
                assertSame(initialReloadFunction, reloadFunction)
                assertSame(initialSource, source)
                isLoadingInBackground = expectedIsLoading
                reloadFunction = expectedReloadFunction
                source = expectedSource
            }
        val collectedItems = outputFlow.startCollecting()

        inputFlow.emit(successContainer(expectedValue, initialSource, initialIsLoading, initialReloadFunction))
        val container1 = collectedItems.lastItem as Container.Success<String>
        assertEquals(expectedValue, container1.value)
        assertEquals(expectedIsLoading, container1.isLoadingInBackground)
        assertEquals(expectedSource, container1.source)
        assertSame(expectedReloadFunction, container1.reloadFunction)

        inputFlow.emit(errorContainer(expectedException, initialSource, initialIsLoading, initialReloadFunction))
        val container2 = collectedItems.lastItem as Container.Error
        assertEquals(expectedException, container2.exception)
        assertEquals(expectedIsLoading, container2.isLoadingInBackground)
        assertEquals(expectedSource, container2.source)
        assertSame(expectedReloadFunction, container2.reloadFunction)
    }

}