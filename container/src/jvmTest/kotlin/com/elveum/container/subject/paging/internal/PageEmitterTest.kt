package com.elveum.container.subject.paging.internal

import com.elveum.container.Emitter
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PageEmitterTest {

    @RelaxedMockK
    private lateinit var state: PageLoaderState<Int, String>

    @RelaxedMockK
    private lateinit var emitter: Emitter<List<String>>

    @RelaxedMockK
    private lateinit var launcher: PageLoadTaskLauncher<Int, String>

    private lateinit var pageEmitter: PageEmitterImpl<Int, String>

    private val key = 1

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        pageEmitter = PageEmitterImpl(key, state, emitter, launcher)
    }

    @Test
    fun emitPage_setsEmitPageCalledToTrue() = runTest {
        pageEmitter.emitPage(listOf("a", "b"))

        assertTrue(pageEmitter.emitPageCalled)
    }

    @Test
    fun emitPage_callsOnKeyLoaded() = runTest {
        val list = listOf("a", "b")

        pageEmitter.emitPage(list)

        coVerify(exactly = 1) { state.onKeyLoaded(key, list) }
    }

    @Test
    fun emitNextKey_whenImmediateLoadingDisabled_registersNewKey() = runTest {
        every { state.isImmediateLaunchScheduled() } returns false

        pageEmitter.emitNextKey(2)

        verify(exactly = 1) { state.registerKey(2) }
    }

    @Test
    fun emitNextKey_whenImmediateLoadingEnabled_launchesKey() = runTest {
        every { state.isImmediateLaunchScheduled() } returns true

        pageEmitter.emitNextKey(2)

        verify(exactly = 1) { launcher.launch(2) }
    }

    @Test
    fun emitNextKey_whenImmediateLoadingDisabled_doesNotLaunchKey() = runTest {
        every { state.isImmediateLaunchScheduled() } returns false

        pageEmitter.emitNextKey(2)

        verify(exactly = 0) { launcher.launch(any()) }
    }

    @Test
    fun emitNextKey_whenImmediateLoadingEnabled_doesNotRegisterKey() = runTest {
        every { state.isImmediateLaunchScheduled() } returns true

        pageEmitter.emitNextKey(2)

        verify(exactly = 0) { state.registerKey(any()) }
    }

    @Test
    fun emitNextKey_whenCalledTwice_throwsException() = runTest {
        every { state.isImmediateLaunchScheduled() } returns false
        pageEmitter.emitNextKey(2)

        val exception = runCatching { pageEmitter.emitNextKey(3) }.exceptionOrNull()

        assertNotNull(exception)
    }

}
