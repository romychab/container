package com.elveum.store.load

import com.elveum.store.StoreFactory
import com.elveum.store.base.AbstractStoreTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class OptionalStoreExtensionsTest : AbstractStoreTest() {

    private val exception = IllegalStateException("boom")

    @Test
    fun `GIVEN present optional WHEN firstOptionalGetOrThrow THEN unwrapped value is returned`() = runFlowTest {
        val flow = flowOf<StoreResult<Optional<String>>>(StoreResult.Loaded(Optional.of("value")))

        val result = flow.firstOptionalGetOrThrow()

        assertEquals("value", result)
    }

    @Test
    fun `GIVEN empty optional WHEN firstOptionalGetOrThrow THEN NoSuchElementException is thrown`() = runFlowTest {
        val flow = flowOf<StoreResult<Optional<String>>>(StoreResult.Loaded(Optional.empty()))

        val thrown = runCatching { flow.firstOptionalGetOrThrow() }.exceptionOrNull()

        assertTrue(thrown is NoSuchElementException)
    }

    @Test
    fun `GIVEN failed optional result WHEN firstOptionalGetOrThrow THEN the exception is thrown`() = runFlowTest {
        val flow = flowOf<StoreResult<Optional<String>>>(StoreResult.Failed(exception))

        val thrown = runCatching { flow.firstOptionalGetOrThrow() }.exceptionOrNull()

        assertSame(exception, thrown)
    }

    @Test
    fun `GIVEN present optional WHEN getOptionalValueOrNull THEN value is returned`() {
        val result: StoreResult<Optional<String>> = StoreResult.Loaded(Optional.of("value"))

        assertEquals("value", result.getOptionalValueOrNull())
    }

    @Test
    fun `GIVEN empty optional WHEN getOptionalValueOrNull THEN null is returned`() {
        val result: StoreResult<Optional<String>> = StoreResult.Loaded(Optional.empty())

        assertNull(result.getOptionalValueOrNull())
    }

    @Test
    fun `GIVEN loading result WHEN getOptionalValueOrNull THEN null is returned`() {
        val result: StoreResult<Optional<String>> = StoreResult.Loading

        assertNull(result.getOptionalValueOrNull())
    }

    @Test
    fun `GIVEN failed result WHEN getOptionalValueOrNull THEN null is returned`() {
        val result: StoreResult<Optional<String>> = StoreResult.Failed(exception)

        assertNull(result.getOptionalValueOrNull())
    }

    @Test
    fun `GIVEN null value WHEN toOptional THEN return empty optional`() {
        val input: String? = null

        val optional = input.toOptional()

        assertTrue(optional.getOrNull() == null)
    }


    @Test
    fun `GIVEN non-null value WHEN toOptional THEN return non-empty optional`() {
        val input: String = "value"

        val optional = input.toOptional()

        assertEquals(input, optional.get())
    }

    @Test
    fun `GIVEN loaded present value WHEN store getOptionalValueOrNull THEN value is returned`() = runFlowTest {
        val store = optionalSimpleStore { Optional.of("value") }
        store.observe().startCollecting()
        runCurrent()

        assertEquals("value", store.getOptionalValueOrNull())
        assertFalse(store.isOptionalEmpty())
    }

    @Test
    fun `GIVEN loaded empty value WHEN store getOptionalValueOrNull THEN null is returned and isOptionalEmpty is true`() =
        runFlowTest {
            val store = optionalSimpleStore { Optional.empty() }
            store.observe().startCollecting()
            runCurrent()

            assertNull(store.getOptionalValueOrNull())
            assertTrue(store.isOptionalEmpty())
        }

    @Test
    fun `GIVEN value still loading WHEN store getOptionalValueOrNull THEN null is returned and isOptionalEmpty is false`() =
        runFlowTest {
            val store = optionalSimpleStore {
                delay(10)
                Optional.of("value")
            }
            store.observe().startCollecting()
            runCurrent()

            // nothing is loaded yet -> not "empty", just unknown
            assertNull(store.getOptionalValueOrNull())
            assertFalse(store.isOptionalEmpty())
        }

    @Test
    fun `GIVEN loaded keys WHEN keyed getOptionalValueOrNull THEN each key resolves independently`() = runFlowTest {
        val store = optionalKeyedStore { key ->
            if (key == "empty") Optional.empty() else Optional.of("value-$key")
        }
        store.observe("k1").startCollecting()
        store.observe("empty").startCollecting()
        runCurrent()

        assertEquals("value-k1", store.getOptionalValueOrNull("k1"))
        assertFalse(store.isOptionalEmpty("k1"))

        assertNull(store.getOptionalValueOrNull("empty"))
        assertTrue(store.isOptionalEmpty("empty"))
    }

    private fun optionalSimpleStore(
        onFetch: suspend () -> Optional<String>,
    ) = StoreFactory.simpleStoreBuilder<Optional<String>>()
        .setCoroutineScopeFactory(createStoreScopeFactory())
        .build { onFetch() }

    private fun optionalKeyedStore(
        onFetch: suspend (String) -> Optional<String>,
    ) = StoreFactory.keyedStoreBuilder<String, Optional<String>>()
        .setCoroutineScopeFactory(createStoreScopeFactory())
        .build { key -> onFetch(key) }

}
