package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.Emitter
import com.elveum.container.LoadTrigger
import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.container.exceptionOrNull
import com.elveum.container.subject.FlowSubject
import com.elveum.container.subject.ValueLoader
import com.elveum.container.successContainer
import com.uandcode.flowtest.CollectStatus
import com.uandcode.flowtest.runFlowTest
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoadTaskTest {

    @MockK
    private lateinit var flowSubject: FlowSubject<String>

    @MockK
    private lateinit var flowEmitter: FlowEmitter<String>

    private lateinit var valueLoader: ValueLoader<String>

    @Before
    fun setUp() {
        valueLoader = mockk(relaxed = true)
        MockKAnnotations.init(this, relaxed = true)
    }

    @Test
    fun instantLoadTask_emitsDataImmediately() = runFlowTest {
        val task = LoadTask.Instant(successContainer("item"))

        val flow = task.execute()
        val state = flow.startCollecting()
        advanceUntilIdle()

        assertEquals(CollectStatus.Completed, state.collectStatus)
        assertEquals(
            listOf(successContainer("item")),
            state.collectedItems,
        )
    }

    @Test
    fun loadTask_withoutSilentFlag_emitsPendingStatus() = runFlowTest {
        val task = makeLoadTask(silent = false)
        coEvery { valueLoader.invoke(any()) } coAnswers {
            delay(10)
        }

        val state = task.execute().startCollecting(StandardTestDispatcher(scope.testScheduler))
        advanceTimeBy(5)

        assertEquals(
            listOf(Container.Pending),
            state.collectedItems,
        )
        assertEquals(CollectStatus.Collecting, state.collectStatus)
    }

    @Test
    fun loadTask_withSilentFlag_doesNotEmitPendingStatus() = runFlowTest {
        val task = makeLoadTask(silent = true)
        coEvery { valueLoader.invoke(any()) } coAnswers {
            delay(10)
        }

        val state = task.execute().startCollecting(StandardTestDispatcher(scope.testScheduler))
        advanceTimeBy(5)

        assertEquals(emptyList<Container<String>>(), state.collectedItems)
        assertEquals(CollectStatus.Collecting, state.collectStatus)
    }

    @Test
    fun loadTask_withoutEmittedItemsAndWithSilentFlag_emitsError() = runFlowTest {
        val task = makeLoadTask(silent = true)
        coEvery { valueLoader.invoke(any()) } coAnswers {
            delay(10)
        }

        val state = task.execute().startCollecting()
        advanceTimeBy(11)

        val exception = state.collectedItems.last().exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        verify(exactly = 1) {
            flowSubject.onError(refEq(exception!!))
        }
    }

    @Test
    fun loadTask_withoutEmittedItemsAndWithoutSilentFlag_emitsError() = runFlowTest {
        val task = makeLoadTask(silent = false)
        coEvery { valueLoader.invoke(any()) } coAnswers {
            delay(10)
        }

        val state = task.execute().startCollecting()
        advanceTimeBy(11)

        assertEquals(2, state.collectedItems.size)
        assertEquals(Container.Pending, state.collectedItems.first())
        val exception = state.collectedItems.last().exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        verify(exactly = 1) {
            flowSubject.onError(refEq(exception!!))
        }
    }

    @Test
    fun loadTask_withCancellationExceptionAndSilentFlag_fails() = runFlowTest {
        val task = makeLoadTask(silent = true)
        val cancellationException = CancellationException()
        coEvery { valueLoader.invoke(any()) } coAnswers {
            throw cancellationException
        }

        val state = task.execute().startCollecting()

        assertEquals(CollectStatus.Cancelled, state.collectStatus)
        assertTrue(state.collectedItems.isEmpty())
        verify(exactly = 1) {
            flowSubject.onError(refEq(cancellationException))
        }
    }

    @Test
    fun loadTask_withCancellationExceptionAndWithoutSilentFlag_fails() = runFlowTest {
        val task = makeLoadTask(silent = false)
        val cancellationException = CancellationException()
        coEvery { valueLoader.invoke(any()) } coAnswers {
            throw cancellationException
        }

        val state = task.execute().startCollecting()

        assertEquals(CollectStatus.Cancelled, state.collectStatus)
        assertEquals(
            listOf(Container.Pending),
            state.collectedItems,
        )
        verify(exactly = 1) {
            flowSubject.onError(refEq(cancellationException))
        }
    }

    @Test
    fun loadTask_withExceptionAndSilentFlag_emitsError() = runFlowTest {
        val task = makeLoadTask(silent = true)
        val expectedException = IllegalArgumentException()
        coEvery { valueLoader.invoke(any()) } coAnswers {
            throw expectedException
        }

        val state = task.execute().startCollecting()

        assertEquals(CollectStatus.Completed, state.collectStatus)
        val exception = state.collectedItems.last().exceptionOrNull()
        assertSame(expectedException, exception)
        verify(exactly = 1) {
            flowSubject.onError(refEq(expectedException))
        }
    }


    @Test
    fun loadTask_withExceptionAndWithoutSilentFlag_emitsError() = runFlowTest {
        val task = makeLoadTask(silent = false)
        val expectedException = IllegalArgumentException()
        coEvery { valueLoader.invoke(any()) } coAnswers {
            throw expectedException
        }

        val state = task.execute().startCollecting()

        assertEquals(CollectStatus.Completed, state.collectStatus)
        assertEquals(2, state.collectedItems.size)
        assertEquals(Container.Pending, state.collectedItems.first())
        val exception = state.collectedItems.last().exceptionOrNull()
        assertSame(expectedException, exception)
        verify(exactly = 1) {
            flowSubject.onError(refEq(expectedException))
        }
    }

    @Test
    fun loadTask_withEmittedItemsAndSilentFlag_completes() = runFlowTest {
        var flowCollector: FlowCollector<Container<String>>? = null
        val task = makeLoadTask(
            silent = true,
            flowEmitterCreator = {
                flowCollector = it
                flowEmitter
            }
        )
        every { flowEmitter.hasEmittedValues } returns true
        coEvery { flowEmitter.emit(any(), any()) } coAnswers {
            flowCollector?.emit(successContainer( firstArg(), secondArg()))
        }
        coEvery { valueLoader.invoke(any()) } coAnswers {
            firstArg<Emitter<String>>().emit("111", LocalSourceType)
            firstArg<Emitter<String>>().emit("222", RemoteSourceType)
        }

        val state = task.execute().startCollecting()

        assertEquals(
            listOf(
                successContainer("111", LocalSourceType),
                successContainer("222", RemoteSourceType),
            ),
            state.collectedItems
        )
        assertEquals(CollectStatus.Completed, state.collectStatus)
        verify(exactly = 1) {
            flowSubject.onComplete()
        }
    }

    @Test
    fun loadTask_withEmittedItemsAndWithoutSilentFlag_completes() = runFlowTest {
        var flowCollector: FlowCollector<Container<String>>? = null
        val task = makeLoadTask(
            silent = false,
            flowEmitterCreator = {
                flowCollector = it
                flowEmitter
            }
        )
        every { flowEmitter.hasEmittedValues } returns true
        coEvery { flowEmitter.emit(any(), any()) } coAnswers {
            flowCollector?.emit(successContainer( firstArg(), secondArg()))
        }
        coEvery { valueLoader.invoke(any()) } coAnswers {
            firstArg<Emitter<String>>().emit("111", LocalSourceType)
            firstArg<Emitter<String>>().emit("222", RemoteSourceType)
        }

        val state = task.execute().startCollecting()

        assertEquals(
            listOf(
                Container.Pending,
                successContainer("111", LocalSourceType),
                successContainer("222", RemoteSourceType),
            ),
            state.collectedItems
        )
        assertEquals(CollectStatus.Completed, state.collectStatus)
        verify(exactly = 1) {
            flowSubject.onComplete()
        }
    }

    @Test
    fun loadTask_returnsCurrentLoaderAsLastLoader() {
        val loadTask = makeLoadTask()
        assertSame(valueLoader, loadTask.lastRealLoader)
    }

    @Test
    fun instantTask_returnsPresetLastLoader() {
        val loadTask = LoadTask.Instant(successContainer("1"), valueLoader)
        assertSame(valueLoader, loadTask.lastRealLoader)
    }

    @Test
    fun cancel_forLoadTask_emitsCancellationException() {
        val loadTask = makeLoadTask()

        loadTask.cancel()

        verify(exactly = 1) {
            flowSubject.onError(match { it is CancellationException })
        }
    }

    @Test
    fun loadTask_setLoadTrigger_updatesValue() = runFlowTest {
        val loadTask = LoadTask.Load(
            loader = valueLoader,
            loadTrigger = LoadTrigger.NewLoad,
            silent = true,
            flowSubject = flowSubject,
        )
        val emitterSlot = slot<Emitter<String>>()
        loadTask.setLoadTrigger(LoadTrigger.CacheExpired)
        coEvery { valueLoader.invoke(capture(emitterSlot)) } just runs

        loadTask.execute().startCollecting()

        val flowEmitter = emitterSlot.captured as FlowEmitter<String>
        assertEquals(LoadTrigger.CacheExpired, flowEmitter.loadTrigger)
    }

    @Test
    fun loadTask_withSilentFlag_doesNotHaveInitialValue() {
        val loadTask = makeLoadTask(silent = true)
        assertNull(loadTask.initialContainer)
    }

    @Test
    fun loadTask_withoutSilentFlag_hasInitialPendingValue() {
        val loadTask = makeLoadTask(silent = false)
        assertEquals(Container.Pending, loadTask.initialContainer)
    }

    @Test
    fun instantTask_alwaysHasInitialValue() {
        val loadTask = LoadTask.Instant(successContainer("123"))
        assertEquals(successContainer("123"), loadTask.initialContainer)
    }

    private fun makeLoadTask(
        loadTrigger: LoadTrigger = LoadTrigger.NewLoad,
        silent: Boolean = false,
        flowEmitterCreator: (FlowCollector<Container<String>>) -> FlowEmitter<String> = { flowEmitter }
    ): LoadTask<String> {
        return LoadTask.Load(
            loader = valueLoader,
            loadTrigger = loadTrigger,
            silent = silent,
            flowSubject = flowSubject,
            flowEmitterCreator = flowEmitterCreator,
        )
    }
}