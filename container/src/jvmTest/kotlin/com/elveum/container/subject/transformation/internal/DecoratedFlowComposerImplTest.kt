package com.elveum.container.subject.transformation.internal

import com.elveum.container.FlowComposer
import com.elveum.container.successContainer
import com.elveum.container.utils.catch
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class DecoratedFlowComposerImplTest {

    @RelaxedMockK
    private lateinit var originComposer: FlowComposer

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun completeWithFailure_throwsTerminatedDecorationException() {
        val composer = DecoratedFlowComposerImpl(originComposer)
        val exception = IllegalStateException("failed")

        val terminatedException = catch<TerminatedDecorationException> {
            composer.completeWithFailure(exception)
        }

        assertEquals(
            TerminatedDecorationResult.Failure(exception),
            terminatedException.result,
        )
    }

    @Test
    fun completeWithFailure_keepsTheOriginExceptionInstance() {
        val composer = DecoratedFlowComposerImpl(originComposer)
        val exception = IllegalStateException("failed")

        val terminatedException = catch<TerminatedDecorationException> {
            composer.completeWithFailure(exception)
        }

        val result = terminatedException.result as TerminatedDecorationResult.Failure
        assertSame(exception, result.exception)
    }

    @Test
    fun completeWithCacheCleanUp_throwsTerminatedDecorationException() {
        val composer = DecoratedFlowComposerImpl(originComposer)

        val terminatedException = catch<TerminatedDecorationException> {
            composer.completeWithCacheCleanUp()
        }

        assertEquals(
            TerminatedDecorationResult.ClearCache,
            terminatedException.result,
        )
    }

    @Test
    fun dependsOnFlow_delegatesToOriginComposer() = runTest {
        coEvery { originComposer.dependsOnFlow<String>(any(), flow = any()) } returns "value"
        val composer = DecoratedFlowComposerImpl(originComposer)

        val result = composer.dependsOnFlow("key") { flowOf("value") }

        assertEquals("value", result)
        coVerify(exactly = 1) { originComposer.dependsOnFlow<String>("key", flow = any()) }
    }

    @Test
    fun dependsOnContainerFlow_delegatesToOriginComposer() = runTest {
        coEvery { originComposer.dependsOnContainerFlow<String>(any(), flow = any()) } returns "value"
        val composer = DecoratedFlowComposerImpl(originComposer)

        val result = composer.dependsOnContainerFlow("key") { flowOf(successContainer("value")) }

        assertEquals("value", result)
        coVerify(exactly = 1) { originComposer.dependsOnContainerFlow<String>("key", flow = any()) }
    }

}
