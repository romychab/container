package com.elveum.container

import com.elveum.container.utils.runFlowTest
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class StateFlowExtensionsTest {

    @Test
    fun public_castsTypeToStateFlow() {
        val mutableStateFlow = MutableStateFlow("test")
        val stateFlow = mutableStateFlow.public()
        assertSame(mutableStateFlow, stateFlow)
    }

    @Test
    fun tryUpdate_withMutableStateFlow_updatesValue() {
        val flow: StateFlow<Int> = MutableStateFlow(value = 1)
        flow.tryUpdate(2)
        assertEquals(2, flow.value)
    }

    @Test
    fun tryUpdate_withNonMutableStateFlow_doesNothing() {
        val flow: StateFlow<Int> = MutableStateFlow(value = 1).asStateFlow()
        flow.tryUpdate(2)
        assertEquals(1, flow.value)
    }

    @Test
    fun tryUpdate_withMutableStateFlow_callsUpdater() {
        val flow: StateFlow<Int> = MutableStateFlow(value = 1)
        val updater = mockk<(Int) -> Int>()
        every { updater(1) } returns 2

        flow.tryUpdate(updater)

        assertEquals(2, flow.value)
        verify(exactly = 1) {
            updater(1)
        }
    }

    @Test
    fun tryUpdate_withNonMutableStateFlow_doesNotCallUpdater() {
        val flow: StateFlow<Int> = MutableStateFlow(value = 1).asStateFlow()
        val updater = mockk<(Int) -> Int>()

        flow.tryUpdate(updater)

        assertEquals(1, flow.value)
        verify(exactly = 1) {
            updater wasNot called
        }
    }

    @Test
    fun test_stateMap() = runFlowTest {
        val originFlow = MutableStateFlow(1)
        val mapFunction: (Int) -> String = mockk()
        every { mapFunction.invoke(any()) } answers {
            "item${firstArg<Int>()}"
        }
        val mappedFlow = originFlow.stateMap(mapFunction)

        val collectedItems = mappedFlow.startCollectingToList()

        // initial state
        assertEquals(1, collectedItems.size)
        assertEquals("item1", collectedItems.last())
        assertEquals("item1", mappedFlow.value)
        assertEquals(listOf("item1"), mappedFlow.replayCache)
        verify(exactly = 1) {
            mapFunction(1)
        }
        // emit a new value
        originFlow.value = 2
        assertEquals(2, collectedItems.size)
        assertEquals("item2", collectedItems.last())
        assertEquals("item2", mappedFlow.value)
        assertEquals(listOf("item2"), mappedFlow.replayCache)
        verify(exactly = 1) {
            mapFunction(2)
        }
        // emit the same value
        originFlow.value = 2
        assertEquals(2, collectedItems.size)
        assertEquals("item2", collectedItems.last())
        assertEquals("item2", mappedFlow.value)
        assertEquals(listOf("item2"), mappedFlow.replayCache)
        verify(exactly = 1) {
            mapFunction(2)
        }
        // emit a new value again
        originFlow.value = 3
        assertEquals(3, collectedItems.size)
        assertEquals("item3", collectedItems.last())
        assertEquals("item3", mappedFlow.value)
        assertEquals(listOf("item3"), mappedFlow.replayCache)
        verify(exactly = 1) {
            mapFunction(3)
        }
    }

    @Test
    fun test_combineStates() = runFlowTest {
        val flowA = MutableStateFlow("a1")
        val flowB = MutableStateFlow("b1")
        val flowC = MutableStateFlow("c1")
        val combineFunction: (List<*>) -> String = mockk()
        every { combineFunction.invoke(any()) } answers {
            val inputs = firstArg<List<String>>()
            inputs.joinToString("-")
        }

        val combinedFlow = combineStates(listOf(flowA, flowB, flowC), transform = combineFunction)
        val collectedItems = combinedFlow.startCollectingToList()

        // initial state
        assertEquals(1, collectedItems.size)
        assertEquals("a1-b1-c1", collectedItems.last())
        assertEquals("a1-b1-c1", combinedFlow.value)
        verify(exactly = 1) {
            combineFunction(listOf("a1", "b1", "c1"))
        }
        // emit the same values
        flowA.value = "a1"
        flowB.value = "b1"
        flowC.value = "c1"
        assertEquals(1, collectedItems.size)
        assertEquals("a1-b1-c1", collectedItems.last())
        assertEquals("a1-b1-c1", combinedFlow.value)
        verify(exactly = 1) {
            combineFunction(listOf("a1", "b1", "c1"))
        }
        // emit 1 new value
        flowB.value = "b2"
        assertEquals(2, collectedItems.size)
        assertEquals("a1-b2-c1", collectedItems.last())
        assertEquals("a1-b2-c1", combinedFlow.value)
        verify(exactly = 1) {
            combineFunction(listOf("a1", "b2", "c1"))
        }
        // emit 2 new values
        flowB.value = "b3"
        flowC.value = "c2"
        assertEquals(4, collectedItems.size)
        assertEquals("a1-b3-c1", collectedItems[collectedItems.lastIndex - 1])
        assertEquals("a1-b3-c2", collectedItems[collectedItems.lastIndex])
        assertEquals("a1-b3-c2", combinedFlow.value)
        verify(exactly = 1) {
            combineFunction(listOf("a1", "b3", "c1"))
            combineFunction(listOf("a1", "b3", "c2"))
        }
    }

    @Test
    fun test_combine2states() {
        val flowA = MutableStateFlow("a1")
        val flowB = MutableStateFlow("b1")

        val combinedFlow = combineStates(flowA, flowB) { a, b -> "$a-$b" }

        // initial state
        assertEquals("a1-b1", combinedFlow.value)
        // emit new values
        flowA.value = "a2"
        assertEquals("a2-b1", combinedFlow.value)
        flowB.value = "b2"
        assertEquals("a2-b2", combinedFlow.value)
    }

    @Test
    fun test_combine3states() {
        val flowA = MutableStateFlow("a1")
        val flowB = MutableStateFlow("b1")
        val flowC = MutableStateFlow("c1")

        val combinedFlow = combineStates(flowA, flowB, flowC) { a, b, c -> "$a-$b-$c" }

        // initial state
        assertEquals("a1-b1-c1", combinedFlow.value)
        // emit new values
        flowA.value = "a2"
        assertEquals("a2-b1-c1", combinedFlow.value)
        flowB.value = "b2"
        assertEquals("a2-b2-c1", combinedFlow.value)
        flowC.value = "c2"
        assertEquals("a2-b2-c2", combinedFlow.value)
    }


    @Test
    fun test_combine4states() {
        val flowA = MutableStateFlow("a1")
        val flowB = MutableStateFlow("b1")
        val flowC = MutableStateFlow("c1")
        val flowD = MutableStateFlow("d1")

        val combinedFlow = combineStates(flowA, flowB, flowC, flowD) { a, b, c, d ->
            "$a-$b-$c-$d"
        }

        // initial state
        assertEquals("a1-b1-c1-d1", combinedFlow.value)
        // emit new values
        flowA.value = "a2"
        assertEquals("a2-b1-c1-d1", combinedFlow.value)
        flowB.value = "b2"
        assertEquals("a2-b2-c1-d1", combinedFlow.value)
        flowC.value = "c2"
        assertEquals("a2-b2-c2-d1", combinedFlow.value)
        flowD.value = "d2"
        assertEquals("a2-b2-c2-d2", combinedFlow.value)
    }

    @Test
    fun test_combine5states() {
        val flowA = MutableStateFlow("a1")
        val flowB = MutableStateFlow("b1")
        val flowC = MutableStateFlow("c1")
        val flowD = MutableStateFlow("d1")
        val flowE = MutableStateFlow("e1")

        val combinedFlow = combineStates(flowA, flowB, flowC, flowD, flowE) { a, b, c, d, e ->
            "$a-$b-$c-$d-$e"
        }

        // initial state
        assertEquals("a1-b1-c1-d1-e1", combinedFlow.value)
        // emit new values
        flowA.value = "a2"
        assertEquals("a2-b1-c1-d1-e1", combinedFlow.value)
        flowB.value = "b2"
        assertEquals("a2-b2-c1-d1-e1", combinedFlow.value)
        flowC.value = "c2"
        assertEquals("a2-b2-c2-d1-e1", combinedFlow.value)
        flowD.value = "d2"
        assertEquals("a2-b2-c2-d2-e1", combinedFlow.value)
        flowE.value = "e2"
        assertEquals("a2-b2-c2-d2-e2", combinedFlow.value)
    }

}