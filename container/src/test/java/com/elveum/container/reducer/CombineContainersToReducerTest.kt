package com.elveum.container.reducer

import com.elveum.container.Container
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import org.junit.Assert.assertEquals
import org.junit.Test

class CombineContainersToReducerTest {

    @Test
    fun `test combineContainersToReducer with 2 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<Container<String>>()
        val flowB = MutableSharedFlow<Container<String>>()
        val reducer = combineContainersToReducer(
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
        flowA.emit(successContainer("a1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // new state
        flowB.emit(successContainer("b1"))
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
        flowB.emit(successContainer("b2"))
        runCurrent()
        assertEquals(
            successContainer(State2("a1", "b2", "updated")),
            collector.lastItem
        )
    }

    @Test
    fun `test combineContainersToReducer with 3 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<Container<String>>()
        val flowB = MutableSharedFlow<Container<String>>()
        val flowC = MutableSharedFlow<Container<String>>()
        val reducer = combineContainersToReducer(
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
        flowA.emit(successContainer("a1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // initial state again
        flowB.emit(successContainer("b1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // new state
        flowC.emit(successContainer("c1"))
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
        flowC.emit(successContainer("c2"))
        runCurrent()
        assertEquals(
            successContainer(State3("a1", "b1", "c2", "updated")),
            collector.lastItem
        )
    }

    @Test
    fun `test combineContainersToReducer with 4 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<Container<String>>()
        val flowB = MutableSharedFlow<Container<String>>()
        val flowC = MutableSharedFlow<Container<String>>()
        val flowD = MutableSharedFlow<Container<String>>()
        val reducer = combineContainersToReducer(
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
        flowA.emit(successContainer("a1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // initial state again
        flowB.emit(successContainer("b1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // initial state again
        flowC.emit(successContainer("c1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // new state
        flowD.emit(successContainer("d1"))
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
        flowD.emit(successContainer("d2"))
        runCurrent()
        assertEquals(
            successContainer(State4("a1", "b1", "c1", "d2", "updated")),
            collector.lastItem
        )
    }

    @Test
    fun `test combineContainersToReducer with 5 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<Container<String>>()
        val flowB = MutableSharedFlow<Container<String>>()
        val flowC = MutableSharedFlow<Container<String>>()
        val flowD = MutableSharedFlow<Container<String>>()
        val flowE = MutableSharedFlow<Container<String>>()
        val reducer = combineContainersToReducer(
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
        flowA.emit(successContainer("a1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // initial state again
        flowB.emit(successContainer("b1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // initial state again
        flowC.emit(successContainer("c1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // initial state again
        flowD.emit(successContainer("d1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // new state
        flowE.emit(successContainer("e1"))
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
        flowE.emit(successContainer("e2"))
        runCurrent()
        assertEquals(
            successContainer(State5("a1", "b1", "c1", "d1", "e2", "updated")),
            collector.lastItem
        )
    }

    @Test
    fun `test combineContainersToReducer with iterable collection`() = runFlowTest {
        val flowA = MutableSharedFlow<Container<String>>()
        val flowB = MutableSharedFlow<Container<String>>()
        val reducer = combineContainersToReducer(
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
        flowA.emit(successContainer("a1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // new state
        flowB.emit(successContainer("b1"))
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
        flowB.emit(successContainer("b2"))
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