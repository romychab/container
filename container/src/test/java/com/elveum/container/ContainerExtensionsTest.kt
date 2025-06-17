package com.elveum.container

import com.elveum.container.utils.catch
import org.junit.Assert.*
import org.junit.Test


class ContainerExtensionsTest {

    @Test
    fun map_forPendingContainerAndNonEmptyMapper_returnsSameInstance() {
        val inputContainer: Container<String> = Container.Pending
        val outputContainer: Container<Int> = inputContainer.map { 1 }
        assertSame(inputContainer, outputContainer)
    }

    @Test
    fun map_forErrorContainerAndNonEmptyMapper_returnsErrorInstance() {
        val inputContainer: Container<String> = Container.Error(IllegalStateException())
        val outputContainer: Container<Int> = inputContainer.map { 1 }
        assertEquals(inputContainer, outputContainer)
    }

    @Test
    fun map_forSuccessContainerWithMapper_mapsValue() {
        val inputContainer = Container.Success("123")

        val outputContainer = inputContainer.map { it.toInt() }

        assertEquals(123, (outputContainer as Container.Success).value)
    }

    @Test
    fun map_forSuccessContainer_retainsSourceIndicator() {
        val inputContainer = Container.Success(
            value = "123",
            source = LocalSourceType
        )

        val outputContainer = inputContainer.map { it.toInt() }

        assertEquals(LocalSourceType, (outputContainer as Container.Success).source)
    }

    @Test
    fun exceptionOrNull_forErrorContainer_returnsException() {
        val expectedException = IllegalArgumentException("test")
        val container = Container.Error(expectedException)

        val exception = container.exceptionOrNull()

        assertSame(expectedException, exception)
    }

    @Test
    fun exceptionOrNull_forNonErrorContainer_returnsNull() {
        val pendingContainer = Container.Pending
        val successContainer = Container.Success("123")

        val pendingException = pendingContainer.exceptionOrNull()
        val successException = successContainer.exceptionOrNull()

        assertNull(pendingException)
        assertNull(successException)
    }

    @Test
    fun unwrap_forSuccessContainer_returnsValue() {
        val successContainer = Container.Success("123")

        val value = successContainer.unwrap()

        assertEquals("123", value)
    }

    @Test
    fun unwrap_forErrorContainer_throwsException() {
        val expectedException = Exception("123")
        val container = Container.Error(expectedException)

        val exception = catch<Exception> { container.unwrap() }

        assertSame(expectedException, exception)
    }

    @Test
    fun unwrap_forPendingContainer_throwsLoadInProgressException() {
        val container = Container.Pending

        catch<LoadInProgressException> { container.unwrap() }
    }

    @Test
    fun unwrapData_forSuccessContainer_returnsValueAndSourceIndicator() {
        val successContainer = Container.Success("123", LocalSourceType)

        val data = successContainer.unwrapSourceValue()

        assertEquals(SourceValue("123", LocalSourceType), data)
    }

    @Test
    fun unwrapData_forErrorContainer_throwsException() {
        val expectedException = Exception("123")
        val container = Container.Error(expectedException)

        val exception = catch<Exception> { container.unwrapSourceValue() }

        assertSame(expectedException, exception)
    }

    @Test
    fun unwrapData_forPendingContainer_throwsLoadInProgressException() {
        val container = Container.Pending

        catch<LoadInProgressException> { container.unwrapSourceValue() }
    }

    @Test
    fun unwrapOrNull_forSuccessContainer_returnsValue() {
        val successContainer = Container.Success("123")

        val data = successContainer.unwrapOrNull()

        assertEquals("123", data)
    }

    @Test
    fun unwrapOrNull_forErrorContainer_throwsException() {
        val expectedException = Exception("123")
        val container = Container.Error(expectedException)

        val exception = catch<Exception> { container.unwrapOrNull() }

        assertSame(expectedException, exception)
    }

    @Test
    fun unwrapOrNull_forPendingContainer_returnsNull() {
        val pendingContainer = Container.Pending

        val data = pendingContainer.unwrapOrNull()

        assertNull(data)
    }

    @Test
    fun unwrapDataOrNull_forSuccessContainer_returnsValueAndSourceIndicator() {
        val successContainer = Container.Success("123", LocalSourceType)

        val data = successContainer.unwrapSourceValueOrNull()

        assertEquals(SourceValue("123", LocalSourceType), data)
    }

    @Test
    fun unwrapDataOrNull_forErrorContainer_throwsException() {
        val expectedException = Exception("123")
        val container = Container.Error(expectedException)

        val exception = catch<Exception> { container.unwrapSourceValueOrNull() }

        assertSame(expectedException, exception)
    }

    @Test
    fun unwrapDataOrNull_forPendingContainer_returnsNull() {
        val pendingContainer = Container.Pending

        val data = pendingContainer.unwrapSourceValueOrNull()

        assertNull(data)
    }

    @Test
    fun getOrNull_forSuccessContainer_returnsValue() {
        val successContainer = Container.Success("123")

        val data = successContainer.getOrNull()

        assertEquals("123", data)
    }

    @Test
    fun getOrNull_forErrorContainer_returnsNull() {
        val errorContainer = Container.Error(Exception())

        val data = errorContainer.getOrNull()

        assertNull(data)
    }

    @Test
    fun getOrNull_forPendingContainer_returnsNull() {
        val pendingContainer = Container.Error(Exception())

        val data = pendingContainer.getOrNull()

        assertNull(data)
    }

    @Test
    fun getDataOrNull_forSuccessContainer_returnsValueAndSourceIndicator() {
        val successContainer = Container.Success("123", LocalSourceType)

        val data = successContainer.getSourceValueOrNull()

        assertEquals(SourceValue("123", LocalSourceType), data)
    }

    @Test
    fun getDataOrNull_forErrorContainer_returnsNull() {
        val errorContainer = Container.Error(Exception())

        val data = errorContainer.getSourceValueOrNull()

        assertNull(data)
    }

    @Test
    fun getDataOrNull_forPendingContainer_returnsNull() {
        val pendingContainer = Container.Pending

        val data = pendingContainer.getSourceValueOrNull()

        assertNull(data)
    }

}