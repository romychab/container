package com.elveum.container.subject.paging.internal

import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.resume

class PageLoaderStateTest {

    @RelaxedMockK
    private lateinit var pageResultsEmitter: PageResultsEmitter<Int, String>

    @RelaxedMockK
    private lateinit var store: PageLoaderRecordStore<Int, String>

    private lateinit var pageLoaderState: PageLoaderState<Int, String>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        pageLoaderState = PageLoaderState(pageResultsEmitter, store)
    }

    @Test
    fun processKey_registersKeyInStore() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record

        pageLoaderState.processKey(1, job) { }

        verify(exactly = 1) { store.getOrPut(1, any()) }
    }

    @Test
    fun processKey_callsContinueKeyBeforeBlock() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record
        var continueCalledBeforeBlock = false
        coEvery { store.onKeyContinued(1) } answers {
            continueCalledBeforeBlock = true
        }

        pageLoaderState.processKey(1, job) {
            assertTrue(continueCalledBeforeBlock)
        }
    }

    @Test
    fun processKey_emitsResultsAfterContinueKey() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record

        pageLoaderState.processKey(1, job) { }

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            store.onKeyContinued(1)
            pageResultsEmitter.emitResults()
        }
    }

    @Test
    fun processKey_callsCompleteKeyAfterBlock() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record

        pageLoaderState.processKey(1, job) { }

        coVerify(exactly = 1) { store.onKeyCompleted() }
    }

    @Test
    fun processKey_emitsResultsAfterCompleteKey() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record

        pageLoaderState.processKey(1, job) { }

        coVerify(exactly = 1) { pageResultsEmitter.emitResults() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun processKey_onException_callsFailKey() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record
        // not all containers are errors, so retry path is taken
        every { store.getAllContainers() } returns listOf(pendingContainer())
        val exception = RuntimeException("test error")
        var callCount = 0

        backgroundScope.launch {
            pageLoaderState.processKey(1, job) {
                callCount++
                if (callCount == 1) throw exception
            }
        }
        runCurrent()

        coVerify(exactly = 1) { store.onKeyFailed(1, exception) }

        // Resume to let processKey retry and complete
        record.retryContinuation?.resume(Unit)
        runCurrent()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun processKey_onException_suspendsUntilRetry() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record
        // not all containers are errors, so retry path is taken
        every { store.getAllContainers() } returns listOf(pendingContainer())
        var blockCallCount = 0

        val processJob = backgroundScope.launch {
            pageLoaderState.processKey(1, job) {
                blockCallCount++
                if (blockCallCount == 1) throw RuntimeException("error")
            }
        }
        runCurrent()

        // Block was called once and then suspended
        assertEquals(1, blockCallCount)
        assertFalse(processJob.isCompleted)

        // Resume retry
        record.retryContinuation?.resume(Unit)
        runCurrent()

        // Block called again, succeeds this time
        assertEquals(2, blockCallCount)
        assertTrue(processJob.isCompleted)
    }

    @Test
    fun processKey_onException_whenAllContainersError_callsShutdown() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record
        every { store.getAllContainers() } returns listOf(
            errorContainer(RuntimeException("e1")),
            errorContainer(RuntimeException("e2")),
        )

        pageLoaderState.processKey(1, job) {
            throw RuntimeException("error")
        }

        verify(exactly = 1) { store.shutdown() }
    }

    @Test
    fun processKey_onException_whenAllContainersError_breaksWithoutRetry() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record
        every { store.getAllContainers() } returns listOf(
            errorContainer(RuntimeException("e")),
        )
        var blockCallCount = 0

        pageLoaderState.processKey(1, job) {
            blockCallCount++
            throw RuntimeException("error")
        }

        // Block called only once - no retry
        assertEquals(1, blockCallCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun processKey_onException_whenNotAllContainersError_doesNotCallShutdown() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record
        every { store.getAllContainers() } returns listOf(
            successContainer(listOf("a")),
            errorContainer(RuntimeException("e")),
        )
        var callCount = 0

        backgroundScope.launch {
            pageLoaderState.processKey(1, job) {
                callCount++
                if (callCount == 1) throw RuntimeException("error")
            }
        }
        runCurrent()

        verify(exactly = 0) { store.shutdown() }

        // Cleanup: resume retry
        record.retryContinuation?.resume(Unit)
        runCurrent()
    }

    @Test
    fun processKey_onCancellationException_rethrowsWithoutRetry() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record

        val result = runCatching {
            pageLoaderState.processKey(1, job) {
                throw CancellationException("cancelled")
            }
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
        coVerify(exactly = 0) { store.onKeyFailed(any(), any()) }
    }

    @Test
    fun processKey_resetsImmediateLaunchScheduled() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record

        // Schedule immediate launch first
        every { store.findNextKeyForIndex(0) } returns
                PageLoaderRecordStore.NextKeyLoadResult.ScheduleImmediateLoad
        pageLoaderState.findNextKeyForIndex(0)
        assertTrue(pageLoaderState.isImmediateLaunchScheduled())

        pageLoaderState.processKey(1, job) { }

        assertFalse(pageLoaderState.isImmediateLaunchScheduled())
    }

    @Test
    fun onKeyLoaded_updatesContainerInStore() = runTest {
        val list = listOf("a", "b")

        pageLoaderState.onKeyLoaded(1, list)

        verify(exactly = 1) { store.updateContainer(1, successContainer(list)) }
    }

    @Test
    fun onKeyLoaded_emitsResults() = runTest {
        pageLoaderState.onKeyLoaded(1, listOf("a"))

        coVerify(exactly = 1) { pageResultsEmitter.emitResults() }
    }

    @Test
    fun await_delegatesToStore() = runTest {
        pageLoaderState.await()

        coVerify(exactly = 1) { store.await() }
    }

    @Test
    fun registerKey_delegatesToStoreGetOrPut() {
        val record = PageKeyRecord<Int, String>(1)
        every { store.getOrPut(1, any()) } returns record

        val result = pageLoaderState.registerKey(1)

        assertEquals(record, result)
    }

    @Test
    fun registerKey_withJob_passesJobToNewRecord() {
        val job = Job()
        val slot = mutableListOf<() -> PageKeyRecord<Int, String>>()
        every { store.getOrPut(1, capture(slot)) } answers {
            slot.last().invoke()
        }

        val result = pageLoaderState.registerKey(1, job)!!

        assertEquals(1, result.key)
        assertEquals(job, result.job)
    }

    @Test
    fun registerKey_updatesJobForPreRegisteredKey() {
        val preRegisteredRecord = PageKeyRecord<Int, String>(1, job = null)
        every { store.getOrPut(1, any()) } returns preRegisteredRecord
        val job = Job()

        val result = pageLoaderState.registerKey(1, job)!!

        assertEquals(job, result.job)
    }

    @Test
    fun registerKey_afterShutdown_returnsNull() {
        every { store.getOrPut(1, any()) } returns null

        val result = pageLoaderState.registerKey(1)

        assertNull(result)
    }

    @Test
    fun processKey_afterShutdown_returnsImmediately() = runTest {
        val job = Job()
        every { store.getOrPut(1, any()) } returns null
        var blockCalled = false

        pageLoaderState.processKey(1, job) { blockCalled = true }

        assertFalse(blockCalled)
        coVerify(exactly = 0) { store.onKeyContinued(any()) }
        coVerify(exactly = 0) { store.onKeyCompleted() }
    }

    @Test
    fun processKey_onException_emitsResultsAfterFailKey() = runTest {
        val job = Job()
        val record = PageKeyRecord<Int, String>(1, job = job)
        every { store.getOrPut(1, any()) } returns record
        every { store.getAllContainers() } returns listOf(
            errorContainer(RuntimeException("e")),
        )

        pageLoaderState.processKey(1, job) {
            throw RuntimeException("error")
        }

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            store.onKeyFailed(1, any())
            pageResultsEmitter.emitResults()
        }
    }

    @Test
    fun registerKey_withoutJob_doesNotOverwriteExistingJob() {
        val existingJob = Job()
        val preRegisteredRecord = PageKeyRecord<Int, String>(1, job = existingJob)
        every { store.getOrPut(1, any()) } returns preRegisteredRecord

        val result = pageLoaderState.registerKey(1)

        assertEquals(existingJob, result?.job)
    }

    @Test
    fun findNextKeyForIndex_whenKeyResult_returnsKey() {
        every { store.findNextKeyForIndex(5) } returns
                PageLoaderRecordStore.NextKeyLoadResult.Key(2)

        val result = pageLoaderState.findNextKeyForIndex(5)

        assertEquals(2, result)
    }

    @Test
    fun findNextKeyForIndex_whenSkipResult_returnsNull() {
        every { store.findNextKeyForIndex(5) } returns
                PageLoaderRecordStore.NextKeyLoadResult.Skip

        val result = pageLoaderState.findNextKeyForIndex(5)

        assertNull(result)
    }

    @Test
    fun findNextKeyForIndex_whenScheduleImmediateLoad_returnsNullAndSchedulesImmediateLaunch() {
        every { store.findNextKeyForIndex(5) } returns
                PageLoaderRecordStore.NextKeyLoadResult.ScheduleImmediateLoad

        val result = pageLoaderState.findNextKeyForIndex(5)

        assertNull(result)
        assertTrue(pageLoaderState.isImmediateLaunchScheduled())
    }

    @Test
    fun findNextKeyForIndex_whenSkip_doesNotScheduleImmediateLaunch() {
        every { store.findNextKeyForIndex(5) } returns
                PageLoaderRecordStore.NextKeyLoadResult.Skip

        pageLoaderState.findNextKeyForIndex(5)

        assertFalse(pageLoaderState.isImmediateLaunchScheduled())
    }

    @Test
    fun hasLaunchedKey_delegatesToStore() {
        every { store.hasLaunchedKey(1) } returns true

        assertTrue(pageLoaderState.hasLaunchedKey(1))
    }

    @Test
    fun hasLaunchedKey_returnsFalseWhenStoreReturnsFalse() {
        every { store.hasLaunchedKey(1) } returns false

        assertFalse(pageLoaderState.hasLaunchedKey(1))
    }

    @Test
    fun isImmediateLaunchScheduled_initiallyFalse() {
        assertFalse(pageLoaderState.isImmediateLaunchScheduled())
    }

}
