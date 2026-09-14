package com.elveum.container

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class ContainerCatchExtensionsTest {

    @Test
    fun catchAll_forError_returnsContainer() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(FakeSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val exception = IllegalStateException()
        val inputContainer: Container<String> = errorContainer(exception, metadata)

        val container = inputContainer.catchAll {
            successContainer(it)
        }

        assertEquals(
            successContainer(exception, metadata),
            container,
        )
    }

    @Test
    fun catchAll_forCompletedError_returnsContainer() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(FakeSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val exception = IllegalStateException()
        val inputContainer: Container.Completed<String> = errorContainer(exception, metadata)

        val container = inputContainer.catchAll {
            successContainer(it)
        }

        assertEquals(
            successContainer(exception, metadata),
            container,
        )
    }

    @Test
    fun catchAll_forSuccess_returnsContainer() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(FakeSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val inputContainer: Container<Int> = successContainer(1, metadata)

        val container = inputContainer.catchAll {
            successContainer(it)
        }

        assertEquals(
            successContainer(1, metadata),
            container,
        )
    }

    @Test
    fun catchAll_forCompletedSuccess_returnsContainer() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(FakeSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val inputContainer: Container.Completed<Int> = successContainer(1, metadata)

        val container = inputContainer.catchAll {
            successContainer(it)
        }

        assertEquals(
            successContainer(1, metadata),
            container,
        )
    }

    @Test
    fun catchAll_withCancelledErrorExecution_throwsException() {
        val inputContainer: Container<Int> = errorContainer(IllegalStateException())

        val exception = runCatching {
            inputContainer.catchAll {
                throw CancellationException()
            }
        }.exceptionOrNull()

        assertTrue(exception is CancellationException)
    }

    @Test
    fun catchAll_withCompletedCancelledErrorExecution_throwsException() {
        val inputContainer: Container.Completed<Int> = errorContainer(IllegalStateException())

        val exception = runCatching {
            inputContainer.catchAll {
                throw CancellationException()
            }
        }.exceptionOrNull()

        assertTrue(exception is CancellationException)
    }

    @Test
    fun catch_forSuccess_returnsOrigin() {
        val inputContainer: Container<Int> = successContainer(0)

        val container = inputContainer.catch(IllegalStateException::class) {
            successContainer(1)
        }

        assertEquals(inputContainer, container)
    }

    @Test
    fun catch_forCompletedSuccess_returnsOrigin() {
        val inputContainer: Container.Completed<Int> = successContainer(0)

        val container = inputContainer.catch(IllegalStateException::class) {
            successContainer(1)
        }

        assertEquals(inputContainer, container)
    }

    @Test
    fun catch_forPending_returnsOrigin() {
        val inputContainer = pendingContainer()

        val container = inputContainer.catch(IllegalStateException::class) {
            successContainer(1)
        }

        assertEquals(inputContainer, container)
    }

    @Test
    fun catch_forOtherError_returnsOrigin() {
        val inputContainer: Container<Int> = errorContainer(IllegalArgumentException())

        val container = inputContainer.catch(IllegalStateException::class) {
            successContainer(1)
        }

        assertEquals(inputContainer, container)
    }

    @Test
    fun catch_forCompletedOtherError_returnsOrigin() {
        val inputContainer: Container.Completed<Int> = errorContainer(IllegalArgumentException())

        val container = inputContainer.catch(IllegalStateException::class) {
            successContainer(1)
        }

        assertEquals(inputContainer, container)
    }

    @Test
    fun catch_forMatchedError_returnsMappedContainer() {
        val inputContainer: Container<Int> = errorContainer(IllegalArgumentException())

        val container = inputContainer.catch(IllegalArgumentException::class) {
            successContainer(1)
        }

        assertEquals(successContainer(1), container)
    }

    @Test
    fun catch_forCompletedMatchedError_returnsMappedContainer() {
        val inputContainer: Container.Completed<Int> = errorContainer(IllegalArgumentException())

        val container = inputContainer.catch(IllegalArgumentException::class) {
            successContainer(1)
        }

        assertEquals(successContainer(1), container)
    }

    @Test
    fun catch_forMatchedSubError_returnsMappedContainer() {
        val inputContainer: Container<Int> = errorContainer(FileNotFoundException())

        val container = inputContainer.catch(IOException::class) {
            successContainer(1)
        }

        assertEquals(successContainer(1), container)
    }

    @Test
    fun catch_forCompletedMatchedSubError_returnsMappedContainer() {
        val inputContainer: Container.Completed<Int> = errorContainer(FileNotFoundException())

        val container = inputContainer.catch(IOException::class) {
            successContainer(1)
        }

        assertEquals(successContainer(1), container)
    }


    @Test
    fun mapException_forSuccess_returnsOrigin() {
        val inputContainer: Container<Int> = successContainer(1)

        val container = inputContainer.mapException(IOException::class) {
            CustomException(it)
        }

        assertEquals(inputContainer, container)
    }

    @Test
    fun mapException_forCompletedSuccess_returnsOrigin() {
        val inputContainer: Container.Completed<Int> = successContainer(1)

        val container = inputContainer.mapException(IOException::class) {
            CustomException(it)
        }

        assertEquals(inputContainer, container)
    }

    @Test
    fun mapException_forPending_returnsOrigin() {
        val inputContainer = pendingContainer()

        val container = inputContainer.mapException(IOException::class) {
            CustomException(it)
        }

        assertEquals(inputContainer, container)
    }

    @Test
    fun mapException_forOtherError_returnsOrigin() {
        val inputContainer: Container<Int> = errorContainer(IllegalStateException())

        val container = inputContainer.mapException(IOException::class) {
            CustomException(it)
        }

        assertEquals(inputContainer, container)
    }

    @Test
    fun mapException_forCompletedOtherError_returnsOrigin() {
        val inputContainer: Container.Completed<Int> = errorContainer(IllegalStateException())

        val container = inputContainer.mapException(IOException::class) {
            CustomException(it)
        }

        assertEquals(inputContainer, container)
    }

    @Test
    fun mapException_forMatchedError_returnsMappedContainer() {
        val inputContainer: Container<Int> = errorContainer(IllegalStateException())

        val container = inputContainer.mapException(IllegalStateException::class) {
            CustomException(it)
        }

        assertTrue(container.exceptionOrNull() is CustomException)
    }

    @Test
    fun mapException_forCompletedMatchedError_returnsMappedContainer() {
        val inputContainer: Container.Completed<Int> = errorContainer(IllegalStateException())

        val container = inputContainer.mapException(IllegalStateException::class) {
            CustomException(it)
        }

        assertTrue(container.exceptionOrNull() is CustomException)
    }

    @Test
    fun mapException_forMatchedSubError_returnsMappedContainer() {
        val inputContainer: Container<Int> = errorContainer(FileNotFoundException())

        val container = inputContainer.mapException(IOException::class) {
            CustomException(it)
        }

        assertTrue(container.exceptionOrNull() is CustomException)
    }

    @Test
    fun mapException_forCompletedMatchedSubError_returnsMappedContainer() {
        val inputContainer: Container.Completed<Int> = errorContainer(FileNotFoundException())

        val container = inputContainer.mapException(IOException::class) {
            CustomException(it)
        }

        assertTrue(container.exceptionOrNull() is CustomException)
    }


    @Test
    fun recover_forCompletedSuccess_returnsCompletedSuccessWithOriginValues() {
        val metadata = SourceTypeMetadata(LocalSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(mockk())
        val inputContainer: Container.Completed<Int> = successContainer(
            value = 1,
            metadata = metadata,
        )

        val container: Container.Completed<Int> = inputContainer.recover(IOException::class) { 2 }

        assertEquals(inputContainer, container)
    }

    @Test
    fun recover_forCompletedMatchedError_returnsCompletedMappedSuccess() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(LocalSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val inputContainer: Container.Completed<Int> = errorContainer(
            exception = IOException(),
            metadata = metadata,
        )
        val expectedOutputContainer: Container.Completed<Int> = successContainer(
            value = 2,
            metadata = metadata,
        )

        val container: Container.Completed<Int> = inputContainer.recover(IOException::class) { 2 }

        assertEquals(expectedOutputContainer, container)
    }

    @Test
    fun recover_forCompletedMatchedChildError_returnsCompletedMappedSuccess() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(LocalSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val inputContainer: Container.Completed<Int> = errorContainer(
            exception = FileNotFoundException(),
            metadata = metadata,
        )
        val expectedOutputContainer: Container.Completed<Int> = successContainer(
            value = 2,
            metadata = metadata,
        )

        val container: Container.Completed<Int> = inputContainer.recover(IOException::class) { 2 }

        assertEquals(expectedOutputContainer, container)
    }

    @Test
    fun recover_forCompletedNonMatchedError_returnsCompletedError() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(LocalSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val inputContainer: Container.Completed<Int> = errorContainer(
            exception = FileNotFoundException(),
            metadata = metadata,
        )

        val container: Container.Completed<Int> = inputContainer.recover(IllegalArgumentException::class) { 2 }

        assertEquals(inputContainer, container)
    }

    @Test
    fun recover_forSuccess_returnsSuccessWithOriginValues() {
        val metadata = SourceTypeMetadata(LocalSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(mockk())
        val inputContainer: Container<Int> = successContainer(
            value = 1,
            metadata = metadata,
        )

        val container = inputContainer.recover(IOException::class) { 2 }

        assertEquals(inputContainer, container)
    }

    @Test
    fun recover_forPending_returnsPending() {
        val inputContainer: Container<Int> = pendingContainer()

        val container = inputContainer.recover(IOException::class) { 2 }

        assertSame(inputContainer, container)
    }

    @Test
    fun recover_forMatchedError_returnsMappedSuccess() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(LocalSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val inputContainer: Container<Int> = errorContainer(
            exception = IOException(),
            metadata = metadata,
        )
        val expectedOutputContainer = successContainer(
            value = 2,
            metadata = metadata,
        )

        val container = inputContainer.recover(IOException::class) { 2 }

        assertEquals(expectedOutputContainer, container)
    }

    @Test
    fun recover_forMatchedChildError_returnsMappedSuccess() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(LocalSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val inputContainer: Container<Int> = errorContainer(
            exception = FileNotFoundException(),
            metadata = metadata,
        )
        val expectedOutputContainer = successContainer(
            value = 2,
            metadata = metadata,
        )

        val container = inputContainer.recover(IOException::class) { 2 }

        assertEquals(expectedOutputContainer, container)
    }

    @Test
    fun recover_forNonMatchedError_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(LocalSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val inputContainer: Container<Int> = errorContainer(
            exception = FileNotFoundException(),
            metadata = metadata,
        )

        val container = inputContainer.recover(IllegalArgumentException::class) { 2 }

        assertEquals(inputContainer, container)
    }

    private class CustomException(cause: Throwable) : Exception(cause)

}
