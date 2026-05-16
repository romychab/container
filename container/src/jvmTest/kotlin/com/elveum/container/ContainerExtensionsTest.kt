package com.elveum.container

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerExtensionsTest {

    @Test
    fun test_foldDefault_withSuccessContainer() {
        val success = successContainer("1")

        // 0 functions
        assertEquals(
            "default",
            success.foldDefault("default")
        )
        // 1 function
        assertEquals(
            "default",
            success.foldDefault(
                "default",
                onError = { "error" },
            )
        )
        assertEquals(
            "default",
            success.foldDefault(
                "default",
                onPending = { "pending" },
            )
        )
        assertEquals(
            "success",
            success.foldDefault(
                "default",
                onSuccess = { "success" },
            )
        )
        // 2 functions
        assertEquals(
            "success",
            success.foldDefault(
                "default",
                onSuccess = { "success" },
                onError = { "error" },
            )
        )
        assertEquals(
            "success",
            success.foldDefault(
                "default",
                onSuccess = { "success" },
                onPending = { "pending" },
            )
        )
        assertEquals(
            "default",
            success.foldDefault(
                "default",
                onError = { "error" },
                onPending = { "pending" },
            )
        )
        // 3 functions
        assertEquals(
            "success",
            success.foldDefault(
                "default",
                onError = { "error" },
                onPending = { "pending" },
                onSuccess = { "success" },
            )
        )
    }

    @Test
    fun test_foldDefault_withErrorContainer() {
        val error = errorContainer(IllegalStateException())

        // 0 functions
        assertEquals(
            "default",
            error.foldDefault("default")
        )
        // 1 function
        assertEquals(
            "error",
            error.foldDefault(
                "default",
                onError = { "error" },
            )
        )
        assertEquals(
            "default",
            error.foldDefault(
                "default",
                onPending = { "pending" },
            )
        )
        assertEquals(
            "default",
            error.foldDefault(
                "default",
                onSuccess = { "success" },
            )
        )
        // 2 functions
        assertEquals(
            "error",
            error.foldDefault(
                "default",
                onSuccess = { "success" },
                onError = { "error" },
            )
        )
        assertEquals(
            "default",
            error.foldDefault(
                "default",
                onSuccess = { "success" },
                onPending = { "pending" },
            )
        )
        assertEquals(
            "error",
            error.foldDefault(
                "default",
                onError = { "error" },
                onPending = { "pending" },
            )
        )
        // 3 functions
        assertEquals(
            "error",
            error.foldDefault(
                "default",
                onError = { "error" },
                onPending = { "pending" },
                onSuccess = { "success" },
            )
        )
    }

    @Test
    fun test_foldDefault_withPendingContainer() {
        val pending = pendingContainer()

        // 0 functions
        assertEquals(
            "default",
            pending.foldDefault("default")
        )
        // 1 function
        assertEquals(
            "default",
            pending.foldDefault(
                "default",
                onError = { "error" },
            )
        )
        assertEquals(
            "pending",
            pending.foldDefault(
                "default",
                onPending = { "pending" },
            )
        )
        assertEquals(
            "default",
            pending.foldDefault(
                "default",
                onSuccess = { "success" },
            )
        )
        // 2 functions
        assertEquals(
            "default",
            pending.foldDefault(
                "default",
                onSuccess = { "success" },
                onError = { "error" },
            )
        )
        assertEquals(
            "pending",
            pending.foldDefault(
                "default",
                onSuccess = { "success" },
                onPending = { "pending" },
            )
        )
        assertEquals(
            "pending",
            pending.foldDefault(
                "default",
                onError = { "error" },
                onPending = { "pending" },
            )
        )
        // 3 functions
        assertEquals(
            "pending",
            pending.foldDefault(
                "default",
                onError = { "error" },
                onPending = { "pending" },
                onSuccess = { "success" },
            )
        )
    }

    @Test
    fun test_foldNullable_withSuccessContainer() {
        val success = successContainer("1")

        // 0 functions
        assertNull(success.foldNullable())
        // 1 function
        assertNull(success.foldNullable(onError = { "error" }))
        assertNull(success.foldNullable(onPending = { "pending" }))
        assertEquals("success", success.foldNullable(onSuccess = { "success" }))
        // 2 functions
        assertEquals(
            "success",
            success.foldNullable(
                onSuccess = { "success" },
                onError = { "error" },
            )
        )
        assertEquals(
            "success",
            success.foldNullable(
                onSuccess = { "success" },
                onPending = { "pending" },
            )
        )
        assertNull(success.foldNullable(
            onError = { "error" },
            onPending = { "pending" },
        ))
        // 3 functions
        assertEquals(
            "success",
            success.foldNullable(
                onError = { "error" },
                onPending = { "pending" },
                onSuccess = { "success" },
            )
        )
    }

    @Test
    fun test_foldNullable_withErrorContainer() {
        val error = errorContainer(IllegalStateException())

        // 0 functions
        assertNull(error.foldNullable())
        // 1 function
        assertEquals("error", error.foldNullable(onError = { "error" }))
        assertNull(error.foldNullable(onPending = { "pending" }))
        assertNull(error.foldNullable(onSuccess = { "success" }))
        // 2 functions
        assertEquals(
            "error",
            error.foldNullable(
                onSuccess = { "success" },
                onError = { "error" },
            )
        )
        assertNull(error.foldNullable(
            onSuccess = { "success" },
            onPending = { "pending" },
        ))
        assertEquals(
            "error",
            error.foldNullable(
                onError = { "error" },
                onPending = { "pending" },
            )
        )
        // 3 functions
        assertEquals(
            "error",
            error.foldNullable(
                onError = { "error" },
                onPending = { "pending" },
                onSuccess = { "success" },
            )
        )
    }

    @Test
    fun test_foldNullable_withPendingContainer() {
        val pending = pendingContainer()

        // 0 functions
        assertNull(pending.foldNullable())
        // 1 function
        assertNull(pending.foldNullable(onError = { "error" }))
        assertEquals("pending", pending.foldNullable(onPending = { "pending" }))
        assertNull(pending.foldNullable(onSuccess = { "success" }))
        // 2 functions
        assertNull(pending.foldNullable(
            onSuccess = { "success" },
            onError = { "error" },
        ))
        assertEquals(
            "pending",
            pending.foldNullable(
                onSuccess = { "success" },
                onPending = { "pending" },
            )
        )
        assertEquals(
            "pending",
            pending.foldNullable(
                onError = { "error" },
                onPending = { "pending" },
            )
        )
        // 3 functions
        assertEquals(
            "pending",
            pending.foldNullable(
                onError = { "error" },
                onPending = { "pending" },
                onSuccess = { "success" },
            )
        )
    }

    @Test
    fun exceptionOrNull_forErrorContainer_returnsException() {
        val expectedException = IllegalArgumentException("test")
        val container = errorContainer(expectedException)

        val exception = container.exceptionOrNull()

        assertSame(expectedException, exception)
    }

    @Test
    fun exceptionOrNull_forNonErrorContainer_returnsNull() {
        val pendingContainer = pendingContainer()
        val successContainer = successContainer("123")

        val pendingException = pendingContainer.exceptionOrNull()
        val successException = successContainer.exceptionOrNull()

        assertNull(pendingException)
        assertNull(successException)
    }

    @Test
    fun getOrNull_forSuccessContainer_returnsValue() {
        val successContainer = successContainer("123")

        val data = successContainer.getOrNull()

        assertEquals("123", data)
    }

    @Test
    fun getOrNull_forErrorContainer_returnsNull() {
        val errorContainer = errorContainer(Exception())

        val data = errorContainer.getOrNull()

        assertNull(data)
    }

    @Test
    fun getOrNull_forPendingContainer_returnsNull() {
        val pendingContainer = pendingContainer()

        val data = pendingContainer.getOrNull()

        assertNull(data)
    }

    @Test
    fun unwrap_forPendingContainer_throwsLoadNotFinishedException() {
        val container = pendingContainer()

        val exception = runCatching { container.unwrap() }
            .exceptionOrNull()

        assertTrue(exception is LoadNotFinishedException)
    }

    @Test
    fun unwrap_forErrorContainer_throwsEncapsulatedException() {
        val expectedException = IllegalArgumentException()
        val container = errorContainer(expectedException)

        val exception = runCatching { container.unwrap() }
            .exceptionOrNull()

        assertSame(expectedException, exception)
    }

    @Test
    fun unwrap_forSuccessContainer_returnsValue() {
        val expectedValue = "value"
        val container = successContainer(expectedValue)

        val value = runCatching { container.unwrap() }.getOrNull()

        assertSame(expectedValue, value)
    }

    @Test
    fun unwrapContainerValue_forSuccessContainer_returnsValue() {
        val expectedValue = "value"
        val expectedSource = SourceTypeMetadata(LocalSourceType)
        val expectedIsLoadingInBackground = BackgroundLoadMetadata(BackgroundLoadState.Loading)
        val expectedReloadFunction = ReloadFunctionMetadata(mockk<ReloadFunction>())
        val container = successContainer(expectedValue, expectedSource +
                expectedIsLoadingInBackground + expectedReloadFunction)

        val value = container.unwrapContainerValue()

        assertEquals(expectedSource.sourceType, value.sourceType)
        assertEquals(expectedIsLoadingInBackground.backgroundLoadState, value.backgroundLoadState)
        assertSame(expectedReloadFunction.reloadFunction, value.reloadFunction)
        assertEquals(expectedValue, value.value)
    }

    @Test
    fun test_getContainerValueOrNull() {
        val reloadFunction = mockk<ReloadFunction>()
        val pending: Container<String> = pendingContainer()
        val metadata = SourceTypeMetadata(RemoteSourceType) + BackgroundLoadMetadata(BackgroundLoadState.Loading) + ReloadFunctionMetadata(reloadFunction)
        val success: Container<String> = successContainer("test", metadata)
        val error: Container<String> = errorContainer(IllegalStateException(), metadata)

        assertNull(pending.getContainerValueOrNull())
        assertNull(error.getContainerValueOrNull())
        val successValue = success.getContainerValueOrNull()
        assertSame(reloadFunction, successValue?.reloadFunction)
        assertEquals("test", successValue?.value)
        assertEquals(RemoteSourceType, successValue?.sourceType)
        assertEquals(BackgroundLoadState.Loading, successValue!!.backgroundLoadState)
    }

    @Test
    fun test_getContainerExceptionOrNull() {
        val reloadFunction = mockk<ReloadFunction>()
        val pending: Container<String> = pendingContainer()
        val metadata = SourceTypeMetadata(RemoteSourceType) + BackgroundLoadMetadata(BackgroundLoadState.Loading) + ReloadFunctionMetadata(reloadFunction)
        val success: Container<String> = successContainer("test", metadata)
        val error: Container<String> = errorContainer(IllegalStateException(), metadata)

        assertNull(pending.getContainerExceptionOrNull())
        assertNull(success.getContainerExceptionOrNull())
        val errorValue = error.getContainerExceptionOrNull()
        assertSame(reloadFunction, errorValue?.reloadFunction)
        assertTrue(errorValue?.value is IllegalStateException)
        assertEquals(RemoteSourceType, errorValue?.sourceType)
        assertEquals(BackgroundLoadState.Loading, errorValue!!.backgroundLoadState)
    }

    @Test
    fun test_updateWithBlock_forSuccessContainer() {
        val reloadFunctionBefore: ReloadFunction = mockk()
        val expectedReloadFunction: ReloadFunction = mockk()
        val expectedSource = RemoteSourceType
        val expectedValue = "value"
        val expectedBackgroundLoadState = BackgroundLoadState.Error(RuntimeException(""))
        val container = successContainer(
            value = "value",
            metadata = SourceTypeMetadata(LocalSourceType) +
                    BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                    ReloadFunctionMetadata(reloadFunctionBefore),
        )

        val updatedContainer = container.update {
            assertSame(LocalSourceType, sourceType)
            assertEquals(BackgroundLoadState.Loading, backgroundLoadState)
            assertSame(reloadFunctionBefore, reloadFunction)

            sourceType = expectedSource
            backgroundLoadState = expectedBackgroundLoadState
            reloadFunction = expectedReloadFunction
        } as Container.Success<String>

        assertEquals(expectedValue, updatedContainer.value)
        assertSame(expectedSource, updatedContainer.sourceType)
        assertSame(expectedReloadFunction, updatedContainer.reloadFunction)
        assertEquals(expectedBackgroundLoadState, updatedContainer.backgroundLoadState)
    }

    @Test
    fun isError_forErrorContainer_returnsTrue() {
        assertTrue(errorContainer(RuntimeException()).isError())
    }

    @Test
    fun isError_forSuccessContainer_returnsFalse() {
        assertFalse(successContainer("v").isError())
    }

    @Test
    fun isError_forPendingContainer_returnsFalse() {
        assertFalse(pendingContainer().isError())
    }

    @Test
    fun isPending_forPendingContainer_returnsTrue() {
        assertTrue(pendingContainer().isPending())
    }

    @Test
    fun isPending_forSuccessContainer_returnsFalse() {
        assertFalse(successContainer("v").isPending())
    }

    @Test
    fun isPending_forErrorContainer_returnsFalse() {
        assertFalse(errorContainer(RuntimeException()).isPending())
    }

    @Test
    fun isDataLoading_forPendingContainer_returnsTrue() {
        assertTrue(pendingContainer().isDataLoading())
    }

    @Test
    fun isDataLoading_forSuccessWithBackgroundLoading_returnsTrue() {
        val container = successContainer("v", BackgroundLoadMetadata(BackgroundLoadState.Loading))
        assertTrue(container.isDataLoading())
    }

    @Test
    fun isDataLoading_forSuccessWithoutBackgroundLoading_returnsFalse() {
        assertFalse(successContainer("v").isDataLoading())
    }

    @Test
    fun isDataLoading_forErrorWithBackgroundLoading_returnsTrue() {
        val container = errorContainer(RuntimeException(), BackgroundLoadMetadata(BackgroundLoadState.Loading))
        assertTrue(container.isDataLoading())
    }

    @Test
    fun test_updateWithBlock_forErrorContainer() {
        val reloadFunctionBefore: ReloadFunction = mockk()
        val expectedException = IllegalStateException()
        val expectedReloadFunction: ReloadFunction = mockk()
        val expectedSource = RemoteSourceType
        val expectedBackgroundLoadState = BackgroundLoadState.Error(RuntimeException(""))
        val container = errorContainer(
            exception = expectedException,
            metadata = SourceTypeMetadata(LocalSourceType) +
                    BackgroundLoadMetadata(BackgroundLoadState.Loading) +
                    ReloadFunctionMetadata(reloadFunctionBefore),
        )

        val updatedContainer = container.update {
            assertSame(LocalSourceType, sourceType)
            assertEquals(BackgroundLoadState.Loading, backgroundLoadState)
            assertSame(reloadFunctionBefore, reloadFunction)

            sourceType = expectedSource
            backgroundLoadState = expectedBackgroundLoadState
            reloadFunction = expectedReloadFunction
        } as Container.Error

        assertEquals(expectedException, updatedContainer.exception)
        assertSame(expectedSource, updatedContainer.sourceType)
        assertSame(expectedReloadFunction, updatedContainer.reloadFunction)
        assertEquals(expectedBackgroundLoadState, updatedContainer.backgroundLoadState)
    }

}