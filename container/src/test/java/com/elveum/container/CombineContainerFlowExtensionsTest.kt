package com.elveum.container

import com.uandcode.flowtest.runFlowTest
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombineContainerFlowExtensionsTest {

    @Test
    fun `test combineContainerFlows for 2 flows`() = runFlowTest {
        val flowA = MutableStateFlow<Container<String>>(pendingContainer())
        val flowB = MutableStateFlow<Container<String>>(pendingContainer())

        val resultFlow = combineContainerFlows(flowA, flowB) { v1, v2 ->
            "$v1-$v2"
        }
        val collectedItems = resultFlow.startCollecting()

        // initial combined value => pending
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // first success value => pending
        flowA.value = successContainer("a1")
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // second success value => success
        flowB.value = successContainer("b1")
        assertEquals(successContainer("a1-b1"), collectedItems.lastItem)
        // emit new value to 1st flow
        flowA.value = successContainer("a2")
        assertEquals(successContainer("a2-b1"), collectedItems.lastItem)
        // emit new value to 2nd flow
        flowB.value = successContainer("b2")
        assertEquals(successContainer("a2-b2"), collectedItems.lastItem)
        // emit error to one flow -> combined container must be error
        flowB.value = errorContainer(IllegalStateException())
        assertTrue(collectedItems.lastItem.exceptionOrNull() is IllegalStateException)
        // emit success instead of error
        flowB.value = successContainer("b3")
        assertEquals(successContainer("a2-b3"), collectedItems.lastItem)
    }

    @Test
    fun `test combineContainerFlows for 3 flows`() = runFlowTest {
        val flowA = MutableStateFlow<Container<String>>(pendingContainer())
        val flowB = MutableStateFlow<Container<String>>(pendingContainer())
        val flowC = MutableStateFlow<Container<String>>(pendingContainer())

        val resultFlow = combineContainerFlows(flowA, flowB, flowC) { v1, v2, v3 ->
            "$v1-$v2-$v3"
        }
        val collectedItems = resultFlow.startCollecting()

        // initial combined value => pending
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // first success value => pending
        flowA.value = successContainer("a1")
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // second success value => pending
        flowB.value = successContainer("b1")
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // third success value => success
        flowC.value = successContainer("c1")
        assertEquals(successContainer("a1-b1-c1"), collectedItems.lastItem)
        // emit new value to 1st flow
        flowA.value = successContainer("a2")
        assertEquals(successContainer("a2-b1-c1"), collectedItems.lastItem)
        // emit new value to 2nd flow
        flowB.value = successContainer("b2")
        assertEquals(successContainer("a2-b2-c1"), collectedItems.lastItem)
        // emit new value to 3rd flow
        flowC.value = successContainer("c2")
        assertEquals(successContainer("a2-b2-c2"), collectedItems.lastItem)
        // emit error to one flow -> combined container must be error
        flowC.value = errorContainer(IllegalStateException())
        assertTrue(collectedItems.lastItem.exceptionOrNull() is IllegalStateException)
        // emit success instead of error
        flowC.value = successContainer("c3")
        assertEquals(successContainer("a2-b2-c3"), collectedItems.lastItem)
    }

    @Test
    fun `test combineContainerFlows for 4 flows`() = runFlowTest {
        val flowA = MutableStateFlow<Container<String>>(pendingContainer())
        val flowB = MutableStateFlow<Container<String>>(pendingContainer())
        val flowC = MutableStateFlow<Container<String>>(pendingContainer())
        val flowD = MutableStateFlow<Container<String>>(pendingContainer())

        val resultFlow = combineContainerFlows(flowA, flowB, flowC, flowD) { v1, v2, v3, v4 ->
            "$v1-$v2-$v3-$v4"
        }
        val collectedItems = resultFlow.startCollecting()

        // initial combined value => pending
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // first success value => pending
        flowA.value = successContainer("a1")
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // second success value => pending
        flowB.value = successContainer("b1")
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // third success value => pending
        flowC.value = successContainer("c1")
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // fourth success value => success
        flowD.value = successContainer("d1")
        assertEquals(successContainer("a1-b1-c1-d1"), collectedItems.lastItem)
        // emit new value to 1st flow
        flowA.value = successContainer("a2")
        assertEquals(successContainer("a2-b1-c1-d1"), collectedItems.lastItem)
        // emit new value to 2nd flow
        flowB.value = successContainer("b2")
        assertEquals(successContainer("a2-b2-c1-d1"), collectedItems.lastItem)
        // emit new value to 3rd flow
        flowC.value = successContainer("c2")
        assertEquals(successContainer("a2-b2-c2-d1"), collectedItems.lastItem)
        // emit new value to 4th flow
        flowD.value = successContainer("d2")
        assertEquals(successContainer("a2-b2-c2-d2"), collectedItems.lastItem)
        // emit error to one flow -> combined container must be error
        flowD.value = errorContainer(IllegalStateException())
        assertTrue(collectedItems.lastItem.exceptionOrNull() is IllegalStateException)
        // emit success instead of error
        flowD.value = successContainer("d3")
        assertEquals(successContainer("a2-b2-c2-d3"), collectedItems.lastItem)
    }

    @Test
    fun `test combineContainerFlows for 5 flows`() = runFlowTest {
        val flowA = MutableStateFlow<Container<String>>(pendingContainer())
        val flowB = MutableStateFlow<Container<String>>(pendingContainer())
        val flowC = MutableStateFlow<Container<String>>(pendingContainer())
        val flowD = MutableStateFlow<Container<String>>(pendingContainer())
        val flowE = MutableStateFlow<Container<String>>(pendingContainer())

        val resultFlow = combineContainerFlows(flowA, flowB, flowC, flowD, flowE) { v1, v2, v3, v4, v5 ->
            "$v1-$v2-$v3-$v4-$v5"
        }
        val collectedItems = resultFlow.startCollecting()

        // initial combined value => pending
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // first success value => pending
        flowA.value = successContainer("a1")
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // second success value => pending
        flowB.value = successContainer("b1")
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // third success value => pending
        flowC.value = successContainer("c1")
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // fourth success value=> pending
        flowD.value = successContainer("d1")
        assertEquals(pendingContainer(), collectedItems.lastItem)
        // fifth success value => success
        flowE.value = successContainer("e1")
        assertEquals(successContainer("a1-b1-c1-d1-e1"), collectedItems.lastItem)
        // emit new value to 1st flow
        flowA.value = successContainer("a2")
        assertEquals(successContainer("a2-b1-c1-d1-e1"), collectedItems.lastItem)
        // emit new value to 2nd flow
        flowB.value = successContainer("b2")
        assertEquals(successContainer("a2-b2-c1-d1-e1"), collectedItems.lastItem)
        // emit new value to 3rd flow
        flowC.value = successContainer("c2")
        assertEquals(successContainer("a2-b2-c2-d1-e1"), collectedItems.lastItem)
        // emit new value to 4th flow
        flowD.value = successContainer("d2")
        assertEquals(successContainer("a2-b2-c2-d2-e1"), collectedItems.lastItem)
        // emit new value to 5th flow
        flowE.value = successContainer("e2")
        assertEquals(successContainer("a2-b2-c2-d2-e2"), collectedItems.lastItem)
        // emit error to one flow -> combined container must be error
        flowE.value = errorContainer(IllegalStateException())
        assertTrue(collectedItems.lastItem.exceptionOrNull() is IllegalStateException)
        // emit success instead of error
        flowE.value = successContainer("e3")
        assertEquals(successContainer("a2-b2-c2-d2-e3"), collectedItems.lastItem)
    }

    @Test
    fun `combineContainerFlows uses sourceType from first flow`() = runFlowTest {
        val flowA = MutableStateFlow<Container<String>>(
            successContainer("1", RemoteSourceType)
        )
        val flowB = MutableStateFlow<Container<String>>(
            successContainer("2", LocalSourceType)
        )

        val resultFlow = combineContainerFlows(listOf(flowA, flowB)) {
            it.joinToString("-")
        }
        val collectedItems = resultFlow.startCollecting()

        assertEquals(
            RemoteSourceType,
            (collectedItems.lastItem as Container.Success).source,
        )
    }

    @Test
    fun `combineContainerFlows emits isLoading if at least one container is loading`() = runFlowTest {
        val flowA = MutableStateFlow<Container<String>>(
            successContainer("1")
        )
        val flowB = MutableStateFlow<Container<String>>(
            successContainer("2")
        )

        val resultFlow = combineContainerFlows(listOf(flowA, flowB)) {
            it.joinToString("-")
        }
        val collectedItems = resultFlow.startCollecting()

        // no loading
        assertFalse((collectedItems.lastItem as Container.Success).isLoadingInBackground)
        // emit any loading
        flowB.value = successContainer("2", isLoadingInBackground = true)
        assertTrue((collectedItems.lastItem as Container.Success).isLoadingInBackground)
        // reset loading
        flowB.value = successContainer("2", isLoadingInBackground = false)
        assertFalse((collectedItems.lastItem as Container.Success).isLoadingInBackground)
    }

    @Test
    fun `combineContainerFlows merges reload functions`() = runFlowTest {
        val reloadA = mockk<ReloadFunction>(relaxed = true)
        val reloadB = mockk<ReloadFunction>(relaxed = true)
        val flowA = MutableStateFlow<Container<String>>(
            successContainer("1", reloadFunction = reloadA)
        )
        val flowB = MutableStateFlow<Container<String>>(
            successContainer("2", reloadFunction = reloadB)
        )

        val resultFlow = combineContainerFlows(listOf(flowA, flowB)) {
            it.joinToString("-")
        }
        val collectedItems = resultFlow.startCollecting()
        (collectedItems.lastItem as Container.Success).reloadFunction(true)

        verify(exactly = 1) {
            reloadA(true)
            reloadB(true)
        }
    }

    @Test
    fun `combineContainerFlows with failed mapping emits error`() = runFlowTest {
        val flowA = MutableStateFlow<Container<String>>(
            successContainer("1")
        )
        val flowB = MutableStateFlow<Container<String>>(
            successContainer("2")
        )

        val resultFlow = combineContainerFlows(listOf(flowA, flowB)) { list ->
            if (list.any { it.toString().toIntOrNull() == null }) {
                throw IllegalArgumentException()
            }
            list.joinToString("-")
        }
        val collectedItems = resultFlow.startCollecting()

        flowB.value = successContainer("test")
        assertTrue(
            collectedItems.lastItem.exceptionOrNull() is IllegalArgumentException
        )
    }

}