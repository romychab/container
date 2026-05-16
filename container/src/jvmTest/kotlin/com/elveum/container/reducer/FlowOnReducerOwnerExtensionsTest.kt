package com.elveum.container.reducer

import com.elveum.container.Container
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowOnReducerOwnerExtensionsTest {

    @Test
    fun `test toReducer with ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val inputFlow = MutableSharedFlow<Int>()
            val reducer = inputFlow.toReducer(
                initialState = "",
                nextState = { state, value -> "$state:$value" },
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            assertEquals("", collector.lastItem)

            inputFlow.emit(1)
            runCurrent()
            assertEquals(":1", collector.lastItem)
        }
    }

    @Test
    fun `test toReducer without mapping with ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val inputFlow = MutableSharedFlow<String>()
            val reducer = inputFlow.toReducer(initialState = "init")

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            assertEquals("init", collector.lastItem)

            inputFlow.emit("value")
            runCurrent()
            assertEquals("value", collector.lastItem)
        }
    }

    @Test
    fun `test toContainerReducer with ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val inputFlow = MutableSharedFlow<Int>()
            val reducer = inputFlow.toContainerReducer(
                initialState = { it.toString() },
                nextState = { state, value -> "$state:$value" },
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            assertEquals(pendingContainer(), collector.lastItem)

            inputFlow.emit(1)
            runCurrent()
            assertEquals(successContainer("1"), collector.lastItem)

            inputFlow.emit(2)
            runCurrent()
            assertEquals(successContainer("1:2"), collector.lastItem)
        }
    }

    @Test
    fun `test toContainerReducer without mapping with ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val inputFlow = MutableSharedFlow<Int>()
            val reducer = inputFlow.toContainerReducer<Int>()

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            assertEquals(pendingContainer(), collector.lastItem)

            inputFlow.emit(5)
            runCurrent()
            assertEquals(successContainer(5), collector.lastItem)
        }
    }

    @Test
    fun `test containerToReducer with ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val inputFlow = MutableSharedFlow<Container<Int>>()
            val reducer = inputFlow.containerToReducer(
                initialState = { it.toString() },
                nextState = { state, value -> "$state:$value" },
            )

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            assertEquals(pendingContainer(), collector.lastItem)

            inputFlow.emit(successContainer(1))
            runCurrent()
            assertEquals(successContainer("1"), collector.lastItem)
        }
    }

    @Test
    fun `test containerToReducer without mapping with ReducerOwner`() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val inputFlow = MutableSharedFlow<Container<Int>>()
            val reducer = inputFlow.containerToReducer<Int>()

            val collector = reducer.stateFlow.startCollecting()
            runCurrent()

            assertEquals(pendingContainer(), collector.lastItem)

            inputFlow.emit(successContainer(7))
            runCurrent()
            assertEquals(successContainer(7), collector.lastItem)
        }
    }

    private class TestReducerOwner(
        override val reducerCoroutineScope: CoroutineScope,
        override val reducerSharingStarted: SharingStarted,
    ) : ReducerOwner

}