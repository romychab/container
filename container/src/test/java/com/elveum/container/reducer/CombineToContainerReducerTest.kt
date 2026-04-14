package com.elveum.container.reducer

import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import org.junit.Assert.assertEquals
import org.junit.Test

class CombineToContainerReducerTest {

    @Test
    fun `test combineToContainerReducer with 2 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<String>()
        val flowB = MutableSharedFlow<String>()
        val reducer = combineToContainerReducer(
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
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()
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
    fun `test combineToContainerReducer with 3 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<String>()
        val flowB = MutableSharedFlow<String>()
        val flowC = MutableSharedFlow<String>()
        val reducer = combineToContainerReducer(
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
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()
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
    fun `test combineToContainerReducer with 4 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<String>()
        val flowB = MutableSharedFlow<String>()
        val flowC = MutableSharedFlow<String>()
        val flowD = MutableSharedFlow<String>()
        val reducer = combineToContainerReducer(
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
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()
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
    fun `test combineToContainerReducer with 5 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<String>()
        val flowB = MutableSharedFlow<String>()
        val flowC = MutableSharedFlow<String>()
        val flowD = MutableSharedFlow<String>()
        val flowE = MutableSharedFlow<String>()
        val reducer = combineToContainerReducer(
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
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()
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
    fun `test combineToContainerReducer with iterable collection`() = runFlowTest {
        val flowA = MutableSharedFlow<String>()
        val flowB = MutableSharedFlow<String>()
        val reducer = combineToContainerReducer(
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
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()
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
    fun `test combineToContainerReducer with 2 input flows on ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val flowA = MutableSharedFlow<String>()
            val flowB = MutableSharedFlow<String>()
            val reducer = combineToContainerReducer(
                flowA, flowB,
                initialState = ::State2,
                nextState = State2::copy,
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            flowA.emit("a1")
            runCurrent()
            assertEquals(pendingContainer(), collector.lastItem)

            flowB.emit("b1")
            runCurrent()
            assertEquals(successContainer(State2("a1", "b1")), collector.lastItem)
        }
    }

    @Test
    fun `test combineToContainerReducer with 3 input flows on ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val flowA = MutableSharedFlow<String>()
            val flowB = MutableSharedFlow<String>()
            val flowC = MutableSharedFlow<String>()
            val reducer = combineToContainerReducer(
                flowA, flowB, flowC,
                initialState = ::State3,
                nextState = State3::copy,
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            flowA.emit("a1")
            flowB.emit("b1")
            runCurrent()
            assertEquals(pendingContainer(), collector.lastItem)

            flowC.emit("c1")
            runCurrent()
            assertEquals(successContainer(State3("a1", "b1", "c1")), collector.lastItem)
        }
    }

    @Test
    fun `test combineToContainerReducer with 4 input flows on ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val flowA = MutableSharedFlow<String>()
            val flowB = MutableSharedFlow<String>()
            val flowC = MutableSharedFlow<String>()
            val flowD = MutableSharedFlow<String>()
            val reducer = combineToContainerReducer(
                flowA, flowB, flowC, flowD,
                initialState = ::State4,
                nextState = State4::copy,
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            flowA.emit("a1")
            flowB.emit("b1")
            flowC.emit("c1")
            runCurrent()
            assertEquals(pendingContainer(), collector.lastItem)

            flowD.emit("d1")
            runCurrent()
            assertEquals(successContainer(State4("a1", "b1", "c1", "d1")), collector.lastItem)
        }
    }

    @Test
    fun `test combineToContainerReducer with 5 input flows on ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val flowA = MutableSharedFlow<String>()
            val flowB = MutableSharedFlow<String>()
            val flowC = MutableSharedFlow<String>()
            val flowD = MutableSharedFlow<String>()
            val flowE = MutableSharedFlow<String>()
            val reducer = combineToContainerReducer(
                flowA, flowB, flowC, flowD, flowE,
                initialState = ::State5,
                nextState = State5::copy,
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            flowA.emit("a1")
            flowB.emit("b1")
            flowC.emit("c1")
            flowD.emit("d1")
            runCurrent()
            assertEquals(pendingContainer(), collector.lastItem)

            flowE.emit("e1")
            runCurrent()
            assertEquals(successContainer(State5("a1", "b1", "c1", "d1", "e1")), collector.lastItem)
        }
    }

    @Test
    fun `test combineToContainerReducer with iterable on ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val flowA = MutableSharedFlow<String>()
            val flowB = MutableSharedFlow<String>()
            val reducer = combineToContainerReducer(
                flows = listOf(flowA, flowB),
                initialState = { list -> State2(list[0] as String, list[1] as String) },
                nextState = { state, list -> state.copy(list[0] as String, list[1] as String) },
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            flowA.emit("a1")
            runCurrent()
            assertEquals(pendingContainer(), collector.lastItem)

            flowB.emit("b1")
            runCurrent()
            assertEquals(successContainer(State2("a1", "b1")), collector.lastItem)
        }
    }

    @Test
    fun `test combineToContainerReducer with default nextState uses initialState`() = runFlowTest {
        val flowA = MutableSharedFlow<String>()
        val flowB = MutableSharedFlow<String>()
        val reducer = combineToContainerReducer(
            flowA, flowB,
            initialState = ::State2,
            // nextState defaults to initialState
            scope = scope.backgroundScope,
            started = SharingStarted.Lazily,
        )

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()

        flowA.emit("a1")
        flowB.emit("b1")
        runCurrent()
        assertEquals(successContainer(State2("a1", "b1")), collector.lastItem)

        // With default nextState, updating re-applies initialState
        flowA.emit("a2")
        runCurrent()
        assertEquals(successContainer(State2("a2", "b1")), collector.lastItem)
    }

    private class TestReducerOwner(
        override val reducerCoroutineScope: CoroutineScope,
        override val reducerSharingStarted: SharingStarted,
    ) : ReducerOwner

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