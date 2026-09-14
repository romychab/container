package com.elveum.container.subject.paging.internal

import com.elveum.container.ContainerFlow
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.StatefulEmitter
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PageEmitterImplTest {

    @RelaxedMockK
    private lateinit var context: PageContext<Int, String>

    @RelaxedMockK
    private lateinit var originEmitter: StatefulEmitter<List<String>>

    private lateinit var emitter: PageEmitterImpl<Int, String>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        emitter = PageEmitterImpl(context, originEmitter)
    }

    @Test
    fun isPageEmitted_isFalseBeforeEmittingPage() {
        assertFalse(emitter.isPageEmitted)
    }

    @Test
    fun isPageEmitted_isTrueAfterEmittingPage() = runTest {
        emitter.emitPage(listOf("a"))
        assertTrue(emitter.isPageEmitted)
    }

    @Test
    fun emitPage_callsOnPageDataLoadedWithGivenList() = runTest {
        emitter.emitPage(listOf("x", "y", "z"))
        coVerify { context.onPageDataLoaded(listOf("x", "y", "z"), EmptyMetadata) }
    }

    @Test
    fun emitPage_forwardsProvidedMetadataToContext() = runTest {
        val metadata = mockk<ContainerMetadata>()

        emitter.emitPage(listOf("a"), metadata)

        coVerify { context.onPageDataLoaded(listOf("a"), metadata) }
    }

    @Test
    fun emitPage_setsIsPageEmitted_onEachCall() = runTest {
        emitter.emitPage(listOf("a"))
        emitter.emitPage(listOf("b"))

        assertTrue(emitter.isPageEmitted)
        coVerifyOrder {
            context.onPageDataLoaded(listOf("a"), EmptyMetadata)
            context.onPageDataLoaded(listOf("b"), EmptyMetadata)
        }
    }

    @Test
    fun emitPage_withEmptyList_stillSetsIsPageEmitted() = runTest {
        emitter.emitPage(emptyList())
        assertTrue(emitter.isPageEmitted)
    }

    @Test
    fun emitNextKey_callsScheduleNextKey_forNewKey() = runTest {
        emitter.emitNextKey(42)
        coVerify { context.scheduleNextKey(42) }
    }

    @Test
    fun emitNextKey_doesNotCallScheduleNextKey_forDuplicateKey() = runTest {
        emitter.emitNextKey(1)
        emitter.emitNextKey(1)

        coVerify(exactly = 1) { context.scheduleNextKey(1) }
    }

    @Test
    fun emitNextKey_callsScheduleNextKey_forEachDistinctKey() = runTest {
        emitter.emitNextKey(1)
        emitter.emitNextKey(2)
        emitter.emitNextKey(3)

        coVerify(exactly = 1) { context.scheduleNextKey(1) }
        coVerify(exactly = 1) { context.scheduleNextKey(2) }
        coVerify(exactly = 1) { context.scheduleNextKey(3) }
    }

    @Test
    fun emitNextKey_afterDuplicate_stillAllowsNewKeys() = runTest {
        emitter.emitNextKey(1)
        emitter.emitNextKey(1) // duplicate, ignored
        emitter.emitNextKey(2) // new key, should be scheduled

        coVerify(exactly = 1) { context.scheduleNextKey(1) }
        coVerify(exactly = 1) { context.scheduleNextKey(2) }
    }

    @Test
    fun dependsOnFlow_delegatesToOriginEmitter() = runTest {
        val flowProvider = mockk<() -> Flow<String>>()

        emitter.dependsOnFlow(1, 2, flow = flowProvider)

        coVerify(exactly = 1) {
            originEmitter.dependsOnFlow(1, 2, flow = flowProvider)
        }
    }

    @Test
    fun dependsOnContainerFlow_delegatesToOriginEmitter() = runTest {
        val flowProvider = mockk<() -> ContainerFlow<String>>()

        emitter.dependsOnContainerFlow(1, 2, flow = flowProvider)

        coVerify(exactly = 1) {
            originEmitter.dependsOnContainerFlow(1, 2, flow = flowProvider)
        }
    }

}
