package com.elveum.container

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThrottleLatestTest {

    /**
     * Collects the throttled flow and records the virtual time at which
     * each value is emitted downstream.
     */
    private suspend fun TestScope.collectWithTime(
        flow: kotlinx.coroutines.flow.Flow<Int>,
    ): List<Pair<Long, Int>> {
        val result = mutableListOf<Pair<Long, Int>>()
        flow.collect { result.add(testScheduler.currentTime to it) }
        return result
    }

    @Test
    fun firstValue_isEmittedImmediately() = runTest {
        val origin = flow {
            emit(1)
            delay(1000)
        }

        val result = collectWithTime(origin.throttleLatest(100))

        assertEquals(listOf(0L to 1), result)
    }

    @Test
    fun infrequentValues_areAllEmittedImmediately() = runTest {
        // Every value follows >= millis of silence -> each is a leading value.
        val origin = flow {
            emit(1)
            delay(150)
            emit(2)
            delay(150)
            emit(3)
            delay(150)
        }

        val result = collectWithTime(origin.throttleLatest(100))

        assertEquals(listOf(0L to 1, 150L to 2, 300L to 3), result)
    }

    @Test
    fun frequentValues_onlyLatestIsEmittedOncePerPeriod() = runTest {
        // millis = 100, values every 30ms: 1@0, 2@30, 3@60, 4@90, 5@120, 6@150, 7@180
        val origin = flow {
            emit(1); delay(30)
            emit(2); delay(30)
            emit(3); delay(30)
            emit(4); delay(30)
            emit(5); delay(30)
            emit(6); delay(30)
            emit(7); delay(30)
        }

        val result = collectWithTime(origin.throttleLatest(100))

        // 1 immediately; latest at each period boundary: 4@100, 7@200.
        assertEquals(listOf(0L to 1, 100L to 4, 200L to 7), result)
    }

    @Test
    fun synchronousBurst_emitsFirstThenLatest() = runTest {
        // All values arrive at t=0. First (1) immediately, latest (3) one period later.
        val origin = flowOf(1, 2, 3)

        val result = collectWithTime(origin.throttleLatest(100))

        assertEquals(listOf(0L to 1, 100L to 3), result)
    }

    @Test
    fun valueArrivingSoonAfterEmission_isThrottledToMinPeriod() = runTest {
        // 1@0 (leading), 2@50 (frequent) -> latest emitted at period end @100.
        // 3@110 arrives only 10ms after the previous EMISSION, so it is a frequent
        // update and must wait until the next period boundary @200 (not immediate).
        val origin = flow {
            emit(1); delay(50)
            emit(2); delay(60)
            emit(3); delay(500)
        }

        val result = collectWithTime(origin.throttleLatest(100))

        assertEquals(listOf(0L to 1, 100L to 2, 200L to 3), result)
    }

    @Test
    fun singleValueAfterLongIdle_isEmittedImmediately() = runTest {
        // Advantage #2: a lone non-frequent update after a long quiet period
        // must be rendered immediately, not delayed.
        val origin = flow {
            emit(1); delay(50)
            emit(2)          // throttled with 1 -> emitted at period boundary @100
            delay(1000)
            emit(9)          // long idle -> leading -> immediate
            delay(50)
        }

        val result = collectWithTime(origin.throttleLatest(100))

        assertEquals(listOf(0L to 1, 100L to 2, 1050L to 9), result)
    }

    @Test
    fun emissionPeriod_isNeverLessThanMillis() = runTest {
        // A dense stream must never produce two emissions closer than millis apart.
        val origin = flow {
            repeat(50) { i ->
                emit(i)
                delay(10)
            }
        }

        val result = collectWithTime(origin.throttleLatest(100))
        val times = result.map { it.first }

        for (i in 1 until times.size) {
            val gap = times[i] - times[i - 1]
            assertTrue("emission $i came only ${gap}ms after previous", gap >= 100)
        }
    }

    @Test
    fun lastValue_isNotLostOnCompletion() = runTest {
        // 1@0, 2@50 then completes. 2 must still be delivered.
        val origin = flow {
            emit(1); delay(50)
            emit(2)
        }

        val result = collectWithTime(origin.throttleLatest(100))

        assertEquals(listOf(1, 2), result.map { it.second })
    }

    @Test
    fun finiteFlow_completionIsDelayedByOnePeriodAfterLastValue() = runTest {
        // Documents the trailing-delay quirk: after the LAST value is emitted, the
        // loop still runs delay(millis) before it observes the closed channel, so a
        // finite source's terminal is postponed by `millis` even though no further
        // value can ever arrive.
        val origin = flowOf(1) // completes immediately after emitting 1

        val emittedAt = mutableListOf<Long>()
        origin.throttleLatest(100).collect { emittedAt.add(testScheduler.currentTime) }
        val completedAt = testScheduler.currentTime

        assertEquals(listOf(0L), emittedAt)          // value emitted immediately
        assertEquals(100L, completedAt)              // ...but collection returns 100ms later
    }

    @Test
    fun exceptionFromOrigin_isPropagatedDownstream() = runTest {
        val boom = IllegalStateException("boom")
        val origin = flow {
            emit(1)
            delay(10)
            throw boom
        }

        try {
            collectWithTime(origin.throttleLatest(100))
            fail("Expected the origin exception to propagate")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }
    }
}
