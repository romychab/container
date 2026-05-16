package com.elveum.container

import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerFlowCatchExtensionsTest {

    @Test
    fun containerCatchAll_pendingContainer_emitsPending() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerCatchAll { successContainer("recovered") }.startCollecting()
        assertEquals(pendingContainer(), collected.lastItem)
    }

    @Test
    fun containerCatchAll_successContainer_emitsUnchanged() = runFlowTest {
        val input = successContainer("value")
        val flow = MutableStateFlow<Container<String>>(input)
        val collected = flow.containerCatchAll { successContainer("recovered") }.startCollecting()
        assertEquals(input, collected.lastItem)
    }

    @Test
    fun containerCatchAll_errorContainer_callsMapper() = runFlowTest {
        val exception = IllegalStateException()
        val flow = MutableStateFlow<Container<String>>(errorContainer(exception))
        val collected = flow.containerCatchAll { successContainer("recovered") }.startCollecting()
        assertEquals(successContainer("recovered"), collected.lastItem)
    }

    @Test
    fun containerCatchAll_updatesAcrossMultipleEmissions() = runFlowTest {
        val exception = IllegalStateException()
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerCatchAll { successContainer("recovered") }.startCollecting()

        flow.value = errorContainer(exception)
        assertEquals(successContainer("recovered"), collected.lastItem)

        flow.value = successContainer("direct")
        assertEquals(successContainer("direct"), collected.lastItem)
    }

    @Test
    fun containerCatchAll_errorContainer_mapperCanReturnError() = runFlowTest {
        val original = IllegalStateException()
        val replacement = RuntimeException("new")
        val flow = MutableStateFlow<Container<String>>(errorContainer(original))
        val collected = flow.containerCatchAll { errorContainer(replacement) }.startCollecting()
        assertSame(replacement, collected.lastItem.exceptionOrNull())
    }

    @Test
    fun containerCatch_matchingException_callsMapper() = runFlowTest {
        val exception = IllegalStateException()
        val flow = MutableStateFlow<Container<String>>(errorContainer(exception))
        val collected = flow.containerCatch(IllegalStateException::class) { successContainer("caught") }.startCollecting()
        assertEquals(successContainer("caught"), collected.lastItem)
    }

    @Test
    fun containerCatch_nonMatchingException_keepsOriginalError() = runFlowTest {
        val exception = IllegalArgumentException()
        val flow = MutableStateFlow<Container<String>>(errorContainer(exception))
        val collected = flow.containerCatch(IllegalStateException::class) { successContainer("caught") }.startCollecting()
        assertTrue(collected.lastItem.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun containerCatch_pendingContainer_emitsPending() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerCatch(IllegalStateException::class) { successContainer("caught") }.startCollecting()
        assertEquals(pendingContainer(), collected.lastItem)
    }

    @Test
    fun containerCatch_successContainer_emitsUnchanged() = runFlowTest {
        val input = successContainer("value")
        val flow = MutableStateFlow<Container<String>>(input)
        val collected = flow.containerCatch(IllegalStateException::class) { successContainer("caught") }.startCollecting()
        assertEquals(input, collected.lastItem)
    }

    @Test
    fun containerMapException_matchingException_mapsToNewException() = runFlowTest {
        val original = IllegalStateException()
        val mapped = RuntimeException("mapped")
        val flow = MutableStateFlow<Container<String>>(errorContainer(original))
        val collected = flow.containerMapException(IllegalStateException::class) { mapped }.startCollecting()
        assertSame(mapped, collected.lastItem.exceptionOrNull())
    }

    @Test
    fun containerMapException_nonMatchingException_keepsOriginal() = runFlowTest {
        val exception = IllegalArgumentException()
        val flow = MutableStateFlow<Container<String>>(errorContainer(exception))
        val collected = flow.containerMapException(IllegalStateException::class) { RuntimeException() }.startCollecting()
        assertSame(exception, collected.lastItem.exceptionOrNull())
    }

    @Test
    fun containerMapException_successContainer_emitsUnchanged() = runFlowTest {
        val input = successContainer("value")
        val flow = MutableStateFlow<Container<String>>(input)
        val collected = flow.containerMapException(IllegalStateException::class) { RuntimeException() }.startCollecting()
        assertEquals(input, collected.lastItem)
    }

    @Test
    fun containerMapException_pendingContainer_emitsPending() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerMapException(IllegalStateException::class) { RuntimeException() }.startCollecting()
        assertEquals(pendingContainer(), collected.lastItem)
    }

    @Test
    fun containerRecover_matchingException_recoversToSuccess() = runFlowTest {
        val exception = IllegalStateException()
        val flow = MutableStateFlow<Container<String>>(errorContainer(exception))
        val collected = flow.containerRecover(IllegalStateException::class) { "recovered" }.startCollecting()
        assertEquals(successContainer("recovered"), collected.lastItem)
    }

    @Test
    fun containerRecover_nonMatchingException_keepsError() = runFlowTest {
        val exception = IllegalArgumentException()
        val flow = MutableStateFlow<Container<String>>(errorContainer(exception))
        val collected = flow.containerRecover(IllegalStateException::class) { "recovered" }.startCollecting()
        assertTrue(collected.lastItem.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun containerRecover_successContainer_emitsUnchanged() = runFlowTest {
        val input = successContainer("value")
        val flow = MutableStateFlow<Container<String>>(input)
        val collected = flow.containerRecover(IllegalStateException::class) { "recovered" }.startCollecting()
        assertEquals(input, collected.lastItem)
    }

    @Test
    fun containerRecover_pendingContainer_emitsPending() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerRecover(IllegalStateException::class) { "recovered" }.startCollecting()
        assertEquals(pendingContainer(), collected.lastItem)
    }

    @Test
    fun containerRecover_updatesAcrossMultipleEmissions() = runFlowTest {
        val flow = MutableStateFlow<Container<String>>(pendingContainer())
        val collected = flow.containerRecover(IllegalStateException::class) { "recovered" }.startCollecting()

        flow.value = errorContainer(IllegalStateException())
        assertEquals(successContainer("recovered"), collected.lastItem)

        flow.value = errorContainer(IllegalArgumentException())
        assertTrue(collected.lastItem.exceptionOrNull() is IllegalArgumentException)

        flow.value = successContainer("direct")
        assertEquals(successContainer("direct"), collected.lastItem)
    }

}
