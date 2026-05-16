@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
import com.elveum.container.Emitter
import com.elveum.container.map
import com.elveum.container.subject.paging.PageEmitter
import com.elveum.container.successContainer
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class PageLoadTaskLauncherTest {

    @RelaxedMockK
    private lateinit var state: PageLoaderState<Int, String>

    @RelaxedMockK
    private lateinit var emitter: Emitter<List<String>>

    @RelaxedMockK
    private lateinit var pageEmitterFactory: PageLoadTaskLauncher.PageEmitterFactory<Int, String>

    private lateinit var block: suspend PageEmitter<Int, String>.(Int) -> Unit

    private lateinit var testScope: TestScope

    private lateinit var launcher: PageLoadTaskLauncher<Int, String>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        block = mockk(relaxed = true)
        testScope = TestScope()
        launcher = PageLoadTaskLauncher(state, testScope.backgroundScope, emitter, block, pageEmitterFactory)
    }

    @Test
    fun launch_whenStateHasLaunchedKey_doesNotProcessKey() = testScope.runTest {
        every { state.hasLaunchedKey(1) } returns true

        launcher.launch(1)
        runCurrent()

        coVerify(exactly = 0) { state.processKey(any(), any(), any()) }
    }

    @Test
    fun launch_whenStateHasLaunchedKey_doesNotCreatePageEmitter() = testScope.runTest {
        every { state.hasLaunchedKey(1) } returns true

        launcher.launch(1)
        runCurrent()

        verify(exactly = 0) { pageEmitterFactory.create(any(), any()) }
    }

    @Test
    fun launch_whenStateHasLaunchedKey_startsProcessingKey() = testScope.runTest {
        every { state.hasLaunchedKey(1) } returns false

        launcher.launch(1)
        runCurrent()

        coVerify(exactly = 1) { state.processKey(eq(1), any(), any()) }
    }

    @Test
    fun launch_whenKeyIsBeingProcessed_executesBlockOnEmitterCreatedByFactory() = testScope.runTest {
        val mockPageEmitter = mockk<PageEmitterImpl<Int, String>>(relaxed = true)
        every { mockPageEmitter.emitPageCalled } returns true
        every { state.hasLaunchedKey(1) } returns false
        every { pageEmitterFactory.create(eq(1), eq(launcher)) } returns mockPageEmitter
        coEvery { state.processKey(any(), any(), any()) } coAnswers {
            thirdArg<suspend () -> Unit>().invoke()
        }

        launcher.launch(1)
        runCurrent()

        coVerify { block.invoke(mockPageEmitter, 1) }
    }

    @Test
    fun launch_whenKeyIsBeingProcessedAndPageIsNotEmitted_throwsException() = testScope.runTest {
        val mockPageEmitter = mockk<PageEmitterImpl<Int, String>>(relaxed = true)
        every { mockPageEmitter.emitPageCalled } returns false
        every { state.hasLaunchedKey(1) } returns false
        every { pageEmitterFactory.create(any(), any()) } returns mockPageEmitter
        var caughtException: Throwable? = null
        coEvery { state.processKey(any(), any(), any()) } coAnswers {
            caughtException = runCatching {
                thirdArg<suspend () -> Unit>().invoke()
            }.exceptionOrNull()
        }

        launcher.launch(1)
        runCurrent()

        assertNotNull(caughtException)
        assertEquals("emitPage() must be called at least once.", caughtException!!.message)
    }

    @Test
    fun launch_whenKeyIsBeingProcessedAndPageIsEmitted_doesNotThrowException() = testScope.runTest {
        val mockPageEmitter = mockk<PageEmitterImpl<Int, String>>(relaxed = true)
        every { mockPageEmitter.emitPageCalled } returns true
        every { state.hasLaunchedKey(1) } returns false
        every { pageEmitterFactory.create(any(), any()) } returns mockPageEmitter
        coEvery { state.processKey(any(), any(), any()) } coAnswers {
            thirdArg<suspend () -> Unit>().invoke()
        }

        launcher.launch(1)
        runCurrent()

        // no exception thrown - test passes
    }

    @Test
    fun findNextKeyForIndex_delegatesCallToState() = testScope.runTest {
        every { state.findNextKeyForIndex(5) } returns 2

        val result = launcher.findNextKeyForIndex(5)

        assertEquals(2, result)
        verify { state.findNextKeyForIndex(5) }
    }

    @Test
    fun intercept_delegatesToState() = testScope.runTest {
        val input = successContainer(listOf("a", "b"))
        val expected = input.map { it.reversed() }
        every { state.intercept(input) } returns expected

        val result = launcher.intercept(input)

        assertEquals(expected, result)
        verify(exactly = 1) { state.intercept(input) }
    }

}
