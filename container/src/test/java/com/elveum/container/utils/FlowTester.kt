package com.elveum.container.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.coroutines.EmptyCoroutineContext

fun runFlowTest(
    testBody: suspend FlowTest.() -> Unit
) {
    runTest {
        doRunFlowTest(testBody)
    }
}

fun TestScope.runFlowTest(
    testBody: suspend FlowTest.() -> Unit
) {
    runTest {
        doRunFlowTest(testBody)
    }
}

private suspend fun TestScope.doRunFlowTest(
    testBody: suspend FlowTest.() -> Unit
) {
    val scope = FlowTest(this)
    try {
        testBody.invoke(scope)
    } finally {
        scope.cancelAll()
    }
}

sealed class JobStatus {
    data object Collecting : JobStatus()
    data object Completed : JobStatus()
    data object Cancelled : JobStatus()
    data class Failed(val error: Throwable) : JobStatus()
}

interface CollectState<T> {
    val collectedItems: List<T>
    val count: Int
    val lastItem: T
    val jobStatus: JobStatus
    val hasItems: Boolean get() = count > 0

    fun cancel()
}

class FlowTest(
    private val scope: TestScope
) {

    private val allJobs =
        mutableMapOf<Flow<*>, MutableList<Job>>()

    fun testScope(): TestScope = scope

    fun advanceUntilIdle() {
        testScope().advanceUntilIdle()
    }

    fun advanceTimeBy(millis: Long) {
        testScope().advanceTimeBy(millis)
    }

    fun runCurrent() {
        testScope().runCurrent()
    }

    fun <T> Flow<T>.startCollecting(unconfined: Boolean = true): CollectState<T> {
        val state = CollectStateImpl<T>(
            collectedItems = mutableListOf(),
            jobStatus = JobStatus.Collecting
        )
        val context = if (unconfined) {
            UnconfinedTestDispatcher()
        } else {
            EmptyCoroutineContext
        }
        val job = scope.backgroundScope.launch(context) {
            try {
                toList(state.collectedItems)
                state.jobStatus = JobStatus.Completed
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    state.jobStatus = JobStatus.Cancelled
                    throw e
                } else {
                    state.jobStatus = JobStatus.Failed(e)
                }
            }
        }
        state.job = job
        val jobs = allJobs[this] ?: mutableListOf<Job>().also {
            allJobs[this] = it
        }
        jobs.add(job)

        return state
    }

    fun <T> Flow<T>.startCollectingToList(unconfined: Boolean = true): List<T> {
        return startCollecting(unconfined).collectedItems
    }

    fun <T> Flow<T>.cancelCollecting() {
        allJobs[this]?.forEach { it.cancel() }
        allJobs[this]?.clear()
    }

    fun cancelAll() {
        allJobs.forEach {
            it.key.cancelCollecting()
        }
    }

    private class CollectStateImpl<T>(
        override val collectedItems: MutableList<T>,
        override var jobStatus: JobStatus,
    ): CollectState<T> {

        override val lastItem: T get() = collectedItems.last()
        override val count: Int get() = collectedItems.size
        var job: Job? = null

        override fun cancel() {
            job?.cancel()
        }
    }
}
