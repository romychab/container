package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.Emitter
import com.elveum.container.EmptyMetadata
import com.elveum.container.LoadTrigger
import com.elveum.container.LoadTriggerMetadata
import com.elveum.container.LoadUuidMetadata
import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.container.SourceType
import com.elveum.container.SourceTypeMetadata
import com.elveum.container.exceptionOrNull
import com.elveum.container.get
import com.elveum.container.loadTrigger
import com.elveum.container.sourceType
import com.elveum.container.subject.FlowSubject
import com.elveum.container.subject.ValueLoader
import com.elveum.container.successContainer
import com.elveum.container.utils.MockFlowEmitterCreator
import com.uandcode.flowtest.CollectStatus
import com.uandcode.flowtest.runFlowTest
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
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

    @MockK
    private lateinit var flowDependencyStore: FlowDependencyStore

    private lateinit var valueLoader: ValueLoader<String>

    private lateinit var executeParams: LoadTask.ExecuteParams<String>

    @Before
    fun setUp() {
        valueLoader = mockk(relaxed = true)
        MockKAnnotations.init(this, relaxed = true)
        executeParams = LoadTask.ExecuteParams(flowDependencyStore = flowDependencyStore)
    }

    @Test
    fun instantLoadTask_emitsDataImmediately() = runFlowTest {
        val task = LoadTask.Instant(successContainer("item"))

        val flow = task.execute(executeParams)
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

        val state = task.execute(executeParams).startCollecting(StandardTestDispatcher(scope.testScheduler))
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

        val state = task.execute(executeParams).startCollecting(StandardTestDispatcher(scope.testScheduler))
        advanceTimeBy(5)

        assertEquals(emptyList<Container<String>>(), state.collectedItems)
        assertEquals(CollectStatus.Collecting, state.collectStatus)
    }

    @Test
    fun loadTask_withSilentFlagAndCurrentContainer_emitsCurrentContainerWithIsLoadingInBackgroundTrue() = runFlowTest {
        val currentContainer = successContainer("current-item")
        val task = makeLoadTask(silent = true)
        coEvery { valueLoader.invoke(any()) } coAnswers { delay(10) }

        val state = task.execute(
            LoadTask.ExecuteParams(
                currentContainer = { currentContainer },
                flowDependencyStore = flowDependencyStore,
            )
        ).startCollecting(StandardTestDispatcher(scope.testScheduler))
        advanceTimeBy(5)

        assertEquals(1, state.collectedItems.size)
        val emitted = state.collectedItems.first() as Container.Success<*>
        assertEquals("current-item", emitted.value)
        assertTrue(emitted.isLoadingInBackground)
    }

    @Test
    fun loadTask_execute_passesExecuteParamsToFlowEmitterCreator() = runFlowTest {
        var capturedParams: LoadTask.ExecuteParams<String>? = null
        val task = makeLoadTask(
            flowEmitterCreator = MockFlowEmitterCreator { _, params ->
                capturedParams = params
                flowEmitter
            }
        )
        every { flowEmitter.hasEmittedValues } returns true
        val executeParams = LoadTask.ExecuteParams<String>(loadUuid = "test-uuid", flowDependencyStore = flowDependencyStore)

        task.execute(executeParams).startCollecting()

        assertSame(executeParams, capturedParams)
    }

    @Test
    fun loadTask_withoutEmittedItemsAndWithSilentFlag_emitsError() = runFlowTest {
        val task = makeLoadTask(silent = true)
        coEvery { valueLoader.invoke(any()) } coAnswers {
            delay(10)
        }

        val state = task.execute(executeParams).startCollecting()
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

        val state = task.execute(executeParams).startCollecting()
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

        val state = task.execute(executeParams).startCollecting()

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

        val state = task.execute(executeParams).startCollecting()

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

        val state = task.execute(executeParams).startCollecting()

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

        val state = task.execute(executeParams).startCollecting()

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
            flowEmitterCreator = MockFlowEmitterCreator { collector, _ ->
                flowCollector = collector
                flowEmitter
            }
        )
        every { flowEmitter.hasEmittedValues } returns true
        coEvery { flowEmitter.emit(any(), any<SourceType>()) } coAnswers {
            flowCollector?.emit(successContainer( firstArg(), secondArg<SourceType>()))
        }
        coEvery { valueLoader.invoke(any()) } coAnswers {
            firstArg<Emitter<String>>().emit("111", LocalSourceType)
            firstArg<Emitter<String>>().emit("222", RemoteSourceType)
        }

        val state = task.execute(executeParams).startCollecting()

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
            flowEmitterCreator = MockFlowEmitterCreator { collector, _ ->
                flowCollector = collector
                flowEmitter
            }
        )
        every { flowEmitter.hasEmittedValues } returns true
        coEvery { flowEmitter.emit(any(), any<SourceType>()) } coAnswers {
            flowCollector?.emit(successContainer( firstArg(), secondArg<SourceType>()))
        }
        coEvery { valueLoader.invoke(any()) } coAnswers {
            firstArg<Emitter<String>>().emit("111", LocalSourceType)
            firstArg<Emitter<String>>().emit("222", RemoteSourceType)
        }

        val state = task.execute(executeParams).startCollecting()

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

        loadTask.cancel("cancelled")

        verify(exactly = 1) {
            flowSubject.onError(match { it is CancellationException })
        }
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

    @Test
    fun loadTask_metadata_returnsStoredMetadata() {
        val metadata = SourceTypeMetadata(RemoteSourceType)

        val loadTask = makeLoadTask(metadata = metadata)

        assertEquals(metadata, loadTask.metadata)
    }

    @Test
    fun instantTask_metadata_returnsInitialContainerMetadata() {
        val metadata = SourceTypeMetadata(LocalSourceType)
        val container = successContainer("item", metadata)

        val loadTask = LoadTask.Instant(container)

        assertEquals(metadata, loadTask.metadata)
    }

    @Test
    fun loadTask_restoreLoadTask_returnsNewTaskWithCombinedMetadata() {
        val loadTask = makeLoadTask(metadata = SourceTypeMetadata(LocalSourceType))

        val newTask = loadTask.restoreLoadTask(LoadUuidMetadata("uuid-1"))

        assertEquals(LocalSourceType, newTask.metadata.sourceType)
        assertEquals(LoadUuidMetadata("uuid-1"), newTask.metadata.get<LoadUuidMetadata>())
    }

    @Test
    fun loadTask_restoreLoadTask_extraMetadataOverridesOriginalMetadataOfSameType() {
        val loadTask = makeLoadTask(metadata = SourceTypeMetadata(LocalSourceType))

        val newTask = loadTask.restoreLoadTask(SourceTypeMetadata(RemoteSourceType))

        assertEquals(RemoteSourceType, newTask.metadata.sourceType)
    }

    @Test
    fun loadTask_restoreLoadTask_copiesLoaderAndMetadataToNewTask() {
        val loadMetadata = SourceTypeMetadata(RemoteSourceType)
        val loadTask = makeLoadTask(metadata = loadMetadata)

        val newTask = loadTask.restoreLoadTask(EmptyMetadata)

        assertSame(valueLoader, newTask.lastRealLoader)
        assertSame(loadMetadata, newTask.lastRealMetadata)
    }

    @Test
    fun instantTask_restoreLoadTask_returnsSameInstance() {
        val loadTask = LoadTask.Instant(successContainer("item"))

        val newTask = loadTask.restoreLoadTask(SourceTypeMetadata(RemoteSourceType))

        assertSame(loadTask, newTask)
    }

    @Test
    fun instantTask_restoreLoadTask_withRealLoader_restoresLoadTask() {
        val loadTask = LoadTask.Instant(
            initialContainer = successContainer("item"),
            lastRealLoader = valueLoader,
            lastRealMetadata = SourceTypeMetadata(RemoteSourceType)
        )

        val newTask = loadTask.restoreLoadTask(LoadTriggerMetadata(LoadTrigger.CacheExpired))

        newTask as LoadTask.Load<String>
        assertEquals(LoadTrigger.CacheExpired, newTask.metadata.loadTrigger)
        assertEquals(RemoteSourceType, newTask.metadata.sourceType)
        assertSame(valueLoader, newTask.lastRealLoader)
    }

    @Test
    fun instantTask_restoreLoadTask_withRealLoader_overridesMetadataOfSameType() {
        val loadTask = LoadTask.Instant(
            initialContainer = successContainer("item"),
            lastRealLoader = valueLoader,
            lastRealMetadata = LoadTriggerMetadata(LoadTrigger.NewLoad),
        )

        val newTask = loadTask.restoreLoadTask(LoadTriggerMetadata(LoadTrigger.CacheExpired))

        newTask as LoadTask.Load<String>
        assertEquals(LoadTrigger.CacheExpired, newTask.metadata.loadTrigger)
    }

    @Test
    fun loadTask_execute_passesMetadataToFlowEmitter() = runFlowTest {
        val task = LoadTask.Load(
            loader = { emit("item", isLastValue = true) },
            metadata = SourceTypeMetadata(RemoteSourceType),
        )

        val state = task.execute(executeParams).startCollecting()

        val emittedContainer = state.collectedItems
            .filterIsInstance<Container.Success<String>>()
            .first()
        assertEquals(RemoteSourceType, emittedContainer.metadata.sourceType)
    }

    private fun makeLoadTask(
        metadata: ContainerMetadata = EmptyMetadata,
        silent: Boolean = false,
        flowEmitterCreator: LoadTask.FlowEmitterCreator<String> = MockFlowEmitterCreator { _, _ -> flowEmitter }
    ): LoadTask<String> {
        return LoadTask.Load(
            metadata = metadata,
            loader = valueLoader,
            silent = silent,
            flowSubject = flowSubject,
            flowEmitterCreator = flowEmitterCreator,
        )
    }

}