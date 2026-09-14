package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.LoadConfig
import com.elveum.container.errorContainer
import com.elveum.container.exceptionOrNull
import com.elveum.container.factory.DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS
import com.elveum.container.pendingContainer
import com.elveum.container.subject.transformation.LoaderDecorator
import com.elveum.container.successContainer
import com.elveum.container.utils.raw
import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

internal class LazyFlowSubjectDecoratorIntegrationTest : AbstractLazyFlowSubjectIntegrationTest() {

    @Test
    fun loaderDecorator_wrapsOriginLoader() = runFlowTest {
        val sessionFlow = MutableStateFlow("token")
        val loaderDecorator = LoaderDecorator { loader ->
            val token = dependsOnFlow("getToken") { sessionFlow }
            // throw Exception when token does not exist
            if (token.isBlank()) throw Exception("No valid session")
            loader()
        }
        val subject = createLazyFlowSubject(
            loaderDecorator = loaderDecorator,
        ) {
            emit("item", isLastValue = true)
        }

        val state = subject.listen().startCollecting()
        runCurrent()
        assertEquals(
            listOf(pendingContainer(), successContainer("item")),
            state.collectedItems.raw(),
        )

        sessionFlow.value = ""
        advanceTimeBy(DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS + 1)
        assertTrue(state.lastItem.exceptionOrNull()?.message == "No valid session")
    }

    @Test
    fun completeWithFailure_emitsErrorContainerWithTheGivenException() = runFlowTest {
        val expectedException = IllegalStateException("terminated")
        val loaderDecorator = LoaderDecorator {
            completeWithFailure(expectedException)
        }
        val subject = createLazyFlowSubject(
            loaderDecorator = loaderDecorator,
        ) {
            emit("item", isLastValue = true)
        }

        val state = subject.listen().startCollecting()
        runCurrent()

        assertEquals(
            listOf(pendingContainer(), errorContainer(expectedException)),
            state.collectedItems.raw(),
        )
        assertSame(expectedException, state.lastItem.exceptionOrNull())
    }

    @Test
    fun completeWithFailure_doesNotExecuteOriginLoader() = runFlowTest {
        var loaderExecuted = false
        val loaderDecorator = LoaderDecorator {
            completeWithFailure(IllegalStateException("terminated"))
        }
        val subject = createLazyFlowSubject(
            loaderDecorator = loaderDecorator,
        ) {
            loaderExecuted = true
            emit("item", isLastValue = true)
        }

        subject.listen().startCollecting()
        runCurrent()

        assertFalse(loaderExecuted)
    }

    @Test
    fun completeWithCacheCleanUp_beforeAnyValueEmitted_staysPending() = runFlowTest {
        val loaderDecorator = LoaderDecorator {
            completeWithCacheCleanUp()
        }
        val subject = createLazyFlowSubject(
            loaderDecorator = loaderDecorator,
        ) {
            emit("item", isLastValue = true)
        }

        val state = subject.listen().startCollecting()
        runCurrent()

        // the terminal Pending is identical to the initial one, so it is de-duplicated
        assertEquals(listOf(pendingContainer()), state.collectedItems.raw())
    }

    @Test
    fun completeWithCacheCleanUp_afterOriginLoaderEmittedValue_dropsTheLoadedValue() = runFlowTest {
        val loaderDecorator = LoaderDecorator { loader ->
            loader()
            delay(1.milliseconds)
            completeWithCacheCleanUp()
        }
        val subject = createLazyFlowSubject(
            loaderDecorator = loaderDecorator,
        ) {
            emit("item", isLastValue = true)
        }

        val state = subject.listen().startCollecting()
        advanceTimeBy(2)

        assertEquals(
            listOf(pendingContainer(), successContainer("item"), pendingContainer()),
            state.collectedItems.raw(),
        )
    }

    @Test
    fun completeWithCacheCleanUp_onReload_clearsPreviouslyCachedValue() = runFlowTest {
        val triggerFlow = MutableStateFlow(0)
        val loaderDecorator = LoaderDecorator { loader ->
            val trigger = dependsOnFlow("trigger") { triggerFlow }
            if (trigger == 0) loader() else completeWithCacheCleanUp()
        }
        val subject = createLazyFlowSubject(
            loaderDecorator = loaderDecorator,
        ) {
            emit("item", isLastValue = true)
        }

        val state = subject.listen().startCollecting()
        runCurrent()
        assertEquals(successContainer("item"), state.lastItem.raw())

        triggerFlow.value = 1
        advanceTimeBy(DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS + 1)

        assertEquals(pendingContainer(), state.lastItem.raw())
    }

    @Test
    fun completeWithFailure_withSilentErrorsEnabled_stillEmitsErrorContainer() = runFlowTest {
        val expectedException = IllegalStateException("terminated")
        val triggerFlow = MutableStateFlow(0)
        val loaderDecorator = LoaderDecorator { loader ->
            val trigger = dependsOnFlow("trigger") { triggerFlow }
            if (trigger == 0) loader() else completeWithFailure(expectedException)
        }
        val subject = createLazyFlowSubject(
            loaderDecorator = loaderDecorator,
            loadConfig = LoadConfig.SilentLoadingAndError,
        ) {
            emit("item", isLastValue = true)
        }

        val state = subject.listen().startCollecting()
        runCurrent()
        assertEquals(successContainer("item"), state.lastItem.raw())

        triggerFlow.value = 1
        advanceTimeBy(DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS + 1)

        assertTrue(state.lastItem is Container.Error)
        assertSame(expectedException, state.lastItem.exceptionOrNull())
    }

    @Test
    fun thrownException_withSilentErrorsEnabled_keepsCachedValue() = runFlowTest {
        // contrast with completeWithFailure: a regular failure still honours the silent policy
        val triggerFlow = MutableStateFlow(0)
        val loaderDecorator = LoaderDecorator { loader ->
            val trigger = dependsOnFlow("trigger") { triggerFlow }
            if (trigger == 0) loader() else throw IllegalStateException("failed")
        }
        val subject = createLazyFlowSubject(
            loaderDecorator = loaderDecorator,
            loadConfig = LoadConfig.SilentLoadingAndError,
        ) {
            emit("item", isLastValue = true)
        }

        val state = subject.listen().startCollecting()
        runCurrent()

        triggerFlow.value = 1
        advanceTimeBy(DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS + 1)

        assertEquals(successContainer("item"), state.lastItem.raw())
    }

    @Test
    fun loaderDecorator_whenOriginLoaderEmitsNothing_failsWithIllegalStateException() = runFlowTest {
        val subject = createLazyFlowSubject(
            loaderDecorator = LoaderDecorator,
        ) {
            // emits nothing
        }

        val state = subject.listen().startCollecting()
        runCurrent()

        val exception = state.lastItem.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertTrue(exception?.message?.startsWith("Value Loader should emit at least one item") == true)
    }

}
