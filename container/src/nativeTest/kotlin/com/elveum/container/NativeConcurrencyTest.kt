package com.elveum.container

import com.elveum.container.internal.Lock
import com.elveum.container.internal.withLock
import com.elveum.container.subject.FlowSubjectImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val THREADS = 8
private const val ITERATIONS = 20_000

class NativeConcurrencyTest {

    @Test
    fun withLock_isReentrant_likeAJvmMonitor() {
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
    fun withLock_returnsBlockValue() {
        val lock = Lock()
        assertEquals("ok", lock.withLock { "ok" })
    }

    @Test
    fun withLock_providesMutualExclusion() = runBlocking {
        val lock = Lock()
        var counter = 0
        coroutineScope {
            repeat(THREADS) {
                launch(Dispatchers.Default) {
                    repeat(ITERATIONS) { lock.withLock { counter++ } }
                }
            }
        }
        assertEquals(THREADS * ITERATIONS, counter)
    }

    @Test
    fun withLock_releasesTheLockWhenBlockThrows() {
        val lock = Lock()
        try {
            lock.withLock { throw IllegalStateException("boom") }
        } catch (_: IllegalStateException) {
            // expected
        }
        // if the lock leaked, this would deadlock
        assertEquals(Unit, lock.withLock { Unit })
    }

    @Test
    fun cachingFunction_underConcurrentAccess_neverTearsState() = runBlocking {
        var calls = 0
        val callLock = Lock()
        val function = CachingFunction<Int, String> { input ->
            callLock.withLock { calls++ }
            "value-$input"
        }
        coroutineScope {
            repeat(THREADS) {
                launch(Dispatchers.Default) {
                    repeat(ITERATIONS / 10) { i ->
                        val arg = i % 4
                        assertEquals("value-$arg", function(arg))
                    }
                }
            }
        }
        assertTrue(calls > 0)
    }

    @Test
    fun flowSubjectImpl_concurrentListenerChurnAndEmissions_stableCollectorCompletesExactlyOnce() = runBlocking {
        val subject = FlowSubjectImpl<Int>()
        val completionLock = Lock()
        var stableCompletions = 0

        // A long-lived collector: exercises listeners.forEach in onNext/onComplete
        // concurrently with the churners below mutating the same listeners set.
        val stableJob = launch(Dispatchers.Default) {
            subject.flow().collect { }
            completionLock.withLock { stableCompletions++ }
        }

        coroutineScope {
            // Churners: repeatedly attach and cancel a collector, driving add()/remove()
            // on the listeners set concurrently with the emissions below.
            repeat(THREADS) {
                launch(Dispatchers.Default) {
                    repeat(ITERATIONS / 200) {
                        val churnJob = launch(Dispatchers.Default) {
                            subject.flow().collect { }
                        }
                        churnJob.cancelAndJoin()
                    }
                }
            }
            // Emitters: drive onNext(), which iterates the listeners set concurrently
            // with the add()/remove() calls above.
            repeat(THREADS) {
                launch(Dispatchers.Default) {
                    repeat(ITERATIONS / 20) { i -> subject.onNext(i) }
                }
            }
        }

        subject.onComplete()
        stableJob.join()

        completionLock.withLock { assertEquals(1, stableCompletions) }
    }

    @Test
    fun atomicBoolean_compareAndSet_isExclusive() = runBlocking {
        val flag = AtomicBoolean(false)
        var winners = 0
        val winnerLock = Lock()
        coroutineScope {
            repeat(THREADS) {
                launch(Dispatchers.Default) {
                    if (flag.compareAndSet(false, true)) {
                        winnerLock.withLock { winners++ }
                    }
                }
            }
        }
        assertEquals(1, winners)
        assertTrue(flag.load())
        assertFalse(flag.compareAndSet(false, true))
    }
}
