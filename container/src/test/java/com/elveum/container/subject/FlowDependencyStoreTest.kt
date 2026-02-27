package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.LoadConfig
import com.elveum.container.errorContainer
import com.elveum.container.factory.DefaultReloadDependenciesPeriodMillis
import com.elveum.container.subject.lazy.FlowDependencyStore
import com.elveum.container.subject.lazy.LoadTaskManager
import com.elveum.container.successContainer
import com.uandcode.flowtest.FlowTestScope
import com.uandcode.flowtest.JobStatus
import com.uandcode.flowtest.runFlowTest
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FlowDependencyStoreTest {

    @MockK
    private lateinit var loadTaskManager: LoadTaskManager<String>

    @RelaxedMockK
    private lateinit var recomposeFunction: FlowDependencyStore.RecomposeFunction

    private lateinit var flowDependencyStore: FlowDependencyStoreImpl<String>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        flowDependencyStore = FlowDependencyStoreImpl(loadTaskManager)
    }

    @Test
    fun dependsOn_awaitsForItems() = runFlowDependencyTest {
        val flow = MutableSharedFlow<Container<String>>()

        val state = executeInBackground {
            flowDependencyStore.dependsOn("key") { flow }
        }
        runCurrent()

        assertEquals(JobStatus.Executing, state.status)
    }

    @Test
    fun dependsOn_whenFirstItemEmitted_returnsTheItem() = runFlowDependencyTest {
        val flow = MutableSharedFlow<Container<String>>()
        val state = executeInBackground {
            flowDependencyStore.dependsOn("key") { flow }
        }
        runCurrent()

        flow.emit(successContainer("item"))

        assertEquals(JobStatus.Completed("item"), state.status)
    }

    @Test
    fun dependsOn_whenFirstErrorEmitted_throwsException() = runFlowDependencyTest {
        val flow = MutableSharedFlow<Container<String>>()
        val exception = IllegalArgumentException()
        val state = executeInBackground {
            flowDependencyStore.dependsOn("key") { flow }
        }
        runCurrent()

        flow.emit(errorContainer(exception))

        assertTrue((state.status as JobStatus.Failed).exception is IllegalArgumentException)
    }

    @Test
    fun dependsOn_whenFirstItemEmitted_doesNotScheduleReload() = runFlowDependencyTest {
        val flow = MutableSharedFlow<Container<String>>()
        executeInBackground {
            flowDependencyStore.dependsOn("key") { flow }
        }
        runCurrent()

        flow.emit(successContainer("item"))

        verify(exactly = 0) {
            recomposeFunction.execute(any())
        }

        advanceTimeBy(DefaultReloadDependenciesPeriodMillis + 1)

        verify(exactly = 0) {
            recomposeFunction.execute(any())
        }
    }

    @Test
    fun dependsOn_whenNextItemEmitted_schedulesReload() = runFlowDependencyTest {
        val flow = MutableSharedFlow<Container<String>>()
        executeInBackground {
            flowDependencyStore.dependsOn("key") { flow }
        }
        runCurrent()

        flow.emit(successContainer("item1"))
        flow.emit(successContainer("item2"))

        verify(exactly = 0) {
            recomposeFunction.execute(any())
        }

        advanceTimeBy(DefaultReloadDependenciesPeriodMillis + 1)

        verify(exactly = 1) {
            recomposeFunction.execute(false)
        }
    }

    @Test
    fun dependsOn_withSameKey_reusesPreviousFlowOnNextLaunch() = runFlowDependencyTest {
        val flow = MutableSharedFlow<Container<String>>()

        // first launch
        executeInBackground {
            flowDependencyStore.dependsOn("key") { flow }
        }
        runCurrent()

        flow.emit(successContainer("item1"))
        flow.emit(successContainer("item2"))

        // second launch (simulate reloading)
        val state = executeInBackground {
            flowDependencyStore.dependsOn<String>("key") {
                throw AssertionError("The second flow must not be called")
            }
        }
        runCurrent()

        assertEquals(JobStatus.Completed("item2"), state.status)
    }

    @Test
    fun dependsOn_withNonUsedKey_closesSubscription() = runFlowTest {
        val flow1 = MutableSharedFlow<Container<String>>()
        val flow2 = MutableSharedFlow<Container<String>>()
        var isBFlowCancelled = false
        flowDependencyStore.initialize(scope.backgroundScope, recomposeFunction)
        flowDependencyStore.begin(reloadDependencies = false, loadConfig = LoadConfig.Normal)
        executeInBackground {
            val a = flowDependencyStore.dependsOn("keyA") { flow1 }
            val b = flowDependencyStore.dependsOn("keyB") {
                flow2.onCompletion { e ->
                    isBFlowCancelled = e is CancellationException
                }
            }
            "$a:$b"
        }
        runCurrent()
        flow2.emit(successContainer("b1"))
        flow1.emit(successContainer("a1"))
        flowDependencyStore.end()

        flow1.emit(successContainer("a2"))
        flow2.emit(successContainer("b2"))

        flowDependencyStore.begin(reloadDependencies = false, loadConfig = LoadConfig.Normal)
        val state = executeInBackground {
            flowDependencyStore.dependsOn("keyA") { flow1 }
        }
        runCurrent()
        flowDependencyStore.end()
        runCurrent()

        assertTrue(isBFlowCancelled)
        assertEquals(JobStatus.Completed("a2"), state.status)
    }

    private fun runFlowDependencyTest(
        block: suspend FlowTestScope.() -> Unit
    ) = runFlowTest {
        flowDependencyStore.initialize(scope.backgroundScope, recomposeFunction)
        flowDependencyStore.begin(reloadDependencies = false, loadConfig = LoadConfig.Normal)
        this.block()
    }


}