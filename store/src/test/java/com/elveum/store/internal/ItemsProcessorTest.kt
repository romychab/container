package com.elveum.store.internal

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ItemsProcessorTest {

    @Test
    fun `GIVEN a key removed then re-added WHEN processed THEN it ends with exactly one tracked live job`() = runTest {
        val starts = AtomicInteger()
        val ends = AtomicInteger()
        val added = mutableListOf<Int>()
        val removed = mutableListOf<Int>()

        // remove and re-add key 1 back-to-back (no delay in between), so the stale job's
        // completion handler races with the re-added job (#4).
        val flow = flow {
            emit(setOf(1))
            delay(10)
            emit(emptySet())
            emit(setOf(1))
            delay(10)
            emit(emptySet())
            delay(10)
        }

        val processing = launch {
            processItems(
                flow = flow,
                onAdded = { added += it },
                onRemoved = { removed += it },
                job = {
                    starts.incrementAndGet()
                    try {
                        awaitCancellation()
                    } finally {
                        ends.incrementAndGet()
                    }
                },
            )
        }
        advanceUntilIdle()

        // the re-add must be honored as a fresh add (the buggy version mishandled the
        // remove/re-add interleaving and dropped the second add)
        assertEquals(2, added.count { it == 1 })
        // no leaked job: every started job must have ended after the final removal
        assertEquals(starts.get(), ends.get())
        // add/remove notifications stay balanced (no spurious or missing onRemoved)
        assertEquals(added.count { it == 1 }, removed.count { it == 1 })

        processing.cancel()
    }

    @Test
    fun `GIVEN keys that stay active across emissions WHEN processed THEN each job is launched once`() = runTest {
        val starts = AtomicInteger()
        val perKeyStarts = mutableMapOf<Int, Int>()

        val flow = flow {
            emit(setOf(1))
            delay(10)
            emit(setOf(1, 2))
            delay(10)
            emit(setOf(1, 2, 3))
            delay(10)
        }

        val processing = launch {
            processItems(
                flow = flow,
                job = { key ->
                    starts.incrementAndGet()
                    perKeyStarts[key] = (perKeyStarts[key] ?: 0) + 1
                    awaitCancellation()
                },
            )
        }
        advanceUntilIdle()

        // persistent keys must not be relaunched: exactly one job per distinct key (1, 2, 3)
        assertEquals(3, starts.get())
        assertEquals(mapOf(1 to 1, 2 to 1, 3 to 1), perKeyStarts)

        processing.cancel()
    }
}
