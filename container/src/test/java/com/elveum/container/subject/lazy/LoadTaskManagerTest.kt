@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.LoadTrigger
import com.elveum.container.subject.ValueLoader
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoadTaskManagerTest {

    private lateinit var loadTaskManager: LoadTaskManager<String>

    @Before
    fun setUp() {
        loadTaskManager = LoadTaskManager()
    }

    @Test
    fun isValueLoading_holdsFalseByDefault() {
        assertFalse(loadTaskManager.isValueLoading().value)
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
            loadTask.execute()
        }
    }

    @Test
    fun submitLoadTask_startsLoadingImmediately_ifListenerAlreadySubscribed() = runTest {
        val loadTask = spyk(MockLoadTask(this))

        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTaskManager.submitNewLoadTask(loadTask)
        runCurrent()

        verify(exactly = 1) {
            loadTask.execute()
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
            loadTask2.execute()
        }
    }

    @Test
    fun startProcessingLoads_updatesIsValueLoadingFlow() = runTest {
        val loadTask = MockLoadTask(this)
        loadTaskManager.startProcessingLoads(backgroundScope)

        loadTaskManager.submitNewLoadTask(loadTask)
        runCurrent()

        assertTrue(loadTaskManager.isValueLoading().value)
        loadTask.controller.start()
        loadTask.controller.complete()
        assertFalse(loadTaskManager.isValueLoading().value)
    }

    @Test
    fun startProcessingLoads_updatesOutputFlow() = runTest {
        val loadTask = MockLoadTask(this)
        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTaskManager.submitNewLoadTask(loadTask)
        runCurrent()

        loadTask.controller.start()

        loadTask.controller.emit(Container.Success("1"))
        assertEquals(Container.Success("1"), loadTaskManager.listen().value)

        loadTask.controller.emit(Container.Success("2"))
        assertEquals(Container.Success("2"), loadTaskManager.listen().value)
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

        loadTask1.controller.emit(Container.Success("1"))
        assertEquals(Container.Pending, loadTaskManager.listen().value)

        loadTask2.controller.emit(Container.Success("2"))
        assertEquals(Container.Success("2"), loadTaskManager.listen().value)
    }

    @Test
    fun cancelProcessingLoads_stopsEmittingItems() = runTest {
        val loadTask = MockLoadTask(this)
        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTaskManager.submitNewLoadTask(loadTask)

        loadTaskManager.cancelProcessingLoads()
        advanceTimeBy(1)

        loadTask.controller.emit(Container.Success("1"))
        assertEquals(Container.Pending, loadTaskManager.listen().value)
    }

    @Test
    fun cancelProcessingLoads_setsPendingStatus() = runTest {
        val loadTask = MockLoadTask(this)
        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTaskManager.submitNewLoadTask(loadTask)

        loadTask.controller.start()
        loadTask.controller.emit(Container.Success("1"))
        assertEquals(Container.Success("1"), loadTaskManager.listen().value)
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
    fun cancelProcessingLoads_setsCacheExpiredTrigger() = runTest {
        val loadTask = spyk(MockLoadTask(this))
        loadTaskManager.startProcessingLoads(backgroundScope)
        loadTaskManager.submitNewLoadTask(loadTask)

        loadTask.controller.start()
        loadTask.controller.emit(Container.Success("1"))
        assertEquals(Container.Success("1"), loadTaskManager.listen().value)
        loadTaskManager.cancelProcessingLoads()
        advanceTimeBy(1)

        verify(exactly = 1) {
            loadTask.setLoadTrigger(LoadTrigger.CacheExpired)
        }
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

        loadTask1.controller.emit(Container.Success("1"))
        assertEquals(Container.Pending, loadTaskManager.listen().value)

        loadTask2.controller.emit(Container.Success("2"))
        assertEquals(Container.Success("2"), loadTaskManager.listen().value)
    }

}