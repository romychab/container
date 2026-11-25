package com.elveum.store.internal

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowSwitchTest {

    @Test
    fun `WHEN upstream emits new value THEN new inner flow starts before previous one is cancelled`() = runTest {
        val events = mutableListOf<String>()
        val upstream = MutableStateFlow(1)
        val job = launch {
            upstream
                .flatMapLatestMakeBeforeBreak { value ->
                    flow<Int> { awaitCancellation() }
                        .onStart { events += "start-$value" }
                        .onCompletion { events += "stop-$value" }
                }
                .collect {}
        }
        runCurrent()

        upstream.value = 2
        runCurrent()
        job.cancel()
        runCurrent()

        assertEquals(listOf("start-1", "start-2", "stop-1", "stop-2"), events)
    }

    @Test
    fun `WHEN upstream emits new value THEN only values of the latest inner flow are emitted`() = runTest {
        val upstream = MutableStateFlow("a")
        val inners = mapOf("a" to MutableSharedFlow<String>(), "b" to MutableSharedFlow())
        val collected = mutableListOf<String>()
        val job = launch {
            upstream
                .flatMapLatestMakeBeforeBreak { value -> inners.getValue(value) }
                .collect { collected += it }
        }
        runCurrent()
        inners.getValue("a").emit("a1")
        runCurrent()

        upstream.value = "b"
        runCurrent()
        inners.getValue("a").tryEmit("a2")
        inners.getValue("b").emit("b1")
        runCurrent()
        job.cancel()

        assertEquals(listOf("a1", "b1"), collected)
    }

    @Test
    fun `GIVEN slow collector WHEN upstream switches THEN buffered values of the replaced inner flow are dropped`() = runTest {
        val upstream = MutableStateFlow("a")
        val inners = mapOf("a" to MutableSharedFlow<String>(), "b" to MutableSharedFlow())
        val collected = mutableListOf<String>()
        val job = launch {
            upstream
                .flatMapLatestMakeBeforeBreak { value -> inners.getValue(value) }
                .collect {
                    collected += it
                    delay(10) // a1 is being processed while a2 waits in the buffer
                }
        }
        runCurrent()
        inners.getValue("a").emit("a1")
        inners.getValue("a").emit("a2")
        runCurrent()

        upstream.value = "b"
        runCurrent()
        inners.getValue("b").emit("b1")
        advanceTimeBy(100)
        job.cancel()

        assertEquals(listOf("a1", "b1"), collected)
    }

    @Test
    fun `WHEN upstream completes THEN flow completes after the last inner flow`() = runTest {
        val result = flowOf(1, 2)
            .flatMapLatestMakeBeforeBreak { value ->
                flow {
                    delay(10)
                    emit(value * 10)
                }
            }
            .toList()

        assertEquals(listOf(20), result)
    }

}
