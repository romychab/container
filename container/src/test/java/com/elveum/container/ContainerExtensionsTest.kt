package com.elveum.container

import io.mockk.mockk
import org.junit.Assert.assertEquals
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
        val pendingContainer = errorContainer(Exception())

        val data = pendingContainer.getOrNull()

        assertNull(data)
    }


    @Test
    fun test_getContainerValueOrNull() {
        val reloadFunction = mockk<ReloadFunction>()
        val pending: Container<String> = pendingContainer()
        val success: Container<String> = successContainer("test", RemoteSourceType, true, reloadFunction)
        val error: Container<String> = errorContainer(IllegalStateException(), RemoteSourceType, true, reloadFunction)

        assertNull(pending.getContainerValueOrNull())
        assertNull(error.getContainerValueOrNull())
        val successValue = success.getContainerValueOrNull()
        assertSame(reloadFunction, successValue?.reloadFunction)
        assertEquals("test", successValue?.value)
        assertEquals(RemoteSourceType, successValue?.source)
        assertTrue(successValue!!.isLoadingInBackground)
    }

    @Test
    fun test_getContainerExceptionOrNull() {
        val reloadFunction = mockk<ReloadFunction>()
        val pending: Container<String> = pendingContainer()
        val success: Container<String> = successContainer("test", RemoteSourceType, true, reloadFunction)
        val error: Container<String> = errorContainer(IllegalStateException(), RemoteSourceType, true, reloadFunction)

        assertNull(pending.getContainerExceptionOrNull())
        assertNull(success.getContainerExceptionOrNull())
        val errorValue = error.getContainerExceptionOrNull()
        assertSame(reloadFunction, errorValue?.reloadFunction)
        assertTrue(errorValue?.value is IllegalStateException)
        assertEquals(RemoteSourceType, errorValue?.source)
        assertTrue(errorValue!!.isLoadingInBackground)
    }

    @Test
    fun test_update() {
        val originReloadFunction = mockk<ReloadFunction>()
        val newReloadFunction = mockk<ReloadFunction>()
        val origin = successContainer("1", LocalSourceType, isLoadingInBackground = false, originReloadFunction)

        // update source
        val updatedSource = origin.update(source = RemoteSourceType)
        assertEquals(
            successContainer("1", RemoteSourceType, isLoadingInBackground = false, originReloadFunction),
            updatedSource,
        )

        // update isLoading
        val updatedLoading = origin.update(isLoadingInBackground = true)
        assertEquals(
            successContainer("1", LocalSourceType, isLoadingInBackground = true, originReloadFunction),
            updatedLoading,
        )

        // update reloadFunction
        val updatedReloadFunction = origin.update(reloadFunction = newReloadFunction)
        assertEquals(
            successContainer("1", LocalSourceType, isLoadingInBackground = false, newReloadFunction),
            updatedReloadFunction,
        )
    }

}