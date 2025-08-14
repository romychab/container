package com.elveum.container.reducer

import com.elveum.container.Container
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class MapperContainerReducerTest {

    @Test
    fun `test mapper container reducer`() = runFlowTest {
        val inputFlow1 = MutableSharedFlow<Container<String>>()
        val inputFlow2 = MutableSharedFlow<Container<Int>>()
        val reducer = MapperContainerReducer(
            inputFlows = listOf(inputFlow1, inputFlow2),
            scope = scope.backgroundScope,
            started = SharingStarted.Eagerly,
            initialValue = { list -> State(list[0] as String, list[1] as Int) },
            nextValue = { state, list ->
                state.copy(list[0] as String, list[1] as Int)
            }
        )
        runCurrent()

        val collector = reducer.stateFlow.startCollecting()

        // initial state
        assertEquals(pendingContainer(), collector.lastItem)

        // emit 1st item
        inputFlow1.emit(successContainer("t1"))
        runCurrent()
        assertEquals(pendingContainer(), collector.lastItem)

        // emit 2nd item
        inputFlow2.emit(successContainer(1))
        runCurrent()
        assertEquals(
            successContainer(State("t1", 1)),
            collector.lastItem,
        )

        // emit 1st item again
        inputFlow1.emit(successContainer("t2"))
        runCurrent()
        assertEquals(
            successContainer(State("t2", 1)),
            collector.lastItem,
        )

        // update item manually
        reducer.updateState { it.copy(otherValue = "updated") }
        assertEquals(
            successContainer(State("t2", 1, "updated")),
            collector.lastItem,
        )

        // emit 2nd item
        inputFlow2.emit(successContainer(2))
        runCurrent()
        assertEquals(
            successContainer(State("t2", 2, "updated")),
            collector.lastItem,
        )

        // emit error
        inputFlow1.emit(errorContainer(IllegalStateException()))
        runCurrent()
        Assert.assertTrue(collector.lastItem is Container.Error)

        // emit item after error
        inputFlow1.emit(successContainer("t3"))
        runCurrent()
        assertEquals(
            successContainer(State("t3", 2)),
            collector.lastItem,
        )
    }

    private data class State(
        val string: String,
        val int: Int,
        val otherValue: String = "",
    )

}
