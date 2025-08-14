package com.elveum.container.reducer

import com.elveum.container.Container
import com.elveum.container.getOrNull
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import com.uandcode.flowtest.runFlowTest
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class ReducerStateFlowTest {

    private lateinit var testScope: TestScope

    private lateinit var inputFlow1: MutableSharedFlow<Container<String>>
    private lateinit var inputFlow2: MutableSharedFlow<Container<Int>>
    private lateinit var transform: suspend (Container<State>, Container<List<*>>) -> Container<State>

    @Before
    fun setUp() {
        testScope = TestScope()
        inputFlow1 = MutableSharedFlow()
        inputFlow2 = MutableSharedFlow()
        transform = { old, new ->
            val oldState = old.getOrNull() ?: State(0, "")
            val newValues = new.getOrNull()!!
            val stringValue = newValues[0] as String
            val intValue = newValues[1] as Int
            successContainer(oldState.copy(counter = oldState.counter + 1, result = "$stringValue:$intValue"))
        }
        transform = spyk(transform)
    }

    @Test
    fun `reducer does not start collecting by default`() = testScope.runFlowTest {
        val outputFlow = MutableStateFlow<Container<State>>(pendingContainer())
        createReducerFlow(outputFlow = outputFlow)
        runCurrent()

        inputFlow1.emit(successContainer("t1"))
        inputFlow2.emit(successContainer(1))
        runCurrent()

        assertEquals(pendingContainer(), outputFlow.value)
    }

    @Test
    fun `test eager collecting`() = testScope.runFlowTest {
        val outputFlow = MutableStateFlow<Container<State>>(pendingContainer())
        createReducerFlow(
            started = SharingStarted.Eagerly,
            outputFlow = outputFlow,
        )
        runCurrent()

        inputFlow1.emit(successContainer("t1"))
        inputFlow2.emit(successContainer(1))
        runCurrent()

        assertEquals(
            successContainer(State(counter = 1, "t1:1")),
            outputFlow.value,
        )
    }

    @Test
    fun `test lazy collecting`() = testScope.runFlowTest {
        val outputFlow = MutableStateFlow<Container<State>>(pendingContainer())
        val flow = createReducerFlow(
            started = SharingStarted.Lazily,
            outputFlow = outputFlow,
        )
        runCurrent()

        // nothing should happen without active collectors
        inputFlow1.emit(successContainer("t1"))
        inputFlow2.emit(successContainer(1))
        runCurrent()
        assertEquals(pendingContainer(), outputFlow.value)

        // start collecting after the first subscription
        flow.startCollecting()
        runCurrent()
        inputFlow1.emit(successContainer("t2"))
        inputFlow2.emit(successContainer(2))
        runCurrent()
        assertEquals(
            successContainer(State(1, "t2:2")),
            outputFlow.value
        )
    }

    @Test
    fun `test whileSubscribed collecting`() = testScope.runFlowTest {
        val outputFlow = MutableStateFlow<Container<State>>(pendingContainer())
        val flow = createReducerFlow(
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000, replayExpirationMillis = 500),
            outputFlow = outputFlow,
        )
        runCurrent()

        // nothing should happen without active collectors
        inputFlow1.emit(successContainer("t1"))
        inputFlow2.emit(successContainer(1))
        runCurrent()
        coVerify(exactly = 0) { transform(any(), any()) }

        // start collecting after the first subscription
        val collector1 = flow.startCollecting()
        runCurrent()
        inputFlow1.emit(successContainer("t2"))
        inputFlow2.emit(successContainer(2))
        runCurrent()
        coVerify(exactly = 1) {
            transform(
                pendingContainer(),
                successContainer(listOf("t2", 2))
            )
        }

        // continue collecting before timeout expires
        collector1.cancel()
        advanceTimeBy(999) // almost expired (1ms left)
        runCurrent()
        inputFlow2.emit(successContainer(3))
        runCurrent()
        coVerify(exactly = 1) {
            transform(
                any(),
                successContainer(listOf("t2", 3))
            )
        }

        // stop collecting after timeout
        advanceTimeBy(1) // now expired
        runCurrent()
        inputFlow2.emit(successContainer(4))
        runCurrent()
        coVerify(exactly = 0) {
            transform(
                any(),
                successContainer(listOf("t2", 4))
            )
        }

        // now the last item should be still cached before replayTimeout expires
        advanceTimeBy(499) // almost expired
        runCurrent()
        assertNotEquals(pendingContainer(), outputFlow.value)

        // reset cache after replayTimeout expires
        advanceTimeBy(1) // now expired
        runCurrent()
        assertEquals(pendingContainer(), outputFlow.value)

        // restart collecting after the next subscription
        flow.startCollecting()
        runCurrent()
        inputFlow1.emit(successContainer("t3"))
        inputFlow2.emit(successContainer(5))
        runCurrent()
        coVerify(exactly = 1) {
            transform(
                any(),
                successContainer(listOf("t3", 5))
            )
        }
    }

    @Test
    fun `test transformations`() = testScope.runFlowTest {
        val flow = createReducerFlow()

        val collector = flow.startCollecting()
        runCurrent()

        // initial state
        assertEquals(pendingContainer(), collector.lastItem)

        // emit item by first flow -> still waiting for the second one
        inputFlow1.emit(successContainer("t1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // emit by second flow -> update output
        inputFlow2.emit(successContainer(1))
        runCurrent()
        assertEquals(
            successContainer(State(counter = 1, "t1:1")),
            collector.lastItem,
        )

        // emit next item by first flow
        inputFlow1.emit(successContainer("t2"))
        runCurrent()
        assertEquals(
            successContainer(State(counter = 2, "t2:1")),
            collector.lastItem,
        )

        // emit next item by first flow again
        inputFlow1.emit(successContainer("t3"))
        runCurrent()
        assertEquals(
            successContainer(State(counter = 3, "t3:1")),
            collector.lastItem,
        )

        // emit next item by second flow
        inputFlow2.emit(successContainer(2))
        runCurrent()
        assertEquals(
            successContainer(State(counter = 4, "t3:2")),
            collector.lastItem,
        )
    }

    private fun createReducerFlow(
        started: SharingStarted = SharingStarted.Lazily,
        outputFlow: MutableStateFlow<Container<State>> = MutableStateFlow(pendingContainer())
    ): ReducerStateFlow<State> {
        return ReducerStateFlow(
            originFlows = listOf(inputFlow1, inputFlow2),
            scope = testScope.backgroundScope,
            started = started,
            transform = transform,
            outputFlow = outputFlow,
        )
    }

    private data class State(
        val counter: Int,
        val result: String,
    )
}
