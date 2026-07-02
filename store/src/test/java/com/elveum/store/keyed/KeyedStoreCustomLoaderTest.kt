package com.elveum.store.keyed

import com.elveum.store.StoreFactory
import com.elveum.store.base.AbstractStoreTest
import com.elveum.store.load.StoreResult
import org.junit.Test

class KeyedStoreCustomLoaderTest : AbstractStoreTest() {

    private fun keyedBuilder() = StoreFactory
        .simpleStoreBuilder<String>()
        .withKeys<String>()
        .setCoroutineScopeFactory(createStoreScopeFactory())

    private fun pagedKeyedBuilder() = StoreFactory
        .pagedStoreBuilder<Int, String>(initialKey = 0, itemId = { it })
        .setCoroutineScopeFactory(createStoreScopeFactory())
        .setFetchDistance(1)

    @Test
    fun `GIVEN keyed custom loader WHEN observe a key THEN the key reaches the loader`() = runFlowTest {
        val store = keyedBuilder().buildCustom { key -> emit("value-$key", isLastValue = true) }

        val collector = store.observe("k1").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-k1"), collector.lastItem)
    }

    @Test
    fun `GIVEN keyed query custom loader WHEN observe a key THEN both key and query reach the loader`() = runFlowTest {
        val store = keyedBuilder()
            .withQuery(initialQuery = "q1")
            .buildCustom { key, query -> emit("$key-$query", isLastValue = true) }

        val collector = store.observe("k1").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("k1-q1"), collector.lastItem)
    }

    @Test
    fun `GIVEN paged keyed custom loader WHEN observe a key THEN key and page key reach the loader`() = runFlowTest {
        val store = pagedKeyedBuilder()
            .withKeys<String>()
            .buildCustom { key, pageKey -> emitPage(listOf("$key-page$pageKey")) }

        val collector = store.observe("k1").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("k1-page0")), collector.lastItem)
    }

    @Test
    fun `GIVEN paged keyed query custom loader WHEN observe a key THEN key, query and page key reach the loader`() = runFlowTest {
        val store = pagedKeyedBuilder()
            .withQuery(initialQuery = "q1")
            .withKeys<String>()
            .buildCustom { key, query, pageKey -> emitPage(listOf("$key-$query-page$pageKey")) }

        val collector = store.observe("k1").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("k1-q1-page0")), collector.lastItem)
    }
}
