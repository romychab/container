package com.elveum.container.subject.lazy

import com.elveum.container.BackgroundLoadState
import com.elveum.container.Container
import com.elveum.container.EmptyMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.LocalSourceType
import com.elveum.container.SourceTypeMetadata
import com.elveum.container.backgroundLoadState
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.sourceType
import com.elveum.container.subject.FlowSubject
import com.elveum.container.successContainer
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StatefulEmitterImplTest {

    @RelaxedMockK
    private lateinit var flowCollector: FlowCollector<Container<String>>

    @RelaxedMockK
    private lateinit var flowEmitter: FlowEmitter<String>

    @RelaxedMockK
    private lateinit var flowSubject: FlowSubject<String>

    @RelaxedMockK
    private lateinit var flowDependencyStore: FlowDependencyStore

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { flowEmitter.metadata } returns EmptyMetadata
    }

    @Test
    fun hasEmittedValues_delegatesToFlowEmitter() {
        every { flowEmitter.hasEmittedValues } returns true
        val emitter = makeEmitter()

        assertTrue(emitter.hasEmittedValues)
    }

    @Test
    fun hasEmittedValues_returnsFalseWhenFlowEmitterHasNotEmitted() {
        every { flowEmitter.hasEmittedValues } returns false
        val emitter = makeEmitter()

        assertFalse(emitter.hasEmittedValues)
    }

    @Test
    fun emitPendingState_normalConfig_emitsPendingContainer() = runTest {
        val emitter = makeEmitter(loadConfig = LoadConfig.Normal)

        emitter.emitPendingState()

        coVerify(exactly = 1) { flowCollector.emit(Container.Pending) }
    }

    @Test
    fun emitPendingState_silentLoadingConfig_emitsCurrentContainerWithBackgroundLoading() = runTest {
        val currentContainer = successContainer("old-value")
        val emitter = makeEmitter(
            loadConfig = LoadConfig.SilentLoading,
            currentContainer = { currentContainer },
        )

        emitter.emitPendingState()

        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container is Container.Success &&
                    container.value == "old-value" &&
                    container.metadata.backgroundLoadState == BackgroundLoadState.Loading
            })
        }
    }

    @Test
    fun emitPendingState_silentLoadingAndErrorConfig_emitsCurrentContainerWithBackgroundLoading() = runTest {
        val currentContainer = successContainer("old-value")
        val emitter = makeEmitter(
            loadConfig = LoadConfig.SilentLoadingAndError,
            currentContainer = { currentContainer },
        )

        emitter.emitPendingState()

        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container is Container.Success &&
                    container.value == "old-value" &&
                    container.metadata.backgroundLoadState == BackgroundLoadState.Loading
            })
        }
    }

    @Test
    fun emitPendingState_silentLoadingConfig_withPendingCurrentContainer_emitsPendingWithBackgroundLoading() = runTest {
        val emitter = makeEmitter(
            loadConfig = LoadConfig.SilentLoading,
            currentContainer = { pendingContainer() },
        )

        emitter.emitPendingState()

        // Pending.update returns Pending unchanged (update only applies to Completed)
        coVerify(exactly = 1) { flowCollector.emit(Container.Pending) }
    }

    @Test
    fun emitCompletedState_callsFlowSubjectOnComplete() = runTest {
        val emitter = makeEmitter()

        emitter.emitCompletedState()

        verify(exactly = 1) { flowSubject.onComplete() }
    }

    @Test
    fun emitCompletedState_callsEmitLastItem() = runTest {
        val emitter = makeEmitter()

        emitter.emitCompletedState()

        coVerify(exactly = 1) { flowEmitter.emitLastItem() }
    }

    @Test
    fun emitCompletedState_calledTwice_onlyExecutesOnce() = runTest {
        val emitter = makeEmitter()

        emitter.emitCompletedState()
        emitter.emitCompletedState()

        verify(exactly = 1) { flowSubject.onComplete() }
        coVerify(exactly = 1) { flowEmitter.emitLastItem() }
    }

    @Test
    fun emitCompletedState_withNullFlowSubject_doesNotCrash() = runTest {
        val emitter = makeEmitter(flowSubject = null)

        emitter.emitCompletedState()

        coVerify(exactly = 1) { flowEmitter.emitLastItem() }
    }

    @Test
    fun emitFailureState_normalConfig_emitsErrorContainer() = runTest {
        val emitter = makeEmitter(loadConfig = LoadConfig.Normal)
        val exception = RuntimeException("test error")

        emitter.emitFailureState(exception)

        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container is Container.Error && container.exception === exception
            })
        }
    }

    @Test
    fun emitFailureState_callsFlowSubjectOnError() = runTest {
        val emitter = makeEmitter()
        val exception = RuntimeException("test error")

        emitter.emitFailureState(exception)

        verify(exactly = 1) { flowSubject.onError(exception) }
    }

    @Test
    fun emitFailureState_calledTwice_onlyExecutesOnce() = runTest {
        val emitter = makeEmitter()
        val exception1 = RuntimeException("error 1")
        val exception2 = RuntimeException("error 2")

        emitter.emitFailureState(exception1)
        emitter.emitFailureState(exception2)

        verify(exactly = 1) { flowSubject.onError(any()) }
        verify(exactly = 1) { flowSubject.onError(exception1) }
    }

    @Test
    fun emitFailureState_withCancellationException_rethrowsIt() = runTest {
        val emitter = makeEmitter()
        val exception = CancellationException("cancelled")

        val result = runCatching { emitter.emitFailureState(exception) }

        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test
    fun emitFailureState_withCancellationException_callsFlowSubjectOnErrorBeforeRethrowing() = runTest {
        val emitter = makeEmitter()
        val exception = CancellationException("cancelled")

        runCatching { emitter.emitFailureState(exception) }

        verify(exactly = 1) { flowSubject.onError(exception) }
    }

    @Test
    fun emitFailureState_withNullFlowSubject_doesNotCrash() = runTest {
        val emitter = makeEmitter(flowSubject = null, loadConfig = LoadConfig.Normal)
        val exception = RuntimeException("error")

        emitter.emitFailureState(exception)

        coVerify(exactly = 1) {
            flowCollector.emit(match { it is Container.Error })
        }
    }

    @Test
    fun emitFailureState_silentErrorConfig_withSuccessCurrentContainer_emitsSuccessWithBackgroundError() = runTest {
        val currentContainer = successContainer("old-value")
        val exception = RuntimeException("bg error")
        val emitter = makeEmitter(
            loadConfig = LoadConfig.SilentLoadingAndError,
            currentContainer = { currentContainer },
        )

        emitter.emitFailureState(exception)

        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container is Container.Success &&
                    container.value == "old-value" &&
                    container.metadata.backgroundLoadState.let {
                        it is BackgroundLoadState.Error && it.exception == exception
                    }
            })
        }
    }

    @Test
    fun emitFailureState_silentErrorConfig_withErrorCurrentContainer_emitsErrorContainer() = runTest {
        val currentException = RuntimeException("current")
        val currentContainer = errorContainer(currentException)
        val newException = RuntimeException("new error")
        val emitter = makeEmitter(
            loadConfig = LoadConfig.SilentLoadingAndError,
            currentContainer = { currentContainer },
        )

        emitter.emitFailureState(newException)

        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container is Container.Error && container.exception === newException
            })
        }
    }

    @Test
    fun emitFailureState_silentErrorConfig_withPendingCurrentContainer_emitsErrorContainer() = runTest {
        val exception = RuntimeException("error")
        val emitter = makeEmitter(
            loadConfig = LoadConfig.SilentLoadingAndError,
            currentContainer = { pendingContainer() },
        )

        emitter.emitFailureState(exception)

        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container is Container.Error && container.exception === exception
            })
        }
    }

    @Test
    fun emitFailureState_normalConfig_withSuccessCurrentContainer_emitsErrorNotSuccess() = runTest {
        val currentContainer = successContainer("old-value")
        val exception = RuntimeException("error")
        val emitter = makeEmitter(
            loadConfig = LoadConfig.Normal,
            currentContainer = { currentContainer },
        )

        emitter.emitFailureState(exception)

        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container is Container.Error && container.exception === exception
            })
        }
    }

    @Test
    fun emitFailureState_silentLoadingConfig_withSuccessCurrentContainer_emitsErrorNotSuccess() = runTest {
        // SilentLoading does NOT have silent errors enabled
        val currentContainer = successContainer("old-value")
        val exception = RuntimeException("error")
        val emitter = makeEmitter(
            loadConfig = LoadConfig.SilentLoading,
            currentContainer = { currentContainer },
        )

        emitter.emitFailureState(exception)

        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container is Container.Error && container.exception === exception
            })
        }
    }

    @Test
    fun emitFailureState_afterEmitCompletedState_doesNothing() = runTest {
        val emitter = makeEmitter()

        emitter.emitCompletedState()
        emitter.emitFailureState(RuntimeException("error"))

        verify(exactly = 0) { flowSubject.onError(any()) }
    }

    @Test
    fun emitCompletedState_afterEmitFailureState_doesNothing() = runTest {
        val emitter = makeEmitter()

        emitter.emitFailureState(RuntimeException("error"))
        emitter.emitCompletedState()

        verify(exactly = 0) { flowSubject.onComplete() }
        coVerify(exactly = 0) { flowEmitter.emitLastItem() }
    }

    @Test
    fun emitFailureState_silentErrorConfig_attachesEmitterMetadataToSuccessContainer() = runTest {
        val inputMetadata = SourceTypeMetadata(LocalSourceType)
        every { flowEmitter.metadata } returns inputMetadata
        val currentContainer = successContainer("old-value")
        val exception = RuntimeException("bg error")
        val emitter = makeEmitter(
            loadConfig = LoadConfig.SilentLoadingAndError,
            currentContainer = { currentContainer },
        )

        emitter.emitFailureState(exception)

        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container is Container.Success &&
                    container.value == "old-value" &&
                    container.metadata.backgroundLoadState is BackgroundLoadState.Error &&
                    container.metadata.sourceType == LocalSourceType
            })
        }
    }

    @Test
    fun emitFailureState_normalConfig_attachesEmitterMetadataToErrorContainer() = runTest {
        val inputMetadata = SourceTypeMetadata(LocalSourceType)
        every { flowEmitter.metadata } returns inputMetadata
        val exception = RuntimeException("error")
        val emitter = makeEmitter(loadConfig = LoadConfig.Normal)

        emitter.emitFailureState(exception)

        coVerify(exactly = 1) {
            flowCollector.emit(match { container ->
                container is Container.Error &&
                    container.exception === exception &&
                    container.metadata.sourceType == LocalSourceType
            })
        }
    }

    private fun makeEmitter(
        loadConfig: LoadConfig = LoadConfig.Normal,
        currentContainer: () -> Container<String> = { pendingContainer() },
        flowSubject: FlowSubject<String>? = this.flowSubject,
    ): StatefulEmitterImpl<String> {
        val executeParams = LoadTask.ExecuteParams(
            flowDependencyStore = flowDependencyStore,
            currentContainer = currentContainer,
        )
        return StatefulEmitterImpl(
            emitter = flowEmitter,
            executeParams = executeParams,
            loadConfig = loadConfig,
            flowCollector = flowCollector,
            flowSubject = flowSubject,
        )
    }

}
