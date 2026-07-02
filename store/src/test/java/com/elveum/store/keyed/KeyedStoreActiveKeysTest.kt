package com.elveum.store.keyed

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class KeyedStoreActiveKeysTest : AbstractKeyedStoreTest() {

    @Test
    fun `GIVEN keyed store WHEN keys are observed and released THEN activeKeys reflects the active set`() = runFlowTest {
        val store = storeBuilder()
            .setInMemoryCacheTimeout(1.seconds)
            .build { key -> "value-$key" }

        assertEquals(emptySet<String>(), store.activeKeys.value)

        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()
        assertEquals(setOf("k1", "k2"), store.activeKeys.value)

        collector1.cancel()
        advanceTimeBy(1001) // k1 cache expires after its last observer leaves
        assertEquals(setOf("k2"), store.activeKeys.value)
    }
}
