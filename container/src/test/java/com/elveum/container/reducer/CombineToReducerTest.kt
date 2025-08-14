package com.elveum.container.reducer

import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import org.junit.Assert.assertEquals
import org.junit.Test

class CombineToReducerTest {

    @Test
    fun `test combineToReducer with 2 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<String>()
        val flowB = MutableSharedFlow<String>()
        val reducer = combineToReducer(
            flowA,
            flowB,
            initialState = ::State2,
            nextState = State2::copy,
            scope = scope.backgroundScope,
            started = SharingStarted.Lazily,
        )

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()

        // initial state
        flowA.emit("a1")
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // new state
        flowB.emit("b1")
        runCurrent()
        assertEquals(
            successContainer(State2("a1", "b1")),
            collector.lastItem
        )

        // updated state manually
        reducer.updateValue { copy(other = "updated") }
        assertEquals(
            successContainer(State2("a1", "b1", "updated")),
            collector.lastItem
        )

        // update state by emitted value
        flowB.emit("b2")
        runCurrent()
        assertEquals(
            successContainer(State2("a1", "b2", "updated")),
            collector.lastItem
        )
    }

    @Test
    fun `test combineToReducer with 3 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<String>()
        val flowB = MutableSharedFlow<String>()
        val flowC = MutableSharedFlow<String>()
        val reducer = combineToReducer(
            flowA,
            flowB,
            flowC,
            initialState = ::State3,
            nextState = State3::copy,
            scope = scope.backgroundScope,
            started = SharingStarted.Lazily,
        )

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()

        // initial state
        flowA.emit("a1")
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // initial state again
        flowB.emit("b1")
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // new state
        flowC.emit("c1")
        runCurrent()
        assertEquals(
            successContainer(State3("a1", "b1", "c1")),
            collector.lastItem
        )

        // updated state manually
        reducer.updateValue { copy(other = "updated") }
        assertEquals(
            successContainer(State3("a1", "b1", "c1", "updated")),
            collector.lastItem
        )

        // update state by emitted value
        flowC.emit("c2")
        runCurrent()
        assertEquals(
            successContainer(State3("a1", "b1", "c2", "updated")),
            collector.lastItem
        )
    }

    @Test
    fun `test combineToReducer with 4 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<String>()
        val flowB = MutableSharedFlow<String>()
        val flowC = MutableSharedFlow<String>()
        val flowD = MutableSharedFlow<String>()
        val reducer = combineToReducer(
            flowA,
            flowB,
            flowC,
            flowD,
            initialState = ::State4,
            nextState = State4::copy,
            scope = scope.backgroundScope,
            started = SharingStarted.Lazily,
        )

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()

        // initial state
        flowA.emit("a1")
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // initial state again
        flowB.emit("b1")
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // initial state again
        flowC.emit("c1")
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // new state
        flowD.emit("d1")
        runCurrent()
        assertEquals(
            successContainer(State4("a1", "b1", "c1", "d1")),
            collector.lastItem
        )

        // updated state manually
        reducer.updateValue { copy(other = "updated") }
        assertEquals(
            successContainer(State4("a1", "b1", "c1", "d1", "updated")),
            collector.lastItem
        )

        // update state by emitted value
        flowD.emit("d2")
        runCurrent()
        assertEquals(
            successContainer(State4("a1", "b1", "c1", "d2", "updated")),
            collector.lastItem
        )
    }

    @Test
    fun `test combineToReducer with 5 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<String>()
        val flowB = MutableSharedFlow<String>()
        val flowC = MutableSharedFlow<String>()
        val flowD = MutableSharedFlow<String>()
        val flowE = MutableSharedFlow<String>()
        val reducer = combineToReducer(
            flowA,
            flowB,
            flowC,
            flowD,
            flowE,
            initialState = ::State5,
            nextState = State5::copy,
            scope = scope.backgroundScope,
            started = SharingStarted.Lazily,
        )

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()

        // initial state
        flowA.emit("a1")
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // initial state again
        flowB.emit("b1")
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // initial state again
        flowC.emit("c1")
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // initial state again
        flowD.emit("d1")
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // new state
        flowE.emit("e1")
        runCurrent()
        assertEquals(
            successContainer(State5("a1", "b1", "c1", "d1", "e1")),
            collector.lastItem
        )

        // updated state manually
        reducer.updateValue { copy(other = "updated") }
        assertEquals(
            successContainer(State5("a1", "b1", "c1", "d1", "e1", "updated")),
            collector.lastItem
        )

        // update state by emitted value
        flowE.emit("e2")
        runCurrent()
        assertEquals(
            successContainer(State5("a1", "b1", "c1", "d1", "e2", "updated")),
            collector.lastItem
        )
    }

    @Test
    fun `test combineToReducer with iterable collection`() = runFlowTest {
        val flowA = MutableSharedFlow<String>()
        val flowB = MutableSharedFlow<String>()
        val reducer = combineToReducer(
            flows = listOf(flowA, flowB),
            initialState = { list ->
                State2(list[0] as String, list[1] as String)
            },
            nextState = { state, list ->
                state.copy(list[0] as String, list[1] as String)
            },
            scope = scope.backgroundScope,
            started = SharingStarted.Lazily,
        )

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()

        // initial state
        flowA.emit("a1")
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // new state
        flowB.emit("b1")
        runCurrent()
        assertEquals(
            successContainer(State2("a1", "b1")),
            collector.lastItem
        )

        // updated state manually
        reducer.updateValue { copy(other = "updated") }
        assertEquals(
            successContainer(State2("a1", "b1", "updated")),
            collector.lastItem
        )

        // update state by emitted value
        flowB.emit("b2")
        runCurrent()
        assertEquals(
            successContainer(State2("a1", "b2", "updated")),
            collector.lastItem
        )
    }

    private data class State2(
        val a: String,
        val b: String,
        val other: String = "",
    )

    private data class State3(
        val a: String,
        val b: String,
        val c: String,
        val other: String = "",
    )

    private data class State4(
        val a: String,
        val b: String,
        val c: String,
        val d: String,
        val other: String = "",
    )

    private data class State5(
        val a: String,
        val b: String,
        val c: String,
        val d: String,
        val e: String,
        val other: String = "",
    )

}