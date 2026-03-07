package com.elveum.container.subject.paging.internal

import com.elveum.container.ContainerMetadata
import com.elveum.container.StatefulEmitter
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.subject.paging.NextPageStateMetadata
import com.elveum.container.subject.paging.PageState
import com.elveum.container.successContainer
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class PageResultsEmitterTest {

    @RelaxedMockK
    private lateinit var store: PageLoaderRecordStore<Int, String>

    @RelaxedMockK
    private lateinit var emitter: StatefulEmitter<List<String>>

    private lateinit var metadataProvider: () -> ContainerMetadata

    private lateinit var pageResultsEmitter: PageResultsEmitter<Int, String>

    private var lastPageState: PageState? = null

    private val customMetadata = NextPageStateMetadata(PageState.Pending)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        metadataProvider = { customMetadata }
        lastPageState = null
        pageResultsEmitter = PageResultsEmitter(
            store = store,
            emitter = emitter,
            metadataProvider = { customMetadata },
            onNextPageStateChanged = { lastPageState = it },
        )
    }

    @Test
    fun emitResults_whenNoContainers_emitsPendingState() = runTest {
        every { store.getAllContainers() } returns emptyList()
        TestScope(testScheduler)
        pageResultsEmitter.emitResults()

        coVerify(exactly = 1) { emitter.emitPendingState() }
    }

    @Test
    fun emitResults_whenAllContainersPending_emitsPendingState() = runTest {
        every { store.getAllContainers() } returns listOf(
            pendingContainer(),
            pendingContainer(),
        )

        pageResultsEmitter.emitResults()

        coVerify(exactly = 1) { emitter.emitPendingState() }
    }

    @Test
    fun emitResults_whenNoContainsAvailable_doesNotCallOnNextPageStateChanged() = runTest {
        every { store.getAllContainers() } returns emptyList()

        pageResultsEmitter.emitResults()

        assertNull(lastPageState)
    }

    @Test
    fun emitResults_whenNoContainsAvailable_doesNotEmitOutputList() = runTest {
        every { store.getAllContainers() } returns emptyList()

        pageResultsEmitter.emitResults()

        coVerify(exactly = 0) { emitter.emit(any<List<String>>()) }
    }

    @Test
    fun emitResults_whenAllContainersPending_doesNotEmitOutputList() = runTest {
        every { store.getAllContainers() } returns listOf(
            pendingContainer(),
        )

        pageResultsEmitter.emitResults()

        coVerify(exactly = 0) { emitter.emit(any<List<String>>()) }
    }

    @Test
    fun emitResults_whenAllContainersError_emitsFailureStateWithFirstException() = runTest {
        val exception1 = RuntimeException("error1")
        val exception2 = RuntimeException("error2")
        every { store.getAllContainers() } returns listOf(
            errorContainer(exception1),
            errorContainer(exception2),
        )

        pageResultsEmitter.emitResults()

        coVerify(exactly = 1) { emitter.emitFailureState(exception1) }
    }

    @Test
    fun emitResults_whenAllContainersError_doesNotCallOnNextPageStateChanged() = runTest {
        every { store.getAllContainers() } returns listOf(
            errorContainer(RuntimeException("error")),
        )

        pageResultsEmitter.emitResults()

        assertNull(lastPageState)
    }

    @Test
    fun emitResults_whenAllContainersError_doesNotEmitOutputList() = runTest {
        every { store.getAllContainers() } returns listOf(
            errorContainer(RuntimeException("error")),
        )

        pageResultsEmitter.emitResults()

        coVerify(exactly = 0) { emitter.emit(any<List<String>>()) }
    }

    @Test
    fun emitResults_whenMixedWithError_emitsPageStateError() = runTest {
        val exception = RuntimeException("page error")
        val errorRecord = PageKeyRecord<Int, String>(2, container = errorContainer(exception))
        every { store.getAllContainers() } returns listOf(
            successContainer(listOf("a")),
            errorContainer(exception),
        )
        every { store.firstOrNull(any()) } returns errorRecord

        pageResultsEmitter.emitResults()

        assertTrue(lastPageState is PageState.Error)
        assertEquals(exception, (lastPageState as PageState.Error).exception)
    }

    @Test
    fun emitResults_whenMixedWithError_emitsOutputList() = runTest {
        val exception = RuntimeException("page error")
        val errorRecord = PageKeyRecord<Int, String>(2, container = errorContainer(exception))
        every { store.getAllContainers() } returns listOf(
            successContainer(listOf("a", "b")),
            errorContainer(exception),
        )
        every { store.firstOrNull(any()) } returns errorRecord
        every { store.buildOutputList() } returns listOf("a", "b")

        pageResultsEmitter.emitResults()

        coVerify(exactly = 1) { emitter.emit(listOf("a", "b"), customMetadata) }
    }

    @Test
    fun emitResults_whenMixedWithPending_emitsPageStatePending() = runTest {
        every { store.getAllContainers() } returns listOf(
            successContainer(listOf("a")),
            pendingContainer(),
        )
        every { store.firstOrNull(any()) } returns null
        every { store.buildOutputList() } returns listOf("a")

        pageResultsEmitter.emitResults()

        assertEquals(PageState.Pending, lastPageState)
    }

    @Test
    fun emitResults_whenMixedWithPending_emitsOutputList() = runTest {
        every { store.getAllContainers() } returns listOf(
            successContainer(listOf("a")),
            pendingContainer(),
        )
        every { store.firstOrNull(any()) } returns null
        every { store.buildOutputList() } returns listOf("a")

        pageResultsEmitter.emitResults()

        coVerify(exactly = 1) { emitter.emit(listOf("a"), customMetadata) }
    }

    @Test
    fun emitResults_whenAllSuccess_emitsPageStateIdle() = runTest {
        every { store.getAllContainers() } returns listOf(
            successContainer(listOf("a")),
            successContainer(listOf("b")),
        )
        every { store.firstOrNull(any()) } returns null
        every { store.buildOutputList() } returns listOf("a", "b")

        pageResultsEmitter.emitResults()

        assertEquals(PageState.Idle, lastPageState)
    }

    @Test
    fun emitResults_whenAllSuccess_emitsOutputList() = runTest {
        every { store.getAllContainers() } returns listOf(
            successContainer(listOf("a")),
            successContainer(listOf("b")),
        )
        every { store.firstOrNull(any()) } returns null
        every { store.buildOutputList() } returns listOf("a", "b")

        pageResultsEmitter.emitResults()

        coVerify(exactly = 1) { emitter.emit(listOf("a", "b"), customMetadata) }
    }

    @Test
    fun emitResults_whenPageStateIsError_retryResumesAllContinuations() = runTest {
        val exception1 = RuntimeException("page error 1")
        val exception2 = RuntimeException("page error 2")
        val continuation1 = mockk<Continuation<Unit>>(relaxed = true)
        val continuation2 = mockk<Continuation<Unit>>(relaxed = true)
        val errorRecord1 = PageKeyRecord<Int, String>(2, container = errorContainer(exception1), retryContinuation = continuation1)
        val errorRecord2 = PageKeyRecord<Int, String>(2, container = errorContainer(exception2), retryContinuation = continuation2)
        every { store.getAllContainers() } returns listOf(
            successContainer(listOf("a")),
            errorContainer(exception1),
            errorContainer(exception2),
        )
        every { store.firstOrNull(any()) } returns errorRecord1
        every { store.buildOutputList() } returns listOf("a")

        pageResultsEmitter.emitResults()

        val errorState = lastPageState as PageState.Error
        errorState.retry()

        val capturedLambda = slot<(PageKeyRecord<Int, String>) -> Unit>()
        verify(exactly = 1) { store.forEach(capture(capturedLambda)) }
        capturedLambda.captured.invoke(errorRecord1)
        capturedLambda.captured.invoke(errorRecord2)
        verify(exactly = 1) {
            continuation1.resume(Unit)
            continuation2.resume(Unit)
        }
    }

}
