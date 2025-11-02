package com.elveum.container.reducer

import com.elveum.container.reducer.impl.ReducerImpl
import com.uandcode.flowtest.runFlowTest
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class ReducerImplTest {

    private lateinit var testScope: TestScope

    private lateinit var originFlow: MutableSharedFlow<String>
    private lateinit var transform: suspend (State, String) -> State

    @Before
    fun setUp() {
        testScope = TestScope()
        originFlow = MutableSharedFlow()
        transform = { old, new ->
            old.copy(result = new)
        }
        transform = spyk(transform)
    }

    @Test
    fun `reducer does not start collecting by default`() = testScope.runFlowTest {
        val initialState = State()
        val reducer = createReducer(initialState)
        runCurrent()

        originFlow.emit("t1")
        runCurrent()

        assertEquals(initialState, reducer.stateFlow.value)
    }

    @Test
    fun `test eager collecting`() = testScope.runFlowTest {
        val initialState = State()
        val reducer = createReducer(initialState, SharingStarted.Eagerly)
        runCurrent()

        reducer.update { it.copy(counter = 2) }
        originFlow.emit("t1")
        runCurrent()

        assertEquals(
            State(counter = 2, "t1"),
            reducer.stateFlow.value,
        )
    }

    @Test
    fun `test lazy collecting`() = testScope.runFlowTest {
        val initialState = State()
        val reducer = createReducer(initialState, SharingStarted.Lazily)
        runCurrent()

        // nothing should happen without active collectors
        originFlow.emit("t2")
        runCurrent()
        assertEquals(initialState, reducer.stateFlow.value)

        // start collecting after the first subscription
        reducer.stateFlow.startCollecting()
        runCurrent()
        originFlow.emit("t3")
        runCurrent()
        assertEquals(
            State(0, "t3"),
            reducer.stateFlow.value
        )
    }

    @Test
    fun `test whileSubscribed collecting`() = testScope.runFlowTest {
        val initialState = State()
        val reducer = createReducer(initialState, SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000, replayExpirationMillis = 500))
        runCurrent()

        // nothing should happen without active collectors
        originFlow.emit("t2")
        runCurrent()
        assertEquals(initialState, reducer.stateFlow.value)
        coVerify(exactly = 0) { transform(any(), any()) }

        // start collecting after the first subscription
        val collector1 = reducer.stateFlow.startCollecting()
        runCurrent()
        originFlow.emit("t3")
        runCurrent()
        coVerify(exactly = 1) {
            transform(
                initialState,
                "t3"
            )
        }

        // continue collecting before timeout expires
        collector1.cancel()
        advanceTimeBy(999) // almost expired (1ms left)
        runCurrent()
        originFlow.emit("t4")
        runCurrent()
        coVerify(exactly = 1) {
            transform(
                any(),
                "t4",
            )
        }

        // stop collecting after timeout
        advanceTimeBy(1) // now expired
        runCurrent()
        originFlow.emit("t5")
        runCurrent()
        coVerify(exactly = 0) {
            transform(
                any(),
                "t5",
            )
        }

        // now the last item should be still cached before replayTimeout expires
        advanceTimeBy(499) // almost expired
        runCurrent()
        assertNotEquals(initialState, reducer.stateFlow.value)

        // reset cache after replayTimeout expires
        advanceTimeBy(1) // now expired
        runCurrent()
        assertEquals(initialState, reducer.stateFlow.value)

        // restart collecting after the next subscription
        reducer.stateFlow.startCollecting()
        runCurrent()
        originFlow.emit("t6")
        runCurrent()
        coVerify(exactly = 1) {
            transform(
                any(),
                "t6",
            )
        }
    }

    private fun createReducer(
        initialState: State,
        started: SharingStarted = SharingStarted.Lazily,
    ): Reducer<State> {
        return ReducerImpl(
            initialState = initialState,
            originFlow = originFlow,
            combiner = transform,
            scope = testScope.backgroundScope,
            started = started,
        )
    }

    private data class State(
        val counter: Int = 0,
        val result: String = "",
    )
}
