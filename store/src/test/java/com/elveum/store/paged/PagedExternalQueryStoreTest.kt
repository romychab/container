package com.elveum.store.paged

import com.elveum.store.load.StoreResult
import com.elveum.store.stores.paged.PagedList
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class PagedExternalQueryStoreTest : AbstractPagedStoreTest() {

    private fun queryPage(query: String, pageKey: Int, totalPages: Int): PagedList<Int, String> {
        val items = listOf("$query-item${pageKey * 2}", "$query-item${pageKey * 2 + 1}")
        val nextKey = (pageKey + 1).takeIf { it < totalPages }
        return PagedList(items, nextKey)
    }

    @Test
    fun `GIVEN paged external StateFlow query WHEN observed THEN first load uses the flow's value`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val store = storeBuilder()
            .withQuery { query }
            .build { q, pageKey -> queryPage(q, pageKey, totalPages = 2) }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("q1-item0", "q1-item1")), collector.lastItem)
    }

    @Test
    fun `GIVEN two loaded pages WHEN flow emits new query THEN pagination resets and reloads first page`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val store = storeBuilder()
            .withQuery { query }
            .build { q, pageKey -> queryPage(q, pageKey, totalPages = 2) }
        val collector = store.observe().startCollecting()
        runCurrent()
        store.onItemRendered(1) // load the second page
        runCurrent()
        assertResult(
            StoreResult.Loaded(listOf("q1-item0", "q1-item1", "q1-item2", "q1-item3")),
            collector.lastItem,
        )

        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("q2-item0", "q2-item1")), collector.lastItem)
    }

    @Test
    fun `GIVEN external StateFlow whose value equals seed WHEN observed THEN fetcher runs once for the first page`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val fetchedQueries = mutableListOf<String>()
        val store = storeBuilder()
            .withQuery { query }
            .build { q, pageKey ->
                fetchedQueries += q
                queryPage(q, pageKey, totalPages = 2)
            }

        store.observe().startCollecting()
        runCurrent()

        assertEquals(listOf("q1"), fetchedQueries) // no double initial load
    }

    @Test
    fun `GIVEN paged external query with debounce WHEN flow emits quickly THEN only the last query is fetched`() = runFlowTest {
        val query = MutableStateFlow("initial")
        val fetchedQueries = mutableListOf<String>()
        val store = storeBuilder()
            .withQuery(debounceMillis = 100) { query }
            .build { q, pageKey ->
                fetchedQueries += q
                queryPage(q, pageKey, totalPages = 2)
            }
        val collector = store.observe().startCollecting()
        runCurrent()

        query.value = "a"
        advanceTimeBy(50)
        query.value = "ab"
        advanceTimeBy(101)

        assertResult(StoreResult.Loaded(listOf("ab-item0", "ab-item1")), collector.lastItem)
        assertEquals(listOf("initial", "ab"), fetchedQueries)
    }

    @Test
    fun `GIVEN paged external query suspending store WHEN flow emits THEN reloads and saves the first page`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val saved = mutableListOf<Pair<String, Int>>()
        val store = storeBuilder()
            .addSuspendingLocalStorage()
            .withQuery { query }
            .build(
                onFetch = { q, pageKey -> queryPage(q, pageKey, totalPages = 2) },
                onSaveToStorage = { q, pageKey, _ -> saved += q to pageKey },
                onLoadFromStorage = { _, _ -> null },
            )
        val collector = store.observe().startCollecting()
        runCurrent()
        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("q2-item0", "q2-item1")), collector.lastItem)
        assertEquals("q2" to 0, saved.last())
    }

    @Test
    fun `GIVEN paged withQuery BEFORE addSuspendingLocalStorage WHEN built THEN store works`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val store = storeBuilder()
            .withQuery { query }
            .addSuspendingLocalStorage()
            .build(
                onFetch = { q, pageKey -> queryPage(q, pageKey, totalPages = 2) },
                onSaveToStorage = { _, _, _ -> },
                onLoadFromStorage = { _, _ -> null },
            )
        val collector = store.observe().startCollecting()
        runCurrent()
        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("q2-item0", "q2-item1")), collector.lastItem)
    }

    @Test
    fun `GIVEN paged withQuery BEFORE withKeys WHEN built THEN keys share the query flow`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val store = storeBuilder()
            .withQuery { query }
            .withKeys<String>()
            .build { key, q, pageKey ->
                PagedList(
                    listOf("$key-$q-i${pageKey * 2}", "$key-$q-i${pageKey * 2 + 1}"),
                    (pageKey + 1).takeIf { it < 2 },
                )
            }
        val c1 = store.observe("k1").startCollecting()
        val c2 = store.observe("k2").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("k1-q1-i0", "k1-q1-i1")), c1.lastItem)
        assertResult(StoreResult.Loaded(listOf("k2-q1-i0", "k2-q1-i1")), c2.lastItem)

        query.value = "q2" // shared flow: every key resets and reloads
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("k1-q2-i0", "k1-q2-i1")), c1.lastItem)
        assertResult(StoreResult.Loaded(listOf("k2-q2-i0", "k2-q2-i1")), c2.lastItem)
    }
}
