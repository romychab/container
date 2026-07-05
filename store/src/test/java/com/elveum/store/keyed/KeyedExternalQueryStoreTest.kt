package com.elveum.store.keyed

import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyedExternalQueryStoreTest : AbstractKeyedStoreTest() {

    @Test
    fun `GIVEN keyed external query WHEN each key observed THEN each key follows its own query flow`() = runFlowTest {
        val queries = mapOf("k1" to MutableStateFlow("a"), "k2" to MutableStateFlow("b"))
        val store = storeBuilder()
            .withQuery { key -> queries.getValue(key) }
            .build { key, query -> "$key-$query" }

        val c1 = store.observe("k1").startCollecting()
        val c2 = store.observe("k2").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("k1-a"), c1.lastItem)
        assertResult(StoreResult.Loaded("k2-b"), c2.lastItem)

        queries.getValue("k1").value = "a2"
        runCurrent()

        assertResult(StoreResult.Loaded("k1-a2"), c1.lastItem)
        assertResult(StoreResult.Loaded("k2-b"), c2.lastItem) // key 2 unaffected
    }

    @Test
    fun `GIVEN keyed external query primary overload WHEN observed THEN first load uses initial query`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "seed") { _ -> MutableStateFlow("seed") }
            .build { key, query -> "$key-$query" }

        val c1 = store.observe("k1").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("k1-seed"), c1.lastItem)
    }

    @Test
    fun `GIVEN keyed external query suspending store WHEN flow emits THEN reloads and saves per key`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val saved = mutableListOf<Triple<String, String, String>>()
        val store = storeBuilder()
            .addSuspendingLocalStorage()
            .withQuery { query }
            .build(
                onFetch = { key, q -> "$key-$q" },
                onSaveToStorage = { key, q, v -> saved += Triple(key, q, v) },
                onLoadFromStorage = { _, _ -> null },
            )
        val collector = store.observe("k1").startCollecting()
        runCurrent()
        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded("k1-q2"), collector.lastItem)
        assertEquals(listOf(Triple("k1", "q1", "k1-q1"), Triple("k1", "q2", "k1-q2")), saved)
    }

    @Test
    fun `GIVEN keyed external query no-fetcher store WHEN flow emits THEN observes the new query's local flow`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val store = storeBuilder()
            .disableFetcher()
            .withQuery { query }
            .build(onObserve = { key, q -> flowOf("$key-$q") })
        val collector = store.observe("k1").startCollecting()
        runCurrent()
        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded("k1-q2"), collector.lastItem)
    }

    @Test
    fun `GIVEN keyed withQuery BEFORE addSuspendingLocalStorage WHEN built THEN store works and saves per key`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val saved = mutableListOf<Triple<String, String, String>>()
        val store = storeBuilder()
            .withQuery { query }
            .addSuspendingLocalStorage()
            .build(
                onFetch = { key, q -> "$key-$q" },
                onSaveToStorage = { key, q, v -> saved += Triple(key, q, v) },
                onLoadFromStorage = { _, _ -> null },
            )
        val collector = store.observe("k1").startCollecting()
        runCurrent()
        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded("k1-q2"), collector.lastItem)
        assertEquals(Triple("k1", "q2", "k1-q2"), saved.last())
    }
}
