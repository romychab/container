@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.paging.internal

import com.elveum.container.ContainerMetadata
import com.elveum.container.StatefulEmitter
import com.elveum.container.subject.paging.PageEmitter
import com.elveum.container.subject.paging.PageState
import com.elveum.container.subject.paging.nextPageState
import com.elveum.container.subject.paging.onItemRendered
import io.mockk.MockKAnnotations
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class PageLoaderTest {

    @RelaxedMockK
    private lateinit var state: PageLoaderState<Int, String>

    @RelaxedMockK
    private lateinit var launcher: PageLoadTaskLauncher<Int, String>

    @RelaxedMockK
    private lateinit var statefulEmitter: StatefulEmitter<List<String>>

    private lateinit var loader: PageLoaderImpl<Int, String>

    @MockK
    private lateinit var factory: PageLoaderImpl.Factory<Int, String>

    private lateinit var block: suspend PageEmitter<Int, String>.(Int) -> Unit

    private val initialKey: Int = 1

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        block = mockk(relaxed = true)
        every { factory.createState(any(), any(), any()) } returns state
        every { factory.createLauncher(any(), any(), any()) } returns launcher
        loader = PageLoaderImpl(initialKey, threshold = 0.5f, emitMetadata = true, block = block, factory = factory)
    }

    @Test
    fun onItemRendered_whenStatefulInvokeIsBeingLaunchedAndNextKeyExists_launchesLoadForNextKey() = runTest {
        coEvery { state.await() } just awaits
        every { launcher.findNextKeyForIndex(5) } returns 2
        backgroundScope.launch { with(loader) { statefulEmitter.statefulInvoke() } }
        runCurrent()

        loader.onItemRendered(5)

        verify(exactly = 1) { launcher.launch(2) }
    }

    @Test
    fun onItemRendered_whenStatefulInvokeIsBeingLaunched_findsNextKeyForCorrectIndex() = runTest {
        coEvery { state.await() } just awaits
        every { launcher.findNextKeyForIndex(7) } returns null
        backgroundScope.launch { with(loader) { statefulEmitter.statefulInvoke() } }
        runCurrent()

        loader.onItemRendered(7)

        verify(exactly = 1) { launcher.findNextKeyForIndex(7) }
    }

    @Test
    fun onItemRendered_whenStatefulInvokeIsBeingLaunchedAndNextKeyDoesNotExist_doesNotLaunchLoadForNextKey() = runTest {
        coEvery { state.await() } just awaits
        every { launcher.findNextKeyForIndex(5) } returns null
        backgroundScope.launch { with(loader) { statefulEmitter.statefulInvoke() } }
        runCurrent()

        loader.onItemRendered(5)

        verify(exactly = 1) { launcher.launch(any()) } // only initialKey
    }

    @Test
    fun onItemRendered_whenStatefulInvokeIsNotLaunched_nothingHappens() {
        loader.onItemRendered(5)

        verify(exactly = 0) { launcher.findNextKeyForIndex(any()) }
    }

    @Test
    fun onItemRendered_afterStatefulInvokeIsCompleted_nothingHappens() = runTest {
        coEvery { state.await() } just runs
        with(loader) { statefulEmitter.statefulInvoke() }

        loader.onItemRendered(5)

        verify(exactly = 0) { launcher.findNextKeyForIndex(any()) }
    }

    @Test
    fun statefulInvoke_createsStateAndLauncherWithSupervisorScope() = runTest {
        coEvery { state.await() } just runs

        with(loader) { statefulEmitter.statefulInvoke() }

        verify(exactly = 1) { factory.createState(eq(statefulEmitter), any(), any()) }
        verify(exactly = 1) {
            factory.createLauncher(
                state = eq(state),
                coroutineScope = match { it.toString().contains("SupervisorCoroutine") },
                emitter = eq(statefulEmitter)
            )
        }
    }

    @Test
    fun statefulInvoke_launchesLauncherWithInitialKeyAndAwaitsResults() = runTest {
        coEvery { state.await() } just runs

        with(loader) { statefulEmitter.statefulInvoke() }

        verify(exactly = 1) { launcher.launch(initialKey) }
        coVerify(exactly = 1) { state.await() }
    }

    @Test
    fun statefulInvoke_createsLauncherWithPreviouslyCreatedState() = runTest {
        coEvery { state.await() } just runs

        with(loader) { statefulEmitter.statefulInvoke() }

        verify(exactly = 1) { factory.createLauncher(eq(state), any(), any()) }
    }

    @Test
    fun statefulInvoke_afterLoadCompletion_setsStateToIdle() = runTest {
        coEvery { state.await() } just runs
        loader.nextPageState.value = PageState.Pending

        with(loader) { statefulEmitter.statefulInvoke() }

        assertEquals(PageState.Idle, loader.nextPageState.value)
    }

    @Test
    fun statefulInvoke_emitsCompletedStateOnlyAfterAwait() = runTest {
        val deferred = CompletableDeferred<Unit>()
        coEvery { state.await() } coAnswers { deferred.await() }

        backgroundScope.launch { with(loader) { statefulEmitter.statefulInvoke() } }
        runCurrent()

        coVerify(exactly = 0) {
            statefulEmitter.emitCompletedState()
        }

        deferred.complete(Unit)
        runCurrent()

        coVerify(exactly = 1) {
            statefulEmitter.emitCompletedState()
        }
    }

    @Test
    fun nextPageState_initialValue_isIdle() {
        assertEquals(PageState.Idle, loader.nextPageState.value)
    }

    @Test
    fun statefulInvoke_whenCreatingState_usesLambdaForUpdatingNextPageState() = runTest {
        val onNextPageChangedSlot = slot<(PageState) -> Unit>()
        every { factory.createState(any(), capture(onNextPageChangedSlot), any()) } returns state
        coEvery { state.await() } just awaits

        backgroundScope.launch { with(loader) { statefulEmitter.statefulInvoke() } }
        runCurrent()

        onNextPageChangedSlot.captured.invoke(PageState.Pending)
        assertEquals(PageState.Pending, loader.nextPageState.value)
    }

    @Test
    fun statefulInvoke_whenCreatingState_createsValidMetadataProvider() = runTest {
        val expectedPageState = PageState.Error(IllegalStateException()) {}
        val metadataProviderSlot = slot<() -> ContainerMetadata>()
        every { factory.createState(any(), any(), capture(metadataProviderSlot)) } returns state
        every { launcher.findNextKeyForIndex(2) } returns 10
        coEvery { state.await() } just awaits
        backgroundScope.launch { with(loader) { statefulEmitter.statefulInvoke() } }
        runCurrent()

        loader.nextPageState.value = expectedPageState
        val metadata = metadataProviderSlot.captured.invoke()

        assertSame(expectedPageState, metadata.nextPageState)
        metadata.onItemRendered(2)
        verify(exactly = 1) {
            launcher.findNextKeyForIndex(2)
            launcher.launch(10)
        }
    }

}
