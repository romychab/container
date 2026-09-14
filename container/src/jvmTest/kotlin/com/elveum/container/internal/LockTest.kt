package com.elveum.container.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class LockTest {

    @Test
    fun withLock_isReentrant() {
        val lock = Lock()
        var depth = 0
        lock.withLock {
            depth++
            lock.withLock {
                depth++
                lock.withLock { depth++ }
            }
        }
        assertEquals(3, depth)
    }

    @Test
    fun withLock_returnsTheBlockValue() {
        val lock = Lock()
        assertEquals("ok", lock.withLock { "ok" })
    }

    @Test
    fun withLock_releasesTheLockWhenBlockThrows() {
        val lock = Lock()
        try {
            lock.withLock { throw IllegalStateException("boom") }
        } catch (_: IllegalStateException) {
            // expected
        }
        // would deadlock if the lock leaked
        assertEquals("reacquired", lock.withLock { "reacquired" })
    }

    @Test
    fun withLock_providesMutualExclusion() {
        val lock = Lock()
        val threadCount = 8
        val iterations = 20_000
        var counter = 0
        val start = CountDownLatch(1)
        val threads = (1..threadCount).map {
            thread {
                start.await()
                repeat(iterations) { lock.withLock { counter++ } }
            }
        }
        start.countDown()
        threads.forEach { it.join() }
        assertEquals(threadCount * iterations, counter)
    }

    @Test
    fun separateLocks_doNotBlockEachOther() {
        val first = Lock()
        val second = Lock()
        var reached = false
        first.withLock {
            second.withLock { reached = true }
        }
        assertTrue(reached)
    }
}
