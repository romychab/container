package com.elveum.store.simple

import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onSubscription
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SimpleExternalQueryStoreTest : AbstractSimpleStoreTest() {

    @Test
    fun `GIVEN imperative query store WHEN observed THEN loads with initial query`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query -> "value-$query" }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-q1"), collector.lastItem)
        assertEquals("q1", store.queryFlow.value)
    }

    @Test
    fun `GIVEN external StateFlow query WHEN observed THEN loads with the flow's current value`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val store = storeBuilder()
            .withQuery { query }
            .build { q -> "value-$q" }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-q1"), collector.lastItem)
    }

    @Test
    fun `GIVEN observed external-query store WHEN flow emits new query THEN data is re-fetched`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val store = storeBuilder()
            .withQuery { query }
            .build { q -> "value-$q" }
        val collector = store.observe().startCollecting()
        runCurrent()

        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded("value-q2"), collector.lastItem)
    }

    @Test
    fun `GIVEN external StateFlow whose value equals seed WHEN observed THEN fetcher runs exactly once`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val fetchedQueries = mutableListOf<String>()
        val store = storeBuilder()
            .withQuery { query }
            .build { q -> fetchedQueries += q; "value-$q" }

        store.observe().startCollecting()
        runCurrent()

        assertEquals(listOf("q1"), fetchedQueries) // no double initial load
    }

    @Test
    fun `GIVEN primary overload with explicit initial query WHEN flow later emits THEN first load uses initial then reloads`() = runFlowTest {
        val query = MutableSharedFlow<String>() // cold-ish: no initial value
        val fetchedQueries = mutableListOf<String>()
        val store = storeBuilder()
            .withQuery(initialQuery = "seed") { query }
            .build { q -> fetchedQueries += q; "value-$q" }
        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-seed"), collector.lastItem)

        query.emit("q2")
        runCurrent()

        assertResult(StoreResult.Loaded("value-q2"), collector.lastItem)
        assertEquals(listOf("seed", "q2"), fetchedQueries)
    }

    @Test
    fun `GIVEN external query with debounce WHEN flow emits quickly THEN only last query is fetched`() = runFlowTest {
        val query = MutableStateFlow("initial")
        val fetchedQueries = mutableListOf<String>()
        val store = storeBuilder()
            .withQuery(debounceMillis = 100) { query }
            .build { q -> fetchedQueries += q; "value-$q" }
        val collector = store.observe().startCollecting()
        runCurrent()

        query.value = "a"
        advanceTimeBy(50)
        query.value = "ab"
        advanceTimeBy(101)

        assertResult(StoreResult.Loaded("value-ab"), collector.lastItem)
        assertEquals(listOf("initial", "ab"), fetchedQueries)
    }

    @Test
    fun `GIVEN external-query suspending store WHEN flow emits THEN reloads via remote and saves to storage`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val saved = mutableListOf<Pair<String, String>>()
        val store = storeBuilder()
            .addSuspendingLocalStorage()
            .withQuery { query }
            .build(
                onFetch = { q -> "value-$q" },
                onSaveToStorage = { q, v -> saved += q to v },
                onLoadFromStorage = { null },
            )
        val collector = store.observe().startCollecting()
        runCurrent()
        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded("value-q2"), collector.lastItem)
        assertEquals(listOf("q1" to "value-q1", "q2" to "value-q2"), saved)
    }

    @Test
    fun `GIVEN external-query reactive store WHEN flow emits THEN reloads and observes storage for the new query`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val storages = mapOf(
            "q1" to MutableStateFlow<String?>(null),
            "q2" to MutableStateFlow<String?>(null),
        )
        val store = storeBuilder()
            .addReactiveLocalStorage()
            .withQuery { query }
            .build(
                onFetch = { q -> "remote-$q" },
                onSaveToStorage = { q, v -> storages.getValue(q).value = v },
                onObserveStorage = { q -> storages.getValue(q) },
            )
        val collector = store.observe().startCollecting()
        runCurrent()
        assertResult(StoreResult.Loaded("remote-q1"), collector.lastItem)

        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded("remote-q2"), collector.lastItem)
        // a later reactive write to the q2 storage propagates automatically
        storages.getValue("q2").value = "external-q2"
        runCurrent()
        assertResult(StoreResult.Loaded("external-q2"), collector.lastItem)
    }

    @Test
    fun `GIVEN external-query no-fetcher store WHEN flow emits THEN observes the new query's local flow`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val store = storeBuilder()
            .disableFetcher()
            .withQuery { query }
            .build(onObserve = { q -> flowOf("local-$q") })
        val collector = store.observe().startCollecting()
        runCurrent()
        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded("local-q2"), collector.lastItem)
    }

    @Test
    fun `GIVEN withQuery BEFORE addSuspendingLocalStorage WHEN built THEN store works and saves per query`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val saved = mutableListOf<Pair<String, String>>()
        val store = storeBuilder()
            .withQuery { query }
            .addSuspendingLocalStorage()
            .build(
                onFetch = { q -> "value-$q" },
                onSaveToStorage = { q, v -> saved += q to v },
                onLoadFromStorage = { null },
            )
        val collector = store.observe().startCollecting()
        runCurrent()
        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded("value-q2"), collector.lastItem)
        assertEquals(listOf("q1" to "value-q1", "q2" to "value-q2"), saved)
    }

    @Test
    fun `GIVEN withQuery BEFORE addReactiveLocalStorage WHEN built THEN store works`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val storages = mapOf("q1" to MutableStateFlow<String?>(null), "q2" to MutableStateFlow<String?>(null))
        val store = storeBuilder()
            .withQuery { query }
            .addReactiveLocalStorage()
            .build(
                onFetch = { q -> "remote-$q" },
                onSaveToStorage = { q, v -> storages.getValue(q).value = v },
                onObserveStorage = { q -> storages.getValue(q) },
            )
        val collector = store.observe().startCollecting()
        runCurrent()
        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded("remote-q2"), collector.lastItem)
    }

    @Test
    fun `GIVEN withQuery BEFORE disableFetcher WHEN built THEN store observes local data`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val store = storeBuilder()
            .withQuery { query }
            .disableFetcher()
            .build(onObserve = { q -> flowOf("local-$q") })
        val collector = store.observe().startCollecting()
        runCurrent()
        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded("local-q2"), collector.lastItem)
    }

    @Test
    fun `GIVEN withQuery BEFORE withKeys WHEN built THEN all keys share the same query flow`() = runFlowTest {
        val query = MutableStateFlow("a")
        val store = storeBuilder()
            .withQuery { query }
            .withKeys<String>()
            .build { key, q -> "$key-$q" }
        val c1 = store.observe("k1").startCollecting()
        val c2 = store.observe("k2").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("k1-a"), c1.lastItem)
        assertResult(StoreResult.Loaded("k2-a"), c2.lastItem)

        query.value = "b" // shared flow: every key reloads
        runCurrent()

        assertResult(StoreResult.Loaded("k1-b"), c1.lastItem)
        assertResult(StoreResult.Loaded("k2-b"), c2.lastItem)
    }

    @Test
    fun `GIVEN external query store WHEN inactive THEN query flow is collected only while active`() = runFlowTest {
        var subscriptions = 0
        val query = MutableStateFlow("q1")
        val trackedFlow = query.onSubscription { subscriptions++ }
        val store = storeBuilder()
            .setInMemoryCacheTimeout(1.seconds)
            .withQuery(initialQuery = "q1") { trackedFlow }
            .build { q -> "value-$q" }

        assertEquals(0, subscriptions) // nothing collected before observing

        val collector = store.observe().startCollecting()
        runCurrent()
        assertEquals(1, subscriptions) // collected once active

        collector.cancel()
        advanceTimeBy(1001) // pass the in-memory cache timeout -> store becomes inactive
        runCurrent()

        query.value = "q2" // an emission while inactive must not trigger any work
        runCurrent()

        assertEquals(1, subscriptions) // still exactly one subscription; no leak, no re-collect
    }
}
