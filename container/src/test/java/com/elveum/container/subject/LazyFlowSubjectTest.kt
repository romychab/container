package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.LoadTrigger
import com.elveum.container.LoadTriggerMetadata
import com.elveum.container.LoadUuidMetadata
import com.elveum.container.ReloadDependenciesMetadata
import com.elveum.container.RemoteSourceType
import com.elveum.container.SourceTypeMetadata
import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.container.subject.LazyFlowSubjectImpl.LoadTaskFactory
import com.elveum.container.subject.lazy.LoadTask
import com.elveum.container.subject.lazy.LoadTaskManager
import com.elveum.container.successContainer
import com.uandcode.flowtest.CollectStatus
import com.uandcode.flowtest.runFlowTest
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LazyFlowSubjectTest {

    private val timeoutMillis = 1000L

    @MockK(relaxUnitFun = true)
    private lateinit var loadTaskManager: LoadTaskManager<String>

    @MockK
    private lateinit var loadTaskFactory: LoadTaskFactory

    @RelaxedMockK
    private lateinit var flowDependencyStore: FlowDependencyStoreImpl<String>

    private lateinit var scope: TestScope

    private lateinit var coroutineScopeFactory: CoroutineScopeFactory

    private lateinit var subject: LazyFlowSubjectImpl<String>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        scope = TestScope()
        coroutineScopeFactory = CoroutineScopeFactory { TestScope(scope.testScheduler) }
        subject = LazyFlowSubjectImpl(
            coroutineScopeFactory = coroutineScopeFactory,
            cacheTimeoutMillis = timeoutMillis,
            loadTaskManager = loadTaskManager,
            loadTaskFactory = loadTaskFactory,
            flowDependencyStore = flowDependencyStore,
        )
    }

    @Test
    fun currentValue_returnsValueFromLoadTaskManager() {
        val expectedValue = successContainer("123")
        val flow = MutableStateFlow<Container<String>>(expectedValue)
        every { loadTaskManager.listen() } returns flow

        val resultValue = subject.currentValue()

        assertEquals(expectedValue, resultValue.raw())
    }

    @Test
    fun reload_withoutLoader_doesNothing() {
        every { loadTaskManager.getLastRealLoader() } returns null

        subject.reload()

        verify(exactly = 0) {
            loadTaskFactory.create<String>(any(), any(), any())
            loadTaskManager.submitNewLoadTask(any())
        }
    }

    @Test
    fun reload_withoutLoader_returnsEmptyFlow() = scope.runFlowTest {
        every { loadTaskManager.getLastRealLoader() } returns null

        val state = subject.reload().startCollecting()

        assertEquals(CollectStatus.Completed, state.collectStatus)
        assertTrue(state.collectedItems.isEmpty())
    }

    @Test
    fun reload_withLoader_executesNewLoad() {
        val valueLoader = mockk<ValueLoader<String>>()
        val loadTask = mockk<LoadTask<String>>()
        val flowSubject = mockk<FlowSubject<String>>()
        val expectedFlow = MutableStateFlow("123")
        every { loadTaskManager.getLastRealLoader() } returns valueLoader
        every { flowSubject.flow() } returns expectedFlow
        every {
            loadTaskFactory.create(
                silently = true,
                valueLoader = refEq(valueLoader),
                metadata = LoadTriggerMetadata(LoadTrigger.Reload) + ReloadDependenciesMetadata(true),
            )
        } returns LoadTaskFactory.LoadTaskRecord(loadTask, flowSubject)

        val resultFlow = subject.reload(silently = true)

        verify(exactly = 1) {
            loadTaskManager.submitNewLoadTask(refEq(loadTask))
        }
        assertSame(expectedFlow, resultFlow)
    }

    @Test
    fun reload_withCustomMetadata_combinesMetadataWithReloadTriggerAndDependencies() {
        val valueLoader = mockk<ValueLoader<String>>()
        val loadTask = mockk<LoadTask<String>>()
        val flowSubject = mockk<FlowSubject<String>>()
        val customMetadata = LoadUuidMetadata("custom-uuid")
        every { loadTaskManager.getLastRealLoader() } returns valueLoader
        every { flowSubject.flow() } returns MutableStateFlow("123")
        every {
            loadTaskFactory.create(
                silently = false,
                valueLoader = refEq(valueLoader),
                metadata = LoadTriggerMetadata(LoadTrigger.Reload) +
                        ReloadDependenciesMetadata(true) +
                        customMetadata,
            )
        } returns LoadTaskFactory.LoadTaskRecord(loadTask, flowSubject)

        subject.reload(metadata = customMetadata)

        verify(exactly = 1) {
            loadTaskManager.submitNewLoadTask(refEq(loadTask))
        }
    }

    @Test
    fun updateWith_executesInstantTask() {
        val expectedContainer = successContainer("123")
        val expectedValueLoader = mockk<ValueLoader<String>>()
        val expectedMetadata = SourceTypeMetadata(RemoteSourceType)
        val loadTaskSlot = slot<LoadTask<String>>()
        every { loadTaskManager.getLastRealLoader() } returns expectedValueLoader
        every { loadTaskManager.getLastRealMetadata() } returns expectedMetadata

        subject.updateWith(expectedContainer)

        verify(exactly = 1) {
            loadTaskManager.submitNewLoadTask(capture(loadTaskSlot))
        }
        val resultLoadTask = loadTaskSlot.captured as LoadTask.Instant
        assertSame(expectedContainer, resultLoadTask.initialContainer)
        assertSame(expectedValueLoader, resultLoadTask.lastRealLoader)
        assertSame(expectedMetadata, resultLoadTask.lastRealMetadata)
    }

    @Test
    fun newLoad_executesLoadTask() {
        val valueLoader = mockk<ValueLoader<String>>()
        val loadTask = mockk<LoadTask<String>>()
        val flowSubject = mockk<FlowSubject<String>>()
        val expectedFlow = MutableStateFlow("123")
        every { flowSubject.flow() } returns expectedFlow
        every {
            loadTaskFactory.create(
                silently = true,
                valueLoader = refEq(valueLoader),
                metadata = LoadTriggerMetadata(LoadTrigger.NewLoad),
            )
        } returns LoadTaskFactory.LoadTaskRecord(loadTask, flowSubject)

        val resultFlow = subject.newLoad(silently = true, valueLoader = valueLoader)

        verify(exactly = 1) {
            loadTaskManager.submitNewLoadTask(refEq(loadTask))
        }
        assertSame(expectedFlow, resultFlow)
    }

    @Test
    fun newLoad_withCustomMetadata_combinesMetadataWithNewLoadTrigger() {
        val valueLoader = mockk<ValueLoader<String>>()
        val loadTask = mockk<LoadTask<String>>()
        val flowSubject = mockk<FlowSubject<String>>()
        val customMetadata = LoadUuidMetadata("custom-uuid")
        every { flowSubject.flow() } returns MutableStateFlow("123")
        every {
            loadTaskFactory.create(
                silently = false,
                valueLoader = refEq(valueLoader),
                metadata = LoadTriggerMetadata(LoadTrigger.NewLoad) + customMetadata,
            )
        } returns LoadTaskFactory.LoadTaskRecord(loadTask, flowSubject)

        subject.newLoad(valueLoader = valueLoader, metadata = customMetadata)

        verify(exactly = 1) {
            loadTaskManager.submitNewLoadTask(refEq(loadTask))
        }
    }

    @Test
    fun listen_withoutSubscribers_doesNothing() {
        subject.listen()

        verify(exactly = 0) {
            loadTaskManager.listen()
            loadTaskManager.startProcessingLoads(any(), any())
            loadTaskManager.cancelProcessingLoads()
            coroutineScopeFactory.createScope()
        }
    }

    @Test
    fun listen_collectsFlowFromLoadTaskManager() = scope.runFlowTest {
        val flow = MutableStateFlow<Container<String>>(Container.Pending)
        every { loadTaskManager.listen() } returns flow

        val state = subject.listen().startCollecting()

        // assert initial state
        assertEquals(1, state.collectedItems.size)
        assertEquals(Container.Pending, state.collectedItems.last())

        // assert next state 1
        val expectedValue1 = successContainer("1")
        flow.value = expectedValue1
        assertEquals(2, state.collectedItems.size)
        assertEquals(expectedValue1, state.collectedItems.last().raw())

        // assert next state 2
        val expectedValue2 = successContainer("2")
        flow.value = expectedValue2
        assertEquals(3, state.collectedItems.size)
        assertEquals(expectedValue2, state.collectedItems.last().raw())
    }

    @Test
    fun listen_startsLoadsOnlyOnce_afterFirstSubscription() = scope.runFlowTest {
        val scopeSlot = slot<TestScope>()
        every { loadTaskManager.listen() } returns MutableStateFlow<Container<String>>(Container.Pending)

        subject.listen().startCollecting()
        subject.listen().startCollecting()
        subject.listen().startCollecting()

        verify(exactly = 1) {
            loadTaskManager.startProcessingLoads(capture(scopeSlot), any())
        }
        assertSame(scope.testScheduler, scopeSlot.captured.testScheduler)
    }

    @Test
    fun listen_cancelsLoads_afterLastSubscription() = scope.runFlowTest {
        every { loadTaskManager.listen() } returns MutableStateFlow<Container<String>>(Container.Pending)

        val state1 = subject.listen().startCollecting()
        val state2 = subject.listen().startCollecting()

        // first cancellation does nothing
        state2.cancel()
        advanceTimeBy(timeoutMillis + 1)
        verify(exactly = 0) {
            loadTaskManager.cancelProcessingLoads()
        }

        // last cancellation cancels loads
        state1.cancel()
        advanceTimeBy(timeoutMillis + 1)
        verify(exactly = 1) {
            loadTaskManager.cancelProcessingLoads()
        }
    }

    @Test
    fun listen_cancelsLoads_afterTimeout() = scope.runFlowTest {
        every { loadTaskManager.listen() } returns MutableStateFlow<Container<String>>(Container.Pending)

        val state1 = subject.listen().startCollecting()
        val state2 = subject.listen().startCollecting()
        state2.cancel()
        state1.cancel()

        // nothing is cancelled immediately
        advanceTimeBy(1)
        verify(exactly = 0) {
            loadTaskManager.cancelProcessingLoads()
        }

        // loads are cancelled only after timeout
        advanceTimeBy(timeoutMillis)
        verify(exactly = 1) {
            loadTaskManager.cancelProcessingLoads()
        }
    }

    @Test
    fun listen_doesNotCancelLoads_ifNewSubscriberAppears() = scope.runFlowTest {
        every { loadTaskManager.listen() } returns MutableStateFlow<Container<String>>(Container.Pending)

        val state1 = subject.listen().startCollecting()
        state1.cancel()

        advanceTimeBy(timeoutMillis - 1)
        verify(exactly = 0) {
            loadTaskManager.cancelProcessingLoads()
        }
        // start a new collecting before timeout expires:
        subject.listen().startCollecting()
        advanceTimeBy(timeoutMillis * 2)
        // as a result - nothing is cancelled:
        verify(exactly = 0) {
            loadTaskManager.cancelProcessingLoads()
        }
    }

    @Test
    fun listen_startsLoads_evenAfterCancelling() = scope.runFlowTest {
        every { loadTaskManager.listen() } returns MutableStateFlow<Container<String>>(Container.Pending)

        // verify loads started
        val state1 = subject.listen().startCollecting()
        verify(exactly = 1) {
            loadTaskManager.startProcessingLoads(any(), any())
        }
        verify(exactly = 0) {
            loadTaskManager.cancelProcessingLoads()
        }
        // verify loads cancelled
        state1.cancel()
        advanceTimeBy(timeoutMillis + 1)
        verify(exactly = 1) {
            loadTaskManager.cancelProcessingLoads()
        }
        // verify loads started again
        subject.listen().startCollecting()
        verifySequence {
            // verify full sequence of calls
            loadTaskManager.startProcessingLoads(any(), any()) // first collecting
            loadTaskManager.listen()
            loadTaskManager.cancelProcessingLoads() // first collecting cancelled
            loadTaskManager.startProcessingLoads(any(), any()) // second collecting started
            loadTaskManager.listen()
        }
    }

    @Test
    fun activeListenersCount_returnsNumberOfCollectors() = scope.runFlowTest {
        every { loadTaskManager.listen() } returns MutableStateFlow<Container<String>>(Container.Pending)

        // initial state
        assertEquals(0, subject.activeCollectorsCount)
        assertFalse(subject.hasActiveCollectors)

        // first listener
        val state1 = subject.listen().startCollecting()
        assertEquals(1, subject.activeCollectorsCount)
        assertTrue(subject.hasActiveCollectors)

        // second listener
        val state2 = subject.listen().startCollecting()
        assertEquals(2, subject.activeCollectorsCount)
        assertTrue(subject.hasActiveCollectors)

        // first listener cancelled
        state1.cancel()
        assertEquals(1, subject.activeCollectorsCount)
        assertTrue(subject.hasActiveCollectors)

        // second listener cancelled
        state2.cancel()
        assertEquals(0, subject.activeCollectorsCount)
        assertFalse(subject.hasActiveCollectors)
    }


}