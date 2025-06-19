@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.LoadTrigger
import com.elveum.container.subject.ValueLoader
import com.elveum.container.successContainer
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class LoadTaskManagerTest {

    private lateinit var loadTaskManager: LoadTaskManager<String>

    @Before
    fun setUp() {
        loadTaskManager = LoadTaskManager()
    }

    @Test
    fun listen_holdsPendingStatusByDefault() {
        assertEquals(Container.Pending, loadTaskManager.listen().value)
    }

    @Test
    fun getLastRealLoader_returnsValueLoader() = runTest {
        val loadTask = spyk(MockLoadTask(this))
        val expectedValueLoader = mockk<ValueLoader<String>>()
        every { loadTask.lastRealLoader } returns expectedValueLoader
        loadTaskManager.submitNewLoadTask(loadTask)

        val valueLoader = loadTaskManager.getLastRealLoader()
        assertSame(expectedValueLoader, valueLoader)
    }

    @Test
    fun submitLoadTask_withoutListeners_doesNotStartLoading() = runTest {
        val loadTask = spyk(MockLoadTask(this))

        loadTaskManager.submitNewLoadTask(loadTask)
        runCurrent()

        verify(exactly = 0) {
            loadTask.execute()
        }
    }

    @Test
    fun submitLoadTask_startsLoadingWhenFirstListenerSubscribed() = runTest {
        val loadTask = spyk(MockLoadTask(this))

        loadTaskManager.submitNewLoadTask(loadTask)
        loadTaskManager.startProcessingLoads(backgroundScope)
        runCurrent()

        verify(exactly = 1) {
            loadTask.execute(any())
        }
    }

    @Test
    fun submitLoadTask_startsLoadingImmediately_ifListenerAlreadySubscribed() = runTest {
        val loadTask = spyk(MockLoadTask(this))

        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTaskManager.submitNewLoadTask(loadTask)
        runCurrent()

        verify(exactly = 1) {
            loadTask.execute(any())
        }
    }

    @Test
    fun submitLoadTask_cancelsPreviousTaskAndExecutesNewTask() = runTest {
        val loadTask1 = spyk(MockLoadTask(this))
        val loadTask2 = spyk(MockLoadTask(this))
        loadTaskManager.startProcessingLoads(backgroundScope)

        loadTaskManager.submitNewLoadTask(loadTask1)
        loadTaskManager.submitNewLoadTask(loadTask2)
        advanceTimeBy(1)

        verify(exactly = 1) {
            loadTask1.cancel()
            loadTask2.execute(any())
        }
    }

    @Test
    fun submitNewLoadTask_updatesValueImmediately() = runTest {
        val loadTask = MockLoadTask(this, initialContainer = successContainer("123"))
        loadTaskManager.startProcessingLoads(backgroundScope)

        loadTaskManager.submitNewLoadTask(loadTask)
        advanceTimeBy(1)

        assertEquals(
            successContainer("123"),
            loadTaskManager.listen().value
        )
    }

    @Test
    fun submitNewLoadTask_withoutListeners_doesNotUpdateValueImmediately() = runTest {
        val loadTask = MockLoadTask(this, initialContainer = successContainer("123"))

        loadTaskManager.submitNewLoadTask(loadTask)
        advanceTimeBy(1)

        assertEquals(
            Container.Pending,
            loadTaskManager.listen().value
        )
    }

    @Test
    fun startProcessingLoads_updatesOutputFlow() = runTest {
        val loadTask = MockLoadTask(this)
        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTaskManager.submitNewLoadTask(loadTask)
        runCurrent()

        loadTask.controller.start()

        loadTask.controller.emit(successContainer("1"))
        assertEquals(successContainer("1"), loadTaskManager.listen().value)

        loadTask.controller.emit(successContainer("2"))
        assertEquals(successContainer("2"), loadTaskManager.listen().value)
    }

    @Test
    fun startProcessingLoads_doesNotEmitValues_forCancelledTask() = runTest {
        val loadTask1 = MockLoadTask(this)
        val loadTask2 = MockLoadTask(this)
        loadTaskManager.startProcessingLoads(backgroundScope)

        loadTaskManager.submitNewLoadTask(loadTask1)
        loadTask1.controller.start()
        loadTaskManager.submitNewLoadTask(loadTask2)
        loadTask2.controller.start()
        advanceTimeBy(1)

        loadTask1.controller.emit(successContainer("1"))
        assertEquals(Container.Pending, loadTaskManager.listen().value)

        loadTask2.controller.emit(successContainer("2"))
        assertEquals(successContainer("2"), loadTaskManager.listen().value)
    }

    @Test
    fun cancelProcessingLoads_stopsEmittingItems() = runTest {
        val loadTask = MockLoadTask(this)
        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTaskManager.submitNewLoadTask(loadTask)

        loadTaskManager.cancelProcessingLoads()
        advanceTimeBy(1)

        loadTask.controller.emit(successContainer("1"))
        assertEquals(Container.Pending, loadTaskManager.listen().value)
    }

    @Test
    fun cancelProcessingLoads_setsPendingStatus() = runTest {
        val loadTask = MockLoadTask(this)
        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTaskManager.submitNewLoadTask(loadTask)

        loadTask.controller.start()
        loadTask.controller.emit(successContainer("1"))
        assertEquals(successContainer("1"), loadTaskManager.listen().value)
        loadTaskManager.cancelProcessingLoads()
        advanceTimeBy(1)

        assertEquals(Container.Pending, loadTaskManager.listen().value)
    }


    @Test
    fun cancelProcessingLoads_cancelsTask() = runTest {
        val loadTask = spyk(MockLoadTask(this))
        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTaskManager.submitNewLoadTask(loadTask)

        loadTaskManager.cancelProcessingLoads()
        advanceTimeBy(1)

        verify(exactly = 1) {
            loadTask.cancel()
        }
    }

    @Test
    fun cancelProcessingLoads_withEmptyRealLoader_setsCacheExpiredTrigger() = runTest {
        val loadTask = spyk(MockLoadTask(this))
        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTaskManager.submitNewLoadTask(loadTask)

        loadTask.controller.start()
        loadTask.controller.emit(successContainer("1"))
        assertEquals(successContainer("1"), loadTaskManager.listen().value)
        loadTaskManager.cancelProcessingLoads()
        advanceTimeBy(1)

        verify(exactly = 1) {
            loadTask.setLoadTrigger(LoadTrigger.CacheExpired)
        }
    }

    @Test
    fun cancelProcessingLoads_withRealLoader_createsNewLoadTask() = runTest {
        val realLoader: ValueLoader<String> = { emit("2") }
        val loadTask = spyk(MockLoadTask(this))
        loadTask.lastRealLoader = realLoader
        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTaskManager.submitNewLoadTask(loadTask)

        loadTask.controller.start()
        loadTask.controller.emit(successContainer("1"))
        assertEquals(successContainer("1"), loadTaskManager.listen().value)
        loadTaskManager.cancelProcessingLoads()
        advanceTimeBy(1)
        verify(exactly = 0) {
            loadTask.setLoadTrigger(LoadTrigger.CacheExpired)
        }
        loadTaskManager.startProcessingLoads(backgroundScope)
        advanceTimeBy(1)

        assertEquals(successContainer("2"), loadTaskManager.listen().value)
    }

    @Test
    fun startProcessingLoads_afterCancellation_executesLastLoadTask() = runTest {
        val loadTask1 = MockLoadTask(this)
        val loadTask2 = MockLoadTask(this)
        loadTaskManager.startProcessingLoads(backgroundScope)

        loadTaskManager.submitNewLoadTask(loadTask1)
        loadTask1.controller.start()
        loadTaskManager.submitNewLoadTask(loadTask2)
        loadTask2.controller.start()
        loadTaskManager.cancelProcessingLoads()
        loadTask1.controller.reset()
        loadTask2.controller.reset()
        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTask1.controller.start()
        loadTask2.controller.start()
        advanceTimeBy(1)

        loadTask1.controller.emit(successContainer("1"))
        assertEquals(Container.Pending, loadTaskManager.listen().value)

        loadTask2.controller.emit(successContainer("2"))
        assertEquals(successContainer("2"), loadTaskManager.listen().value)
    }

}
