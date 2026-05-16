package com.elveum.container

import com.uandcode.flowtest.runFlowTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class ContainerOfTest {

    @Test
    fun containerOf_forCompletedBlock_returnsSuccessContainer() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(LocalSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val container = containerOf(
            metadata = metadata,
        ) { "hello" }

        assertEquals(
            successContainer("hello", metadata),
            container,
        )
    }

    @Test
    fun containerOf_forFailedBlock_returnsErrorContainer() {
        val exception = IllegalStateException()
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val container = containerOf(
            metadata = metadata,
        ) { throw exception }

        assertEquals(
            errorContainer(exception, metadata),
            container,
        )
    }

    @Test
    fun containerOf_forCancelledBlock_rethrowsCancellationException() {
        val expectedException = CancellationException()

        val exception = runCatching {
            containerOf { throw expectedException }
        }.exceptionOrNull()

        assertSame(expectedException, exception)
    }

    // --- containerFlowOf ---

    @Test
    fun containerFlowOf_emitsPendingFirst() = runFlowTest {
        val collected = containerFlowOf<String> { /* no emissions */ }.startCollecting()
        assertEquals(pendingContainer(), collected.collectedItems.first())
    }

    @Test
    fun containerFlowOf_emitsSingleItemWhenBlockCompletes() = runFlowTest {
        val collected = containerFlowOf<String> { /* no emissions */ }.startCollecting()
        assertEquals(listOf(pendingContainer()), collected.collectedItems)
    }

    @Test
    fun containerFlowOf_withException_emitsErrorAfterPending() = runFlowTest {
        val exception = IllegalStateException("fail")
        val collected = containerFlowOf<String> { throw exception }.startCollecting()
        assertEquals(
            listOf(pendingContainer(), errorContainer(exception)),
            collected.collectedItems,
        )
    }

    @Test
    fun containerFlowOf_withCustomMetadata_attachesMetadataToErrorContainer() = runFlowTest {
        val metadata = SourceTypeMetadata(LocalSourceType)
        val exception = RuntimeException()
        val collected = containerFlowOf<String>(metadata) { throw exception }.startCollecting()
        val errorItem = collected.collectedItems.last()
        assertEquals(LocalSourceType, errorItem.sourceType)
    }

}
