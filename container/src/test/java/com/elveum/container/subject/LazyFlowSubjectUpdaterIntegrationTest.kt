package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.Container.Pending
import com.elveum.container.EmptyMetadata
import com.elveum.container.IsReloadDependenciesMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.ReloadFunction
import com.elveum.container.ReloadFunctionMetadata
import com.elveum.container.RemoteSourceType
import com.elveum.container.SourceTypeMetadata
import com.elveum.container.factory.DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS
import com.elveum.container.pendingContainer
import com.elveum.container.sourceType
import com.elveum.container.successContainer
import com.elveum.container.utils.raw
import com.uandcode.flowtest.runFlowTest
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class LazyFlowSubjectUpdaterIntegrationTest : AbstractLazyFlowSubjectIntegrationTest() {

    @Test
    fun dependencyChange_reloadsSilently_keepingCurrentValueVisible() = runFlowTest {
        val dependency = MutableSharedFlow<String>()
        // the subject uses the default (Normal) config; a dependency-triggered reload must
        // still be silent and must not reset the currently displayed value to Pending.
        val subject = createLazyFlowSubject {
            val dep = dependsOnFlow("d") { dependency }
            emit("value-$dep")
        }

        val state = subject.listen().startCollecting()
        runCurrent()
        dependency.emit("1")
        runCurrent()
        assertEquals(successContainer("value-1"), state.lastItem.raw())

        // change the dependency -> triggers a (silent) dependency reload
        dependency.emit("2")
        advanceTimeBy(DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS + 1)

        assertEquals(successContainer("value-2"), state.lastItem.raw())
        // no Pending was emitted between the two values - the reload was silent
        val afterFirstSuccess = state.collectedItems.raw().dropWhile { it != successContainer("value-1") }
        assertFalse(afterFirstSuccess.contains(Pending))
    }

    @Test
    fun updateWith_cancelsLoadingAndEmitsNewValueImmediately() = runFlowTest {
        val loader: ValueLoader<String> = ValueLoader {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(6)
        subject.updateWith(successContainer("222"))
        advanceTimeBy(6)

        assertEquals(
            listOf(pendingContainer(), successContainer("222")),
            collectedItems.raw(),
        )
    }

    @Test
    fun updateWith_emitsNewValue() = runFlowTest {
        val loader: ValueLoader<String> = ValueLoader {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(11)
        subject.updateWith(successContainer("222"))
        runCurrent()

        assertEquals(
            listOf(pendingContainer(), successContainer("111"), successContainer("222")),
            collectedItems.raw(),
        )
    }

    @Test
    fun updateWithMapper_cancelsLoadingAndEmitsNewValueImmediately() = runFlowTest {
        val loader: ValueLoader<String> = ValueLoader {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(6)
        subject.updateWith { successContainer("222") }
        advanceTimeBy(6)

        assertEquals(
            listOf(pendingContainer(), successContainer("222")),
            collectedItems.raw(),
        )
    }

    @Test
    fun updateWithMapper_emitsNewValue() = runFlowTest {
        val loader: ValueLoader<String> = ValueLoader {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(11)
        subject.updateWith { successContainer("222") }
        runCurrent()

        assertEquals(
            listOf(pendingContainer(), successContainer("111"), successContainer("222")),
            collectedItems.raw(),
        )
    }

    @Test
    fun updateWithMapper_withReturningSameValue_doesNotReEmitIt() = runFlowTest {
        val loader: ValueLoader<String> = ValueLoader {
            delay(10)
            emit("111")
        }
        val subject = createLazyFlowSubject(loader)

        val collectedItems = subject.listen().startCollecting().collectedItems
        advanceTimeBy(11)
        subject.updateWith { it }
        runCurrent()

        assertEquals(
            listOf(pendingContainer(), successContainer("111")),
            collectedItems.raw(),
        )
    }

    @Test
    fun updateWith_updatesValueImmediately() = runFlowTest {
        val subject = createLazyFlowSubject()

        subject.listen().startCollecting()
        subject.updateWith(successContainer("123"))

        assertEquals(successContainer("123"), subject.currentValue().raw())
    }

    @Test
    fun updateWith_withoutListeners_doesNotUpdateValueImmediately() = runFlowTest {
        val subject = createLazyFlowSubject()

        subject.updateWith(successContainer("123"))

        assertEquals(Pending, subject.currentValue())
    }

    @Test
    fun updateWith_afterResubscribing_usesPreviousLoader() = runFlowTest {
        val subject = createLazyFlowSubject(SourceTypeMetadata(RemoteSourceType)) {
            emit("real-item")
        }
        val oldState = subject.listen().startCollecting()
        runCurrent()

        subject.updateWith(successContainer("updated-item"))
        oldState.cancel()
        advanceTimeBy(cacheTimeout + 1)
        val newState = subject.listen().startCollecting()
        runCurrent()

        assertEquals(
            listOf(Pending, successContainer("real-item")),
            newState.collectedItems.raw()
        )
        assertEquals(RemoteSourceType, newState.collectedItems.last().metadata.sourceType)
    }

    @Test
    fun loader_withDependencies_observeAllDependencies() = runFlowTest {
        val reloadFunction = mockk<ReloadFunction>(relaxed = true)
        val dependencyA = MutableSharedFlow<Container<String>>()
        val dependencyB = MutableSharedFlow<String>()
        var isDependencyBCancelled = false
        val subject = createLazyFlowSubject {
            val s1 = dependsOnContainerFlow("a") { dependencyA }
            val s2 = dependsOnFlow("b") {
                dependencyB.onCompletion {
                    isDependencyBCancelled = it is CancellationException
                }
            }
            emit("$s1:$s2")
        }

        val state = subject.listen().startCollecting()
        runCurrent()

        // 1. check first value merged from dependencies is received:
        dependencyA.emit(successContainer("a1"))
        runCurrent()
        dependencyB.emit("b1")
        runCurrent()

        assertEquals(
            listOf(pendingContainer(), successContainer("a1:b1")),
            state.collectedItems.raw(),
        )

        // 2. check nothing changed (2 items as before), because recomposition
        //    starts after a delay:
        dependencyA.emit(successContainer("a2", ReloadFunctionMetadata(reloadFunction)))
        assertEquals(2, state.count)

        // 3. advance delay and check new updated item received:
        advanceTimeBy(DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS + 1)
        assertEquals(
            successContainer("a2:b1"),
            state.lastItem.raw()
        )

        // 4. new loader, without dependencyB -> it must be freed
        subject.newAsyncLoad {
            emit(dependsOnContainerFlow("a") { dependencyA })
        }
        runCurrent()
        assertTrue(isDependencyBCancelled)

        // 5. reload (not silently) -> dependencies must be reloaded
        subject.reloadAsync(config = LoadConfig.Normal)
        verify(exactly = 0) { reloadFunction(LoadConfig.SilentLoading, EmptyMetadata) }
        runCurrent()
        verify(exactly = 1) { reloadFunction(LoadConfig.Normal, EmptyMetadata) }

        // 6. reload (silently) -> dependencies must be reloaded
        subject.reloadAsync(config = LoadConfig.SilentLoading)
        runCurrent()
        verify(exactly = 1) { reloadFunction(LoadConfig.SilentLoading, EmptyMetadata) }

        // 7. reload without dependencies
        clearMocks(reloadFunction)
        subject.reloadAsync(metadata = IsReloadDependenciesMetadata(false))
        runCurrent()
        verify(exactly = 0) { reloadFunction(any(), any()) }
    }

}
