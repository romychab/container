package com.elveum.container

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerTest {

    @Test
    fun test_PendingContainer() = runTest {
        val container: Container<String> = pendingContainer()
        assertTrue(container == Container.Pending)
    }

    @Test
    fun test_SuccessContainer() = runTest {
        val expectedValue = "Success Value"
        val expectedSource = RemoteSourceType
        val expectedReloadFunction: ReloadFunction = mockk(relaxed = true)

        val container: Container<String> = successContainer(
            expectedValue, expectedSource, false, expectedReloadFunction
        )

        container as Container.Success
        assertSame(expectedValue, container.value)
        assertSame(expectedSource, container.source)
        assertFalse(container.isLoadingInBackground)
        container.reload(true)
        verify(exactly = 1) { expectedReloadFunction(true) }
    }

    @Test
    fun test_ErrorContainer() = runTest {
        val expectedException = IllegalStateException()
        val expectedSource = RemoteSourceType
        val expectedReloadFunction: ReloadFunction = mockk(relaxed = true)

        val container: Container<String> = errorContainer(
            expectedException, expectedSource, false, expectedReloadFunction
        )

        container as Container.Error
        assertSame(expectedException, container.exception)
        assertSame(expectedSource, container.source)
        assertFalse(container.isLoadingInBackground)
        container.reload(true)
        verify(exactly = 1) { expectedReloadFunction(true) }
    }

    @Test
    fun test_isLoading_forSuccess() {
        val defaultSuccess: Container.Success<String> = successContainer("1")
        val customSuccess: Container.Success<String> = successContainer("1", isLoadingInBackground = true)

        assertFalse(defaultSuccess.isLoadingInBackground)
        assertTrue(customSuccess.isLoadingInBackground)
    }


    @Test
    fun test_isLoading_forError() {
        val container1: Container.Error = errorContainer(IllegalStateException())
        val container2: Container.Error = errorContainer(IllegalStateException(),
        isLoadingInBackground = true)

        assertFalse(container1.isLoadingInBackground)
        assertTrue(container2.isLoadingInBackground)
    }

    @Test
    fun test_reloadFunction() {
        val reloadFunction = mockk<ReloadFunction>()
        val defaultSuccess: Container.Success<String> = successContainer("123")
        val defaultError: Container.Error = errorContainer(IllegalStateException())
        val customSuccess: Container.Success<String> = successContainer("123", reloadFunction = reloadFunction)
        val customError: Container.Error = errorContainer(IllegalStateException(), reloadFunction = reloadFunction)

        assertSame(defaultSuccess.reloadFunction, EmptyReloadFunction)
        assertSame(defaultError.reloadFunction, EmptyReloadFunction)
        assertSame(customSuccess.reloadFunction, reloadFunction)
        assertSame(customError.reloadFunction, reloadFunction)
    }

    @Test
    fun test_sourceType() {
        val sourceType = RemoteSourceType
        val defaultSuccess: Container.Success<String> = successContainer("123")
        val defaultError: Container.Error = errorContainer(IllegalStateException())
        val customSuccess: Container.Success<String> = successContainer("123", sourceType)
        val customError: Container.Error = errorContainer(IllegalStateException(), sourceType)

        assertSame(defaultSuccess.source, UnknownSourceType)
        assertSame(defaultError.source, UnknownSourceType)
        assertSame(customSuccess.source, sourceType)
        assertSame(customError.source, sourceType)
    }

    @Test
    fun test_fold_forSuccess() {
        val source = RemoteSourceType
        val reloadFunction = mockk<ReloadFunction>()
        val isLoading = true
        val origin = successContainer(
            value = 1,
            source = source,
            isLoadingInBackground = isLoading,
            reloadFunction = reloadFunction
        )

        val transformed = origin.fold(
            onSuccess = { successContainer(it * 10) },
            onError = { pendingContainer() },
            onPending = { pendingContainer() }
        )
        val containerValue = transformed.getContainerValueOrNull()

        assertEquals(10, containerValue?.value)
        assertEquals(source, containerValue?.source)
        assertEquals(isLoading, containerValue?.isLoadingInBackground)
        assertSame(reloadFunction, containerValue?.reloadFunction)
    }

    @Test
    fun test_fold_forError() {
        val exception = IllegalStateException()
        val source = RemoteSourceType
        val reloadFunction = mockk<ReloadFunction>()
        val isLoading = true
        val origin: Container<Int> = errorContainer(
            exception = exception,
            source = source,
            isLoadingInBackground = isLoading,
            reloadFunction = reloadFunction
        )

        val transformed = origin.fold(
            onError = {
                errorContainer(Exception("test", it))
            },
            onSuccess = { pendingContainer() },
            onPending = { pendingContainer() }
        )
        val containerValue = transformed.getContainerExceptionOrNull()

        assertEquals("test", containerValue?.value?.message)
        assertSame(exception, containerValue?.value?.cause)
        assertEquals(source, containerValue?.source)
        assertEquals(isLoading, containerValue?.isLoadingInBackground)
        assertSame(reloadFunction, containerValue?.reloadFunction)
    }

    @Test
    fun test_fold_forPending() {
        val origin: Container<Int> = Container.Pending

        val transformed = origin.fold(
            onPending = { successContainer(1) },
            onSuccess = { pendingContainer() },
            onError = { pendingContainer() }
        )

        assertEquals(successContainer(1), transformed)
    }


    @Test
    fun test_fold_forCompletedSuccess() {
        val source = RemoteSourceType
        val reloadFunction = mockk<ReloadFunction>()
        val isLoading = true
        val origin = successContainer(
            value = 1,
            source = source,
            isLoadingInBackground = isLoading,
            reloadFunction = reloadFunction
        )

        val transformed = origin.fold(
            onSuccess = { successContainer(it * 10) },
            onError = { pendingContainer() },
        )
        val containerValue = transformed.getContainerValueOrNull()

        assertEquals(10, containerValue?.value)
        assertEquals(source, containerValue?.source)
        assertEquals(isLoading, containerValue?.isLoadingInBackground)
        assertSame(reloadFunction, containerValue?.reloadFunction)
    }

    @Test
    fun test_fold_forCompletedError() {
        val exception = IllegalStateException()
        val source = RemoteSourceType
        val reloadFunction = mockk<ReloadFunction>()
        val isLoading = true
        val origin = errorContainer(
            exception = exception,
            source = source,
            isLoadingInBackground = isLoading,
            reloadFunction = reloadFunction
        )

        val transformed = origin.fold(
            onError = {
                errorContainer(Exception("test", it))
            },
            onSuccess = { pendingContainer() },
        )
        val containerValue = transformed.getContainerExceptionOrNull()

        assertEquals("test", containerValue?.value?.message)
        assertSame(exception, containerValue?.value?.cause)
        assertEquals(source, containerValue?.source)
        assertEquals(isLoading, containerValue?.isLoadingInBackground)
        assertSame(reloadFunction, containerValue?.reloadFunction)
    }

}
