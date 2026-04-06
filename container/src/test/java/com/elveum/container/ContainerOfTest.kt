package com.elveum.container

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

}
