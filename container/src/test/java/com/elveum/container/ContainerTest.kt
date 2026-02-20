package com.elveum.container

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        val container2: Container.Error = errorContainer(IllegalStateException(), isLoadingInBackground = true)

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

    @Test
    fun successContainer_withMetadata_hasCorrectMetadata() {
        val metadata = defaultMetadata(
            source = RemoteSourceType,
            isLoadingInBackground = true,
        )

        val container = successContainer("hello", metadata)

        assertEquals("hello", container.value)
        assertSame(metadata, container.metadata)
        assertEquals(RemoteSourceType, container.source)
        assertTrue(container.isLoadingInBackground)
    }

    @Test
    fun successContainer_withDefaultMetadata_hasEmptyMetadata() {
        val container = successContainer("hello")

        assertSame(EmptyMetadata, container.metadata)
    }

    @Test
    fun errorContainer_withMetadata_hasCorrectMetadata() {
        val exception = IllegalStateException()
        val metadata = defaultMetadata(
            source = LocalSourceType,
            isLoadingInBackground = false,
        )

        val container = errorContainer(exception, metadata)

        assertSame(exception, container.exception)
        assertSame(metadata, container.metadata)
        assertEquals(LocalSourceType, container.source)
        assertFalse(container.isLoadingInBackground)
    }

    @Test
    fun errorContainer_withDefaultMetadata_hasEmptyMetadata() {
        val container = errorContainer(IllegalStateException())

        assertSame(EmptyMetadata, container.metadata)
    }

    @Test
    fun containerPending_filterMetadata_returnsPending() {
        val result = Container.Pending.filterMetadata { true }

        assertSame(Container.Pending, result)
    }

    @Test
    fun containerSuccess_filterMetadata_matchingPredicate_retainsMetadata() {
        val container = successContainer(
            "value",
            defaultMetadata(source = RemoteSourceType, isLoadingInBackground = true),
        )

        val result = container.filterMetadata { it is SourceTypeMetadata }

        result as Container.Success
        assertEquals("value", result.value)
        assertEquals(RemoteSourceType, result.source)
        assertFalse(result.isLoadingInBackground)
    }

    @Test
    fun containerSuccess_filterMetadata_nonMatchingPredicate_removesAllMetadata() {
        val container = successContainer(
            "value",
            defaultMetadata(source = RemoteSourceType, isLoadingInBackground = true),
        )

        val result = container.filterMetadata { false }

        result as Container.Success
        assertEquals("value", result.value)
        assertSame(EmptyMetadata, result.metadata)
    }

    @Test
    fun containerError_filterMetadata_matchingPredicate_retainsMetadata() {
        val exception = IllegalStateException()
        val container = errorContainer(
            exception,
            defaultMetadata(source = LocalSourceType, isLoadingInBackground = true),
        )

        val result = container.filterMetadata { it is IsLoadingInBackgroundMetadata }

        assertSame(exception, result.exception)
        assertTrue(result.isLoadingInBackground)
        assertEquals(UnknownSourceType, result.source)
    }

    @Test
    fun containerError_filterMetadata_nonMatchingPredicate_removesAllMetadata() {
        val exception = IllegalStateException()
        val container = errorContainer(
            exception,
            defaultMetadata(source = LocalSourceType, isLoadingInBackground = true),
        )

        val result = container.filterMetadata { false }

        assertSame(exception, result.exception)
        assertSame(EmptyMetadata, result.metadata)
    }

    @Test
    fun containerPending_raw_returnsPending() {
        val result = Container.Pending.raw()

        assertSame(Container.Pending, result)
    }

    @Test
    fun containerSuccess_raw_removesAllMetadata() {
        val container = successContainer(
            42,
            defaultMetadata(source = RemoteSourceType, isLoadingInBackground = true),
        )

        val result = container.raw()

        result as Container.Success
        assertEquals(42, result.value)
        assertSame(EmptyMetadata, result.metadata)
    }

    @Test
    fun containerError_raw_removesAllMetadata() {
        val exception = IllegalStateException()
        val container = errorContainer(
            exception,
            defaultMetadata(source = RemoteSourceType, isLoadingInBackground = true),
        )

        val result = container.raw()

        assertSame(exception, result.exception)
        assertSame(EmptyMetadata, result.metadata)
    }

    @Test
    fun containerPending_plusMetadata_returnsPending() {
        val result = Container.Pending + defaultMetadata(source = RemoteSourceType)

        assertSame(Container.Pending, result)
    }

    @Test
    fun containerSuccess_plusMetadata_appendsMetadata() {
        val reloadFunction = mockk<ReloadFunction>()
        val container = successContainer("value", defaultMetadata(source = RemoteSourceType))

        val result = container + defaultMetadata(
            isLoadingInBackground = true,
            reloadFunction = reloadFunction,
        )

        assertEquals("value", result.value)
        assertEquals(RemoteSourceType, result.source)
        assertTrue(result.isLoadingInBackground)
        assertSame(reloadFunction, result.reloadFunction)
    }

    @Test
    fun containerSuccess_plusMetadata_overridesExistingMetadataOfSameType() {
        val container = successContainer("value", defaultMetadata(source = LocalSourceType))

        val result = container + defaultMetadata(source = RemoteSourceType)

        assertEquals(RemoteSourceType, result.source)
    }

    @Test
    fun containerError_plusMetadata_appendsMetadata() {
        val exception = IllegalStateException()
        val reloadFunction = mockk<ReloadFunction>()
        val container = errorContainer(exception, defaultMetadata(source = LocalSourceType))

        val result = container + defaultMetadata(
            isLoadingInBackground = true,
            reloadFunction = reloadFunction,
        )

        assertSame(exception, result.exception)
        assertEquals(LocalSourceType, result.source)
        assertTrue(result.isLoadingInBackground)
        assertSame(reloadFunction, result.reloadFunction)
    }

    @Test
    fun containerError_plusMetadata_overridesExistingMetadataOfSameType() {
        val exception = IllegalStateException()
        val container = errorContainer(exception, defaultMetadata(source = RemoteSourceType))

        val result = container + defaultMetadata(source = LocalSourceType)

        assertEquals(LocalSourceType, result.source)
    }

}
