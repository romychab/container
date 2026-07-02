package com.elveum.store.internal.stores.common

import com.elveum.container.Container
import com.elveum.container.ContainerValue
import com.elveum.container.LocalSourceType
import com.elveum.container.defaultMetadata
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreValueUpdaterTest {

    private val initialMetadata = defaultMetadata(sourceType = LocalSourceType)
    private var cached: ContainerValue<String>? = ContainerValue("old", initialMetadata)

    private val setToCache: (Container<String>) -> Unit = { container ->
        container as Container.Success
        cached = ContainerValue(container.value, container.metadata)
    }

    @Test
    fun `GIVEN no cached value WHEN optimistic update THEN updater is not invoked`() = runTest {
        cached = null
        var updaterCalled = false

        processOptimisticUpdate(
            getter = { cached },
            setToCache = setToCache,
            updater = { updaterCalled = true },
        )

        assertFalse(updaterCalled)
        assertEquals(null, cached)
    }

    @Test
    fun `GIVEN cached value WHEN optimistic update emits value THEN it is committed to the cache`() = runTest {
        processOptimisticUpdate(
            getter = { cached },
            setToCache = setToCache,
            updater = { emit("new") },
        )

        assertEquals("new", cached?.value)
    }

    @Test
    fun `GIVEN cached value WHEN optimistic update THEN updater receives the current value`() = runTest {
        var receivedValue: String? = null

        processOptimisticUpdate(
            getter = { cached },
            setToCache = setToCache,
            updater = { currentValue -> receivedValue = currentValue },
        )

        assertEquals("old", receivedValue)
    }

    @Test
    fun `GIVEN optimistic emit WHEN updater fails afterwards THEN cached value is rolled back`() = runTest {
        val exception = IllegalStateException("update failed")
        var thrown: Exception? = null

        try {
            processOptimisticUpdate(
                getter = { cached },
                setToCache = setToCache,
                updater = {
                    emit("new")
                    throw exception
                },
            )
        } catch (e: Exception) {
            thrown = e
        }

        assertSame(exception, thrown)
        assertEquals("old", cached?.value)
        assertEquals(initialMetadata, cached?.metadata)
    }

    @Test
    fun `GIVEN no optimistic emit WHEN updater fails THEN cached value is unchanged`() = runTest {
        val exception = IllegalStateException("update failed")
        var thrown: Exception? = null

        try {
            processOptimisticUpdate(
                getter = { cached },
                setToCache = setToCache,
                updater = { throw exception },
            )
        } catch (e: Exception) {
            thrown = e
        }

        assertSame(exception, thrown)
        assertEquals("old", cached?.value)
    }

    @Test
    fun `GIVEN successful optimistic update WHEN it completes THEN value is not rolled back`() = runTest {
        processOptimisticUpdate(
            getter = { cached },
            setToCache = setToCache,
            updater = { emit("new") },
        )

        assertTrue(cached?.value == "new")
    }
}
