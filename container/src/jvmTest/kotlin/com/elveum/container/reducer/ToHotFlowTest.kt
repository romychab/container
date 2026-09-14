package com.elveum.container.reducer

import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import org.junit.Assert.assertEquals
import org.junit.Test

class ToHotFlowTest {

    @Test
    fun stateIn_withReducerOwner_convertsFlowToStateFlow() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val coldFlow = MutableSharedFlow<String>()
            val stateFlow = coldFlow.stateIn(initialValue = "initial")

            val collected = stateFlow.startCollecting()
            runCurrent()

            assertEquals("initial", collected.lastItem)
            coldFlow.emit("first")
            runCurrent()
            assertEquals("first", collected.lastItem)
        }
    }

    @Test
    fun stateIn_withReducerOwner_usesOwnerScopeAndStarted() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val coldFlow = MutableSharedFlow<Int>()
            val stateFlow = coldFlow.stateIn(initialValue = 0)

            val collected = stateFlow.startCollecting()
            runCurrent()

            assertEquals(0, collected.lastItem)
            coldFlow.emit(42)
            runCurrent()
            assertEquals(42, collected.lastItem)
        }
    }

    @Test
    fun shareIn_withReducerOwner_convertsFlowToSharedFlow() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Lazily).apply {
            val coldFlow = MutableSharedFlow<String>()
            val sharedFlow = coldFlow.shareIn(replay = 1)

            val collected = sharedFlow.startCollecting()
            runCurrent()

            coldFlow.emit("hello")
            runCurrent()
            assertEquals("hello", collected.lastItem)
        }
    }

    @Test
    fun shareIn_withDefaultReplay_usesReplayZero() = runFlowTest {
        TestReducerOwner(scope.backgroundScope, SharingStarted.Eagerly).apply {
            val coldFlow = MutableSharedFlow<String>()
            val sharedFlow = coldFlow.shareIn()

            coldFlow.emit("before")
            runCurrent()

            val collected = sharedFlow.startCollecting()
            runCurrent()

            // With replay = 0, the pre-collect emission is not replayed
            coldFlow.emit("after")
            runCurrent()
            assertEquals("after", collected.lastItem)
        }
    }

    private class TestReducerOwner(
        override val reducerCoroutineScope: CoroutineScope,
        override val reducerSharingStarted: SharingStarted,
    ) : ReducerOwner
}
