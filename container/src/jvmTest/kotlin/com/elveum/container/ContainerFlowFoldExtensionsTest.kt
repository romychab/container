package com.elveum.container

import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContainerFlowFoldExtensionsTest {

    @Test
    fun containerFold_pendingContainer_callsOnPending() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerFold(
            onSuccess = { "success:$it" },
            onError = { "error" },
            onPending = { "pending" },
        ).startCollecting()
        assertEquals("pending", collected.lastItem)
    }

    @Test
    fun containerFold_successContainer_callsOnSuccess() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(successContainer("hello"))
        val collected = flow.containerFold(
            onSuccess = { "success:$it" },
            onError = { "error" },
            onPending = { "pending" },
        ).startCollecting()
        assertEquals("success:hello", collected.lastItem)
    }

    @Test
    fun containerFold_errorContainer_callsOnError() = runFlowTest {
        val exception = IllegalStateException("boom")
        val flow = MutableStateFlow<Container<String>>(errorContainer(exception))
        val collected = flow.containerFold(
            onSuccess = { "success" },
            onError = { "error:${it.message}" },
            onPending = { "pending" },
        ).startCollecting()
        assertEquals("error:boom", collected.lastItem)
    }

    @Test
    fun containerFold_updatesOnFlowChanges() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerFold(
            onSuccess = { "success:$it" },
            onError = { "error" },
            onPending = { "pending" },
        ).startCollecting()

        assertEquals("pending", collected.lastItem)

        flow.value = successContainer("v1")
        assertEquals("success:v1", collected.lastItem)

        flow.value = errorContainer(RuntimeException())
        assertEquals("error", collected.lastItem)
    }

    @Test
    fun containerFoldDefault_pendingContainer_returnsDefault() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerFoldDefault(
            defaultValue = "default",
            onSuccess = { "success" },
            onError = { "default" },
            onPending = { "default" },
        ).startCollecting()
        assertEquals("default", collected.lastItem)
    }

    @Test
    fun containerFoldDefault_successContainer_callsOnSuccess() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(successContainer("val"))
        val collected = flow.containerFoldDefault(
            defaultValue = "default",
            onSuccess = { "success:$it" },
            onError = { "default" },
            onPending = { "default" },
        ).startCollecting()
        assertEquals("success:val", collected.lastItem)
    }

    @Test
    fun containerFoldDefault_errorContainer_returnsDefault() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(errorContainer(RuntimeException()))
        val collected = flow.containerFoldDefault(
            defaultValue = "default",
            onSuccess = { "success" },
            onError = { "default" },
            onPending = { "default" },
        ).startCollecting()
        assertEquals("default", collected.lastItem)
    }

    @Test
    fun containerFoldDefault_errorContainer_withOnErrorCallback_callsOnError() = runFlowTest {
        val exception = IllegalStateException()
        val flow = MutableStateFlow<Container<String>>(errorContainer(exception))
        val collected = flow.containerFoldDefault(
            defaultValue = "default",
            onSuccess = { "success" },
            onError = { "error" },
            onPending = { "default" },
        ).startCollecting()
        assertEquals("error", collected.lastItem)
    }

    @Test
    fun containerFoldDefault_pendingContainer_withOnPendingCallback_callsOnPending() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerFoldDefault(
            defaultValue = "default",
            onSuccess = { "success" },
            onError = { "default" },
            onPending = { "pending" },
        ).startCollecting()
        assertEquals("pending", collected.lastItem)
    }

    @Test
    fun containerFoldNullable_pendingContainer_returnsNull() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerFoldNullable(
            onSuccess = { it },
            onError = { null },
            onPending = { null },
        ).startCollecting()
        assertNull(collected.lastItem)
    }

    @Test
    fun containerFoldNullable_successContainer_callsOnSuccess() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(successContainer("val"))
        val collected = flow.containerFoldNullable(
            onSuccess = { it },
            onError = { null },
            onPending = { null },
        ).startCollecting()
        assertEquals("val", collected.lastItem)
    }

    @Test
    fun containerFoldNullable_errorContainer_returnsNull() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(errorContainer(RuntimeException()))
        val collected = flow.containerFoldNullable(
            onSuccess = { it },
            onError = { null },
            onPending = { null },
        ).startCollecting()
        assertNull(collected.lastItem)
    }

    @Test
    fun containerFoldNullable_errorContainer_withOnErrorCallback_callsOnError() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(errorContainer(RuntimeException()))
        val collected = flow.containerFoldNullable(
            onSuccess = { null },
            onError = { "error" },
            onPending = { null },
        ).startCollecting()
        assertEquals("error", collected.lastItem)
    }

    @Test
    fun containerFoldNullable_pendingContainer_withOnPendingCallback_callsOnPending() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerFoldNullable(
            onSuccess = { null },
            onError = { null },
            onPending = { "pending" },
        ).startCollecting()
        assertEquals("pending", collected.lastItem)
    }

    @Test
    fun containerTransform_pendingContainer_emitsPending() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerTransform(
            onSuccess = { successContainer("mapped:$it") },
            onError = { errorContainer(it) },
        ).startCollecting()
        assertEquals(pendingContainer(), collected.lastItem)
    }

    @Test
    fun containerTransform_successContainer_callsOnSuccess() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(successContainer("val"))
        val collected = flow.containerTransform(
            onSuccess = { successContainer("mapped:$it") },
            onError = { errorContainer(it) },
        ).startCollecting()
        assertEquals(successContainer("mapped:val"), collected.lastItem)
    }

    @Test
    fun containerTransform_errorContainer_callsOnError() = runFlowTest {
        val exception = IllegalStateException()
        val flow = MutableStateFlow<Container<String>>(errorContainer(exception))
        val collected = flow.containerTransform(
            onSuccess = { successContainer("mapped:$it") },
            onError = { successContainer("recovered") },
        ).startCollecting()
        assertEquals(successContainer("recovered"), collected.lastItem)
    }

    @Test
    fun containerTransform_updatesOnFlowChanges() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerTransform(
            onSuccess = { successContainer("mapped:$it") },
            onError = { successContainer("caught") },
        ).startCollecting()

        assertEquals(pendingContainer(), collected.lastItem)

        flow.value = successContainer("hello")
        assertEquals(successContainer("mapped:hello"), collected.lastItem)

        flow.value = errorContainer(RuntimeException())
        assertEquals(successContainer("caught"), collected.lastItem)
    }

    @Test
    fun containerTransform_preservesMetadataOnSuccess() = runFlowTest {
        val metadata = SourceTypeMetadata(RemoteSourceType)
        val flow = MutableStateFlow<Container<String>>(successContainer("val", metadata))
        val collected = flow.containerTransform(
            onSuccess = { successContainer("mapped:$it") },
            onError = { errorContainer(it) },
        ).startCollecting()
        assertEquals(RemoteSourceType, collected.lastItem.sourceType)
    }
}
