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
        val origin: Container<Int> = successContainer(0, RemoteSourceType, isLoadingInBackground = true, reloadFunction)

        val transformed = origin.transform(
            onSuccess = { successContainer(it + 1) },
            onError = { errorContainer(it) }
        )

        assertEquals(
            successContainer(1, RemoteSourceType, isLoadingInBackground = true, reloadFunction),
            transformed,
        )
    }

    @Test
    fun transform_forCompletedSuccessContainer_executesOnSuccess() {
        val reloadFunction = mockk<ReloadFunction>()
        val origin: Container.Completed<Int> = successContainer(0, RemoteSourceType, isLoadingInBackground = true, reloadFunction)

        val transformed = origin.transform(
            onSuccess = { successContainer(it + 1) },
            onError = { errorContainer(it) }
        )

        assertEquals(
            successContainer(1, RemoteSourceType, isLoadingInBackground = true, reloadFunction),
            transformed,
        )
    }

    @Test
    fun transform_forErrorContainer_executesOnError() {
        val reloadFunction = mockk<ReloadFunction>()
        val origin: Container<Int> = errorContainer(
            IllegalStateException(), RemoteSourceType, isLoadingInBackground = true, reloadFunction
        )

        val transformed = origin.transform(
            onSuccess = { successContainer(it) },
            onError = { errorContainer(Exception(it)) }
        )

        assertTrue(
            transformed.exceptionOrNull()?.cause is IllegalStateException
        )
        val containerException = transformed.getContainerExceptionOrNull()
        assertEquals(RemoteSourceType, containerException?.source)
        assertTrue(containerException?.isLoadingInBackground ?: false)
        assertEquals(reloadFunction, containerException?.reloadFunction)
    }

    @Test
    fun transform_forCompletedErrorContainer_executesOnError() {
        val reloadFunction = mockk<ReloadFunction>()
        val origin: Container.Completed<String> = errorContainer(
            IllegalStateException(), RemoteSourceType, isLoadingInBackground = true, reloadFunction
        )

        val transformed = origin.transform(
            onSuccess = { successContainer(it) },
            onError = { errorContainer(Exception(it)) }
        )

        assertTrue(
            transformed.exceptionOrNull()?.cause is IllegalStateException
        )
        val containerException = transformed.getContainerExceptionOrNull()
        assertEquals(RemoteSourceType, containerException?.source)
        assertTrue(containerException?.isLoadingInBackground ?: false)
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
        val origin: Container<Int> = successContainer(0, RemoteSourceType, isLoadingInBackground = true, reloadFunction)
        val exception = IllegalStateException()

        val transformed = origin.transform(
            onSuccess = { throw exception },
            onError = { errorContainer(it) }
        )

        assertEquals(
            errorContainer(exception, RemoteSourceType, isLoadingInBackground = true, reloadFunction),
            transformed,
        )
    }

    @Test
    fun transform_withFailedSuccessTransformationForCompletedOrigin_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val origin: Container.Completed<Int> = successContainer(0, RemoteSourceType, isLoadingInBackground = true, reloadFunction)
        val exception = IllegalStateException()

        val transformed = origin.transform(
            onSuccess = { throw exception },
            onError = { errorContainer(it) }
        )

        assertEquals(
            errorContainer(exception, RemoteSourceType, isLoadingInBackground = true, reloadFunction),
            transformed,
        )
    }

    @Test
    fun transform_withFailedErrorTransformation_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val origin: Container<Int> = errorContainer(Exception(), RemoteSourceType, isLoadingInBackground = true, reloadFunction)
        val exception = IllegalStateException()

        val transformed = origin.transform(
            onSuccess = { successContainer(0) },
            onError = { throw exception }
        )

        assertEquals(
            errorContainer(exception, RemoteSourceType, isLoadingInBackground = true, reloadFunction),
            transformed,
        )
    }

    @Test
    fun transform_withFailedErrorTransformationForCompletedOrigin_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val origin: Container.Completed<Int> = errorContainer(Exception(), RemoteSourceType, isLoadingInBackground = true, reloadFunction)
        val exception = IllegalStateException()

        val transformed = origin.transform(
            onSuccess = { successContainer(0) },
            onError = { throw exception }
        )

        assertEquals(
            errorContainer(exception, RemoteSourceType, isLoadingInBackground = true, reloadFunction),
            transformed,
        )
    }

    @Test
    fun transform_withCancelledSuccessTransformation_rethrowsException() {
        val reloadFunction = mockk<ReloadFunction>()
        val origin: Container<Int> = successContainer(0, RemoteSourceType, isLoadingInBackground = true, reloadFunction)

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
        val origin: Container.Completed<Int> = successContainer(0, RemoteSourceType, isLoadingInBackground = true, reloadFunction)

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
        val origin: Container<String> = errorContainer(Exception(), RemoteSourceType, isLoadingInBackground = true, reloadFunction)

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
        val origin: Container.Completed<String> = errorContainer(Exception(), RemoteSourceType, isLoadingInBackground = true, reloadFunction)

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
        val inputContainer: Container<String> = errorContainer(exception, FakeSourceType, true, reloadFunction)
        val outputContainer: Container<Int> = inputContainer.map { 1 }
        assertEquals(
            errorContainer(exception, FakeSourceType, true, reloadFunction),
            outputContainer,
        )
    }

    @Test
    fun map_forCompletedErrorContainer_returnsErrorInstance() {
        val exception = IllegalStateException()
        val reloadFunction = mockk<ReloadFunction>()
        val inputContainer: Container.Completed<String> = errorContainer(exception, FakeSourceType, true, reloadFunction)
        val outputContainer: Container<Int> = inputContainer.map { 1 }
        assertEquals(
            errorContainer(exception, FakeSourceType, true, reloadFunction),
            outputContainer,
        )
    }

    @Test
    fun map_forSuccessContainer_mapsValue() {
        val reloadFunction = mockk<ReloadFunction>()
        val inputContainer: Container<String> = successContainer("123", FakeSourceType, true, reloadFunction)

        val outputContainer = inputContainer.map { it.toInt() }

        assertEquals(
            successContainer(123, FakeSourceType, true, reloadFunction),
            outputContainer,
        )
    }

    @Test
    fun map_forCompletedSuccessContainer_mapsValue() {
        val reloadFunction = mockk<ReloadFunction>()
        val inputContainer: Container.Completed<String> = successContainer("123", FakeSourceType, true, reloadFunction)

        val outputContainer = inputContainer.map { it.toInt() }

        assertEquals(
            successContainer(123, FakeSourceType, true, reloadFunction),
            outputContainer,
        )
    }

    @Test
    fun map_forFailedMapping_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val exception = IllegalStateException()
        val inputContainer: Container<String> = successContainer("123", FakeSourceType, true, reloadFunction)

        val outputContainer = inputContainer.map { throw exception }

        assertEquals(
            errorContainer(exception, FakeSourceType, true, reloadFunction),
            outputContainer,
        )
    }

    @Test
    fun map_forCompletedFailedMapping_returnsError() {
        val reloadFunction = mockk<ReloadFunction>()
        val exception = IllegalStateException()
        val inputContainer: Container.Completed<String> = successContainer("123", FakeSourceType, true, reloadFunction)

        val outputContainer = inputContainer.map { throw exception }

        assertEquals(
            errorContainer(exception, FakeSourceType, true, reloadFunction),
            outputContainer,
        )
    }

    @Test
    fun map_forCancelledMapping_throwsException() {
        val reloadFunction = mockk<ReloadFunction>()
        val expectedException = CancellationException()
        val inputContainer: Container<String> = successContainer("123", FakeSourceType, true, reloadFunction)

        val exception = runCatching {
            inputContainer.map { throw expectedException }
        }.exceptionOrNull()

        assertEquals(expectedException, exception)
    }

    @Test
    fun map_forCompletedCancelledMapping_throwsException() {
        val reloadFunction = mockk<ReloadFunction>()
        val expectedException = CancellationException()
        val inputContainer: Container.Completed<String> = successContainer("123", FakeSourceType, true, reloadFunction)

        val exception = runCatching {
            inputContainer.map { throw expectedException }
        }.exceptionOrNull()

        assertEquals(expectedException, exception)
    }

    @Test
    fun catchAll_forError_returnsContainer() {
        val reloadFunction = mockk<ReloadFunction>()
        val exception = IllegalStateException()
        val inputContainer: Container<String> = errorContainer(exception, FakeSourceType, true, reloadFunction)

        val container = inputContainer.catchAll {
            successContainer(it)
        }

        assertEquals(
            successContainer(exception, FakeSourceType, true, reloadFunction),
            container,
        )
    }

    @Test
    fun catchAll_forCompletedError_returnsContainer() {
        val reloadFunction = mockk<ReloadFunction>()
        val exception = IllegalStateException()
        val inputContainer: Container.Completed<String> = errorContainer(exception, FakeSourceType, true, reloadFunction)

        val container = inputContainer.catchAll {
            successContainer(it)
        }

        assertEquals(
            successContainer(exception, FakeSourceType, true, reloadFunction),
            container,
        )
    }

    @Test
    fun catchAll_forSuccess_returnsContainer() {
        val reloadFunction = mockk<ReloadFunction>()
        val inputContainer: Container<Int> = successContainer(1, FakeSourceType, true, reloadFunction)

        val container = inputContainer.catchAll {
            successContainer(it)
        }

        assertEquals(
            successContainer(1, FakeSourceType, true, reloadFunction),
            container,
        )
    }

    @Test
    fun catchAll_forCompletedSuccess_returnsContainer() {
        val reloadFunction = mockk<ReloadFunction>()
        val inputContainer: Container.Completed<Int> = successContainer(1, FakeSourceType, true, reloadFunction)

        val container = inputContainer.catchAll {
            successContainer(it)
        }

        assertEquals(
            successContainer(1, FakeSourceType, true, reloadFunction),
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

    private class CustomException(cause: Throwable) : Exception(cause)

}