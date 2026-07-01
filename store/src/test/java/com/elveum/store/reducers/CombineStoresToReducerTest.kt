package com.elveum.store.reducers

import com.elveum.container.reducer.ReducerOwner
import com.elveum.store.load.StoreResult
import com.elveum.store.load.failureOrNull
import com.elveum.store.load.getOrNull
import com.elveum.store.load.isFailed
import com.elveum.store.load.isForegroundLoading
import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombineStoresToReducerTest {

    @Test
    fun `test combineStoresToReducer with 2 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val reducer = combineStoresToReducer(
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
        flowA.emit(StoreResult.Loaded("a1"))
        runCurrent()
        assertTrue(collector.lastItem.isForegroundLoading())

        // new state
        flowB.emit(StoreResult.Loaded("b1"))
        runCurrent()
        assertEquals(State2("a1", "b1"), collector.lastItem.getOrNull())

        // updated state manually
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()
        assertEquals(State2("a1", "b1", "updated"), collector.lastItem.getOrNull())

        // update state by emitted value
        flowB.emit(StoreResult.Loaded("b2"))
        runCurrent()
        assertEquals(State2("a1", "b2", "updated"), collector.lastItem.getOrNull())
    }

    @Test
    fun `test combineStoresToReducer with 3 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val flowC = MutableSharedFlow<StoreResult<String>>()
        val reducer = combineStoresToReducer(
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
        flowA.emit(StoreResult.Loaded("a1"))
        runCurrent()
        assertTrue(collector.lastItem.isForegroundLoading())

        // initial state again
        flowB.emit(StoreResult.Loaded("b1"))
        runCurrent()
        assertTrue(collector.lastItem.isForegroundLoading())

        // new state
        flowC.emit(StoreResult.Loaded("c1"))
        runCurrent()
        assertEquals(State3("a1", "b1", "c1"), collector.lastItem.getOrNull())

        // updated state manually
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()
        assertEquals(State3("a1", "b1", "c1", "updated"), collector.lastItem.getOrNull())

        // update state by emitted value
        flowC.emit(StoreResult.Loaded("c2"))
        runCurrent()
        assertEquals(State3("a1", "b1", "c2", "updated"), collector.lastItem.getOrNull())
    }

    @Test
    fun `test combineStoresToReducer with 4 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val flowC = MutableSharedFlow<StoreResult<String>>()
        val flowD = MutableSharedFlow<StoreResult<String>>()
        val reducer = combineStoresToReducer(
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
        flowA.emit(StoreResult.Loaded("a1"))
        runCurrent()
        assertTrue(collector.lastItem.isForegroundLoading())

        // initial state again
        flowB.emit(StoreResult.Loaded("b1"))
        runCurrent()
        assertTrue(collector.lastItem.isForegroundLoading())

        // initial state again
        flowC.emit(StoreResult.Loaded("c1"))
        runCurrent()
        assertTrue(collector.lastItem.isForegroundLoading())

        // new state
        flowD.emit(StoreResult.Loaded("d1"))
        runCurrent()
        assertEquals(State4("a1", "b1", "c1", "d1"), collector.lastItem.getOrNull())

        // updated state manually
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()
        assertEquals(State4("a1", "b1", "c1", "d1", "updated"), collector.lastItem.getOrNull())

        // update state by emitted value
        flowD.emit(StoreResult.Loaded("d2"))
        runCurrent()
        assertEquals(State4("a1", "b1", "c1", "d2", "updated"), collector.lastItem.getOrNull())
    }

    @Test
    fun `test combineStoresToReducer with 5 input flows`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val flowC = MutableSharedFlow<StoreResult<String>>()
        val flowD = MutableSharedFlow<StoreResult<String>>()
        val flowE = MutableSharedFlow<StoreResult<String>>()
        val reducer = combineStoresToReducer(
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
        flowA.emit(StoreResult.Loaded("a1"))
        runCurrent()
        assertTrue(collector.lastItem.isForegroundLoading())

        // initial state again
        flowB.emit(StoreResult.Loaded("b1"))
        runCurrent()
        assertTrue(collector.lastItem.isForegroundLoading())

        // initial state again
        flowC.emit(StoreResult.Loaded("c1"))
        runCurrent()
        assertTrue(collector.lastItem.isForegroundLoading())

        // initial state again
        flowD.emit(StoreResult.Loaded("d1"))
        runCurrent()
        assertTrue(collector.lastItem.isForegroundLoading())

        // new state
        flowE.emit(StoreResult.Loaded("e1"))
        runCurrent()
        assertEquals(State5("a1", "b1", "c1", "d1", "e1"), collector.lastItem.getOrNull())

        // updated state manually
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()
        assertEquals(State5("a1", "b1", "c1", "d1", "e1", "updated"), collector.lastItem.getOrNull())

        // update state by emitted value
        flowE.emit(StoreResult.Loaded("e2"))
        runCurrent()
        assertEquals(State5("a1", "b1", "c1", "d1", "e2", "updated"), collector.lastItem.getOrNull())
    }

    @Test
    fun `test combineStoresToReducer with iterable collection`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val reducer = combineStoresToReducer(
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
        flowA.emit(StoreResult.Loaded("a1"))
        runCurrent()
        assertTrue(collector.lastItem.isForegroundLoading())

        // new state
        flowB.emit(StoreResult.Loaded("b1"))
        runCurrent()
        assertEquals(State2("a1", "b1"), collector.lastItem.getOrNull())

        // updated state manually
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()
        assertEquals(State2("a1", "b1", "updated"), collector.lastItem.getOrNull())

        // update state by emitted value
        flowB.emit(StoreResult.Loaded("b2"))
        runCurrent()
        assertEquals(State2("a1", "b2", "updated"), collector.lastItem.getOrNull())
    }

    @Test
    fun `test combineStoresToReducer propagates failed state`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val reducer = combineStoresToReducer(
            flowA,
            flowB,
            initialState = ::State2,
            nextState = State2::copy,
            scope = scope.backgroundScope,
            started = SharingStarted.Lazily,
        )

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()

        // loaded state
        flowA.emit(StoreResult.Loaded("a1"))
        flowB.emit(StoreResult.Loaded("b1"))
        runCurrent()
        assertEquals(State2("a1", "b1"), collector.lastItem.getOrNull())

        // a failure in one of the sources makes the result failed
        val exception = IllegalStateException("boom")
        flowB.emit(StoreResult.Failed(exception))
        runCurrent()
        assertTrue(collector.lastItem.isFailed())
        assertEquals(exception, collector.lastItem.failureOrNull())
    }

    @Test
    fun `test combineStoresToReducer with 2 input flows on ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val flowA = MutableSharedFlow<StoreResult<String>>()
            val flowB = MutableSharedFlow<StoreResult<String>>()
            val reducer = combineStoresToReducer(
                flowA,
                flowB,
                initialState = ::State2,
                nextState = State2::copy,
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            // initial state
            flowA.emit(StoreResult.Loaded("a1"))
            runCurrent()
            assertTrue(collector.lastItem.isForegroundLoading())

            // new state
            flowB.emit(StoreResult.Loaded("b1"))
            runCurrent()
            assertEquals(State2("a1", "b1"), collector.lastItem.getOrNull())

            // updated state manually
            reducer.updateState { it.copy(other = "updated") }
            runCurrent()
            assertEquals(State2("a1", "b1", "updated"), collector.lastItem.getOrNull())

            // update state by emitted value
            flowB.emit(StoreResult.Loaded("b2"))
            runCurrent()
            assertEquals(State2("a1", "b2", "updated"), collector.lastItem.getOrNull())
        }
    }

    @Test
    fun `test combineStoresToReducer with 3 input flows on ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val flowA = MutableSharedFlow<StoreResult<String>>()
            val flowB = MutableSharedFlow<StoreResult<String>>()
            val flowC = MutableSharedFlow<StoreResult<String>>()
            val reducer = combineStoresToReducer(
                flowA,
                flowB,
                flowC,
                initialState = ::State3,
                nextState = State3::copy,
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            // initial state
            flowA.emit(StoreResult.Loaded("a1"))
            runCurrent()
            assertTrue(collector.lastItem.isForegroundLoading())

            // initial state again
            flowB.emit(StoreResult.Loaded("b1"))
            runCurrent()
            assertTrue(collector.lastItem.isForegroundLoading())

            // new state
            flowC.emit(StoreResult.Loaded("c1"))
            runCurrent()
            assertEquals(State3("a1", "b1", "c1"), collector.lastItem.getOrNull())

            // updated state manually
            reducer.updateState { it.copy(other = "updated") }
            runCurrent()
            assertEquals(State3("a1", "b1", "c1", "updated"), collector.lastItem.getOrNull())

            // update state by emitted value
            flowC.emit(StoreResult.Loaded("c2"))
            runCurrent()
            assertEquals(State3("a1", "b1", "c2", "updated"), collector.lastItem.getOrNull())
        }
    }

    @Test
    fun `test combineStoresToReducer with 4 input flows on ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val flowA = MutableSharedFlow<StoreResult<String>>()
            val flowB = MutableSharedFlow<StoreResult<String>>()
            val flowC = MutableSharedFlow<StoreResult<String>>()
            val flowD = MutableSharedFlow<StoreResult<String>>()
            val reducer = combineStoresToReducer(
                flowA,
                flowB,
                flowC,
                flowD,
                initialState = ::State4,
                nextState = State4::copy,
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            // initial state
            flowA.emit(StoreResult.Loaded("a1"))
            runCurrent()
            assertTrue(collector.lastItem.isForegroundLoading())

            // initial state again
            flowB.emit(StoreResult.Loaded("b1"))
            runCurrent()
            assertTrue(collector.lastItem.isForegroundLoading())

            // initial state again
            flowC.emit(StoreResult.Loaded("c1"))
            runCurrent()
            assertTrue(collector.lastItem.isForegroundLoading())

            // new state
            flowD.emit(StoreResult.Loaded("d1"))
            runCurrent()
            assertEquals(State4("a1", "b1", "c1", "d1"), collector.lastItem.getOrNull())

            // updated state manually
            reducer.updateState { it.copy(other = "updated") }
            runCurrent()
            assertEquals(State4("a1", "b1", "c1", "d1", "updated"), collector.lastItem.getOrNull())

            // update state by emitted value
            flowD.emit(StoreResult.Loaded("d2"))
            runCurrent()
            assertEquals(State4("a1", "b1", "c1", "d2", "updated"), collector.lastItem.getOrNull())
        }
    }

    @Test
    fun `test combineStoresToReducer with 5 input flows on ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val flowA = MutableSharedFlow<StoreResult<String>>()
            val flowB = MutableSharedFlow<StoreResult<String>>()
            val flowC = MutableSharedFlow<StoreResult<String>>()
            val flowD = MutableSharedFlow<StoreResult<String>>()
            val flowE = MutableSharedFlow<StoreResult<String>>()
            val reducer = combineStoresToReducer(
                flowA,
                flowB,
                flowC,
                flowD,
                flowE,
                initialState = ::State5,
                nextState = State5::copy,
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            // initial state
            flowA.emit(StoreResult.Loaded("a1"))
            runCurrent()
            assertTrue(collector.lastItem.isForegroundLoading())

            // initial state again
            flowB.emit(StoreResult.Loaded("b1"))
            runCurrent()
            assertTrue(collector.lastItem.isForegroundLoading())

            // initial state again
            flowC.emit(StoreResult.Loaded("c1"))
            runCurrent()
            assertTrue(collector.lastItem.isForegroundLoading())

            // initial state again
            flowD.emit(StoreResult.Loaded("d1"))
            runCurrent()
            assertTrue(collector.lastItem.isForegroundLoading())

            // new state
            flowE.emit(StoreResult.Loaded("e1"))
            runCurrent()
            assertEquals(State5("a1", "b1", "c1", "d1", "e1"), collector.lastItem.getOrNull())

            // updated state manually
            reducer.updateState { it.copy(other = "updated") }
            runCurrent()
            assertEquals(State5("a1", "b1", "c1", "d1", "e1", "updated"), collector.lastItem.getOrNull())

            // update state by emitted value
            flowE.emit(StoreResult.Loaded("e2"))
            runCurrent()
            assertEquals(State5("a1", "b1", "c1", "d1", "e2", "updated"), collector.lastItem.getOrNull())
        }
    }

    @Test
    fun `test combineStoresToReducer with iterable collection on ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val flowA = MutableSharedFlow<StoreResult<String>>()
            val flowB = MutableSharedFlow<StoreResult<String>>()
            val reducer = combineStoresToReducer(
                flows = listOf(flowA, flowB),
                initialState = { list ->
                    State2(list[0] as String, list[1] as String)
                },
                nextState = { state, list ->
                    state.copy(list[0] as String, list[1] as String)
                },
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            // initial state
            flowA.emit(StoreResult.Loaded("a1"))
            runCurrent()
            assertTrue(collector.lastItem.isForegroundLoading())

            // new state
            flowB.emit(StoreResult.Loaded("b1"))
            runCurrent()
            assertEquals(State2("a1", "b1"), collector.lastItem.getOrNull())

            // updated state manually
            reducer.updateState { it.copy(other = "updated") }
            runCurrent()
            assertEquals(State2("a1", "b1", "updated"), collector.lastItem.getOrNull())

            // update state by emitted value
            flowB.emit(StoreResult.Loaded("b2"))
            runCurrent()
            assertEquals(State2("a1", "b2", "updated"), collector.lastItem.getOrNull())
        }
    }

    @Test
    fun `test combineStoresToReducer with 2 flows and default next state`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val reducer = combineStoresToReducer(
            flowA,
            flowB,
            initialState = ::State2,
            scope = scope.backgroundScope,
            started = SharingStarted.Lazily,
        )

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()
        flowA.emit(StoreResult.Loaded("a1"))
        flowB.emit(StoreResult.Loaded("b1"))
        runCurrent()
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()

        // emit next state - manual update is overridden, since nextState is not specified
        flowA.emit(StoreResult.Loaded("a2"))
        runCurrent()
        assertEquals(State2("a2", "b1"), collector.lastItem.getOrNull())
    }

    @Test
    fun `test combineStoresToReducer with 3 flows and default next state`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val flowC = MutableSharedFlow<StoreResult<String>>()
        val reducer = combineStoresToReducer(
            flowA,
            flowB,
            flowC,
            initialState = ::State3,
            scope = scope.backgroundScope,
            started = SharingStarted.Lazily,
        )

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()
        flowA.emit(StoreResult.Loaded("a1"))
        flowB.emit(StoreResult.Loaded("b1"))
        flowC.emit(StoreResult.Loaded("c1"))
        runCurrent()
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()

        // emit next state - manual update is overridden, since nextState is not specified
        flowA.emit(StoreResult.Loaded("a2"))
        runCurrent()
        assertEquals(State3("a2", "b1", "c1"), collector.lastItem.getOrNull())
    }

    @Test
    fun `test combineStoresToReducer with 4 flows and default next state`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val flowC = MutableSharedFlow<StoreResult<String>>()
        val flowD = MutableSharedFlow<StoreResult<String>>()
        val reducer = combineStoresToReducer(
            flowA,
            flowB,
            flowC,
            flowD,
            initialState = ::State4,
            scope = scope.backgroundScope,
            started = SharingStarted.Lazily,
        )

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()
        flowA.emit(StoreResult.Loaded("a1"))
        flowB.emit(StoreResult.Loaded("b1"))
        flowC.emit(StoreResult.Loaded("c1"))
        flowD.emit(StoreResult.Loaded("d1"))
        runCurrent()
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()

        // emit next state - manual update is overridden, since nextState is not specified
        flowA.emit(StoreResult.Loaded("a2"))
        runCurrent()
        assertEquals(State4("a2", "b1", "c1", "d1"), collector.lastItem.getOrNull())
    }

    @Test
    fun `test combineStoresToReducer with 5 flows and default next state`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val flowC = MutableSharedFlow<StoreResult<String>>()
        val flowD = MutableSharedFlow<StoreResult<String>>()
        val flowE = MutableSharedFlow<StoreResult<String>>()
        val reducer = combineStoresToReducer(
            flowA,
            flowB,
            flowC,
            flowD,
            flowE,
            initialState = ::State5,
            scope = scope.backgroundScope,
            started = SharingStarted.Lazily,
        )

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()
        flowA.emit(StoreResult.Loaded("a1"))
        flowB.emit(StoreResult.Loaded("b1"))
        flowC.emit(StoreResult.Loaded("c1"))
        flowD.emit(StoreResult.Loaded("d1"))
        flowE.emit(StoreResult.Loaded("e1"))
        runCurrent()
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()

        // emit next state - manual update is overridden, since nextState is not specified
        flowA.emit(StoreResult.Loaded("a2"))
        runCurrent()
        assertEquals(State5("a2", "b1", "c1", "d1", "e1"), collector.lastItem.getOrNull())
    }

    @Test
    fun `test combineStoresToReducer with flow iterable and default next state`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val reducer = combineStoresToReducer(
            flows = listOf(flowA, flowB),
            initialState = { State2(it[0].toString(), it[1].toString()) },
            scope = scope.backgroundScope,
            started = SharingStarted.Lazily,
        )

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()
        flowA.emit(StoreResult.Loaded("a1"))
        flowB.emit(StoreResult.Loaded("b1"))
        runCurrent()
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()

        // emit next state - manual update is overridden, since nextState is not specified
        flowA.emit(StoreResult.Loaded("a2"))
        runCurrent()
        assertEquals(State2("a2", "b1"), collector.lastItem.getOrNull())
    }

    @Test
    fun `test combineStoresToReducer with 2 flows, owner, and default next state`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val owner = TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily)
        val reducer = with(owner) {
            combineStoresToReducer(
                flowA,
                flowB,
                initialState = ::State2,
            )
        }

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()
        flowA.emit(StoreResult.Loaded("a1"))
        flowB.emit(StoreResult.Loaded("b1"))
        runCurrent()
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()

        // emit next state - manual update is overridden, since nextState is not specified
        flowA.emit(StoreResult.Loaded("a2"))
        runCurrent()
        assertEquals(State2("a2", "b1"), collector.lastItem.getOrNull())
    }

    @Test
    fun `test combineStoresToReducer with flow iterable, owner, and default next state`() = runFlowTest {
        val flowA = MutableSharedFlow<StoreResult<String>>()
        val flowB = MutableSharedFlow<StoreResult<String>>()
        val owner = TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily)
        val reducer = with(owner) {
            combineStoresToReducer(
                flows = listOf(flowA, flowB),
                initialState = { State2(it[0].toString(), it[1].toString()) },
            )
        }

        val collector = reducer.stateFlow.startCollecting()
        runCurrent()
        flowA.emit(StoreResult.Loaded("a1"))
        flowB.emit(StoreResult.Loaded("b1"))
        runCurrent()
        reducer.updateState { it.copy(other = "updated") }
        runCurrent()

        // emit next state - manual update is overridden, since nextState is not specified
        flowA.emit(StoreResult.Loaded("a2"))
        runCurrent()
        assertEquals(State2("a2", "b1"), collector.lastItem.getOrNull())
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

    private class TestReducerOwner(
        override val reducerCoroutineScope: CoroutineScope,
        override val reducerSharingStarted: SharingStarted,
    ) : ReducerOwner
}
