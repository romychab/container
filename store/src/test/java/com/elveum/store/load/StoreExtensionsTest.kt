package com.elveum.store.load

import com.elveum.store.StoreFactory
import com.elveum.store.base.AbstractStoreTest
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class StoreExtensionsTest : AbstractStoreTest() {

    private val exception = IllegalStateException("boom")

    private fun simpleStoreBuilder() = StoreFactory
        .simpleStoreBuilder<String>()
        .setCoroutineScopeFactory(createStoreScopeFactory())

    private fun keyedStoreBuilder() = StoreFactory
        .simpleStoreBuilder<String>()
        .withKeys<String>()
        .setCoroutineScopeFactory(createStoreScopeFactory())

    @Test
    fun `GIVEN loaded simple store WHEN getOrNull THEN value is returned`() = runFlowTest {
        val store = simpleStoreBuilder().build { "value" }
        store.observe().startCollecting()
        runCurrent()

        assertEquals("value", store.getOrNull())
    }

    @Test
    fun `GIVEN loading simple store WHEN getOrNull THEN null is returned`() = runFlowTest {
        val store = simpleStoreBuilder().build {
            delay(10)
            "value"
        }
        store.observe().startCollecting()
        runCurrent()

        assertNull(store.getOrNull())
    }

    @Test
    fun `GIVEN failed simple store WHEN getOrNull THEN null is returned`() = runFlowTest {
        val store = simpleStoreBuilder().build { throw exception }
        store.observe().startCollecting()
        runCurrent()

        assertNull(store.getOrNull())
    }

    @Test
    fun `GIVEN failed simple store WHEN failureOrNull THEN exception is returned`() = runFlowTest {
        val store = simpleStoreBuilder().build { throw exception }
        store.observe().startCollecting()
        runCurrent()

        assertSame(exception, store.failureOrNull())
    }

    @Test
    fun `GIVEN loaded simple store WHEN failureOrNull THEN null is returned`() = runFlowTest {
        val store = simpleStoreBuilder().build { "value" }
        store.observe().startCollecting()
        runCurrent()

        assertNull(store.failureOrNull())
    }

    @Test
    fun `GIVEN loaded keyed store WHEN getOrNull by key THEN value of that key is returned`() = runFlowTest {
        val store = keyedStoreBuilder().build { key -> "value-$key" }
        store.observe("k1").startCollecting()
        runCurrent()

        assertEquals("value-k1", store.getOrNull("k1"))
    }

    @Test
    fun `GIVEN loading keyed store WHEN getOrNull by key THEN null is returned`() = runFlowTest {
        val store = keyedStoreBuilder().build { key ->
            delay(10)
            "value-$key"
        }
        store.observe("k1").startCollecting()
        runCurrent()

        assertNull(store.getOrNull("k1"))
    }

    @Test
    fun `GIVEN failed keyed store WHEN failureOrNull by key THEN exception is returned`() = runFlowTest {
        val store = keyedStoreBuilder().build { throw exception }
        store.observe("k1").startCollecting()
        runCurrent()

        assertSame(exception, store.failureOrNull("k1"))
    }

    @Test
    fun `GIVEN loaded keyed store WHEN failureOrNull by key THEN null is returned`() = runFlowTest {
        val store = keyedStoreBuilder().build { key -> "value-$key" }
        store.observe("k1").startCollecting()
        runCurrent()

        assertNull(store.failureOrNull("k1"))
    }
}
