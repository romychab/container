package com.elveum.container.reducer

import com.elveum.container.Container
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import org.junit.Assert.assertEquals
import org.junit.Test

class SimpleContainerReducerTest {

    @Test
    fun `test simple container reducer`() = runFlowTest {
        val inputFlow = MutableSharedFlow<Container<String>>()
        val reducer = SimpleContainerReducer(
            scope = scope.backgroundScope,
            started = SharingStarted.Eagerly,
            inputFlow = inputFlow,
        )
        runCurrent()

        val collector = reducer.stateFlow.startCollecting()

        // initial state
        assertEquals(pendingContainer(), collector.lastItem)

        // emit next item
        inputFlow.emit(successContainer("test"))
        runCurrent()
        assertEquals(successContainer("test"), collector.lastItem)

        // update to non-success container
        val errorContainer = errorContainer(IllegalStateException())
        reducer.updateContainer { errorContainer }
        assertEquals(errorContainer, collector.lastItem)

        // update item for non-success container -> nothing happens
        reducer.updateState { "test-ignored" }
        assertEquals(errorContainer, collector.lastItem)

        // update to success container
        reducer.updateContainer { successContainer("test-v2") }
        assertEquals(successContainer("test-v2"), collector.lastItem)

        // update item for success container
        reducer.updateState { "$it-updated" }
        assertEquals(successContainer("test-v2-updated"), collector.lastItem)
    }

}