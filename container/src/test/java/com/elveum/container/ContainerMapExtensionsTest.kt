package com.elveum.container

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class ContainerMapExtensionsTest {

    @Test
    fun transform_forSuccessContainer_executesOnSuccess() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val origin: Container<Int> = successContainer(0, metadata)

        val transformed = origin.transform(
            onSuccess = { successContainer(it + 1) },
            onError = { errorContainer(it) }
        )

        assertEquals(
            successContainer(1, metadata),
            transformed,
        )
    }

    @Test
    fun transform_forCompletedSuccessContainer_executesOnSuccess() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val origin: Container.Completed<Int> = successContainer(0, metadata)

        val transformed = origin.transform(
            onSuccess = { successContainer(it + 1) },
            onError = { errorContainer(it) }
        )

        assertEquals(
            successContainer(1, metadata),
            transformed,
        )
    }

    @Test
    fun transform_forErrorContainer_executesOnError() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val origin: Container<Int> = errorContainer(
            IllegalStateException(), metadata
        )

        val transformed = origin.transform(
            onSuccess = { successContainer(it) },
            onError = { errorContainer(Exception(it)) }
        )

        assertTrue(
            transformed.exceptionOrNull()?.cause is IllegalStateException
        )
        val containerException = transformed.getContainerExceptionOrNull()
        assertEquals(RemoteSourceType, containerException?.sourceType)
        assertEquals(BackgroundLoadState.Loading, containerException?.backgroundLoadState)
        assertEquals(reloadFunction, containerException?.reloadFunction)
    }

    @Test
    fun transform_forCompletedErrorContainer_executesOnError() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val origin: Container.Completed<String> = errorContainer(
            IllegalStateException(), metadata
        )

        val transformed = origin.transform(
            onSuccess = { successContainer(it) },
            onError = { errorContainer(Exception(it)) }
        )

        assertTrue(
            transformed.exceptionOrNull()?.cause is IllegalStateException
        )
        val containerException = transformed.getContainerExceptionOrNull()
        assertEquals(RemoteSourceType, containerException?.sourceType)
        assertEquals(BackgroundLoadState.Loading, containerException?.backgroundLoadState)
        assertEquals(reloadFunction, containerException?.reloadFunction)
    }

    @Test
    fun transform_forPendingContainer_returnsPending() {
        val origin: Container<Int> = pendingContainer()

        val transformed = origin.transform(
            onSuccess = { successContainer(it) },
            onError = { errorContainer(it) }
        )

        assertEquals(pendingContainer(), transformed)
    }

    @Test
    fun transform_withFailedSuccessTransformation_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val origin: Container<Int> = successContainer(0, metadata)
        val exception = IllegalStateException()

        val transformed = origin.transform(
            onSuccess = { throw exception },
            onError = { errorContainer(it) }
        )

        assertEquals(
            errorContainer(exception, metadata),
            transformed,
        )
    }

    @Test
    fun transform_withFailedSuccessTransformationForCompletedOrigin_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val origin: Container.Completed<Int> = successContainer(0, metadata)
        val exception = IllegalStateException()

        val transformed = origin.transform(
            onSuccess = { throw exception },
            onError = { errorContainer(it) }
        )

        assertEquals(
            errorContainer(exception, metadata),
            transformed,
        )
    }

    @Test
    fun transform_withFailedErrorTransformation_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val origin: Container<Int> = errorContainer(Exception(), metadata)
        val exception = IllegalStateException()

        val transformed = origin.transform(
            onSuccess = { successContainer(0) },
            onError = { throw exception }
        )

        assertEquals(
            errorContainer(exception, metadata),
            transformed,
        )
    }

    @Test
    fun transform_withFailedErrorTransformationForCompletedOrigin_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val origin: Container.Completed<Int> = errorContainer(Exception(), metadata)
        val exception = IllegalStateException()

        val transformed = origin.transform(
            onSuccess = { successContainer(0) },
            onError = { throw exception }
        )

        assertEquals(
            errorContainer(exception, metadata),
            transformed,
        )
    }

    @Test
    fun transform_withCancelledSuccessTransformation_rethrowsException() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val origin: Container<Int> = successContainer(0, metadata)

        val exception = runCatching {
            origin.transform(
                onSuccess = { throw CancellationException() },
                onError = { errorContainer(it) }
            )
        }.exceptionOrNull()

        assertTrue(exception is CancellationException)
    }

    @Test
    fun transform_withCancelledSuccessTransformationForCompletedOrigin_rethrowsException() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val origin: Container.Completed<Int> = successContainer(0, metadata)

        val exception = runCatching {
            origin.transform(
                onSuccess = { throw CancellationException() },
                onError = { errorContainer(it) }
            )
        }.exceptionOrNull()

        assertTrue(exception is CancellationException)
    }

    @Test
    fun transform_withCancelledErrorTransformation_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val origin: Container<String> = errorContainer(Exception(), metadata)

        val exception = runCatching {
            origin.transform(
                onSuccess = { successContainer(it) },
                onError = { throw CancellationException() }
            )
        }.exceptionOrNull()

        assertTrue(exception is CancellationException)
    }

    @Test
    fun transform_withCancelledErrorTransformationForCompletedOrigin_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(RemoteSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val origin: Container.Completed<String> = errorContainer(Exception(), metadata)

        val exception = runCatching {
            origin.transform(
                onSuccess = { successContainer(it) },
                onError = { throw CancellationException() }
            )
        }.exceptionOrNull()

        assertTrue(exception is CancellationException)
    }

    @Test
    fun map_forPendingContainer_returnsSameInstance() {
        val inputContainer: Container<Int> = Container.Pending
        val outputContainer: Container<Int> = inputContainer.map { it * 10 }
        assertSame(inputContainer, outputContainer)
    }

    @Test
    fun map_forErrorContainer_returnsErrorInstance() {
        val exception = IllegalStateException()
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(FakeSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val inputContainer: Container<String> = errorContainer(exception, metadata)
        val outputContainer: Container<Int> = inputContainer.map { 1 }
        assertEquals(
            errorContainer(exception, metadata),
            outputContainer,
        )
    }

    @Test
    fun map_forCompletedErrorContainer_returnsErrorInstance() {
        val exception = IllegalStateException()
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(FakeSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val inputContainer: Container.Completed<String> = errorContainer(exception, metadata)
        val outputContainer: Container<Int> = inputContainer.map { 1 }
        assertEquals(
            errorContainer(exception, metadata),
            outputContainer,
        )
    }

    @Test
    fun map_forSuccessContainer_mapsValue() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(FakeSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val inputContainer: Container<String> = successContainer("123", metadata)

        val outputContainer = inputContainer.map { it.toInt() }

        assertEquals(
            successContainer(123, metadata),
            outputContainer,
        )
    }

    @Test
    fun map_forCompletedSuccessContainer_mapsValue() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(FakeSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val inputContainer: Container.Completed<String> = successContainer("123", metadata)

        val outputContainer = inputContainer.map { it.toInt() }

        assertEquals(
            successContainer(123, metadata),
            outputContainer,
        )
    }

    @Test
    fun map_forFailedMapping_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(FakeSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val exception = IllegalStateException()
        val inputContainer: Container<String> = successContainer("123", metadata)

        val outputContainer = inputContainer.map { throw exception }

        assertEquals(
            errorContainer(exception, metadata),
            outputContainer,
        )
    }

    @Test
    fun map_forCompletedFailedMapping_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(FakeSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val exception = IllegalStateException()
        val inputContainer: Container.Completed<String> = successContainer("123", metadata)

        val outputContainer = inputContainer.map { throw exception }

        assertEquals(
            errorContainer(exception, metadata),
            outputContainer,
        )
    }

    @Test
    fun map_forCancelledMapping_throwsException() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(FakeSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val expectedException = CancellationException()
        val inputContainer: Container<String> = successContainer("123", metadata)

        val exception = runCatching {
            inputContainer.map { throw expectedException }
        }.exceptionOrNull()

        assertEquals(expectedException, exception)
    }

    @Test
    fun map_forCompletedCancelledMapping_throwsException() {
        val reloadFunction = mockk<ReloadFunction>()
        val metadata = SourceTypeMetadata(FakeSourceType) +
                BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                ReloadFunctionMetadata(reloadFunction)
        val expectedException = CancellationException()
        val inputContainer: Container.Completed<String> = successContainer("123", metadata)

        val exception = runCatching {
            inputContainer.map { throw expectedException }
        }.exceptionOrNull()

        assertEquals(expectedException, exception)
    }


}
