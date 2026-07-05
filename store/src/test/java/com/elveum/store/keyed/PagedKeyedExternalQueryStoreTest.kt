package com.elveum.store.keyed

import com.elveum.store.StoreFactory
import com.elveum.store.base.AbstractStoreTest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.keyed.PagedKeyedStore
import com.elveum.store.stores.paged.PagedList
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class PagedKeyedExternalQueryStoreTest : AbstractStoreTest() {

    private fun store(queries: Map<String, MutableStateFlow<String>>): PagedKeyedStore<String, String> = StoreFactory
        .pagedStoreBuilder<Int, String>(initialKey = 0, itemId = { it })
        .setCoroutineScopeFactory(createStoreScopeFactory())
        .setFetchDistance(1)
        .withKeys<String>()
        .withQuery { key -> queries.getValue(key) }
        .build { key, query, pageKey -> keyedQueryPage(key, query, pageKey, totalPages = 2) }

    private fun keyedQueryPage(
        key: String,
        query: String,
        pageKey: Int,
        totalPages: Int,
    ): PagedList<Int, String> {
        val items = listOf(
            "$key-$query-item${pageKey * 2}",
            "$key-$query-item${pageKey * 2 + 1}",
        )
        val nextKey = (pageKey + 1).takeIf { it < totalPages }
        return PagedList(items, nextKey)
    }

    @Test
    fun `GIVEN paged keyed external query WHEN each key observed THEN each key follows its own query flow`() = runFlowTest {
        val queries = mapOf("k1" to MutableStateFlow("a"), "k2" to MutableStateFlow("b"))
        val store = store(queries)

        val c1 = store.observe("k1").startCollecting()
        val c2 = store.observe("k2").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("k1-a-item0", "k1-a-item1")), c1.lastItem)
        assertResult(StoreResult.Loaded(listOf("k2-b-item0", "k2-b-item1")), c2.lastItem)

        queries.getValue("k1").value = "a2"
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("k1-a2-item0", "k1-a2-item1")), c1.lastItem)
        assertResult(StoreResult.Loaded(listOf("k2-b-item0", "k2-b-item1")), c2.lastItem) // k2 unaffected
    }

    @Test
    fun `GIVEN paginated key WHEN its query flow emits THEN only that key's pagination resets`() = runFlowTest {
        val queries = mapOf("k1" to MutableStateFlow("q1"), "k2" to MutableStateFlow("q1"))
        val store = store(queries)
        val c1 = store.observe("k1").startCollecting()
        val c2 = store.observe("k2").startCollecting()
        runCurrent()

        // advance k1 to its second page
        store.onItemRendered("k1", 1)
        runCurrent()
        assertResult(
            StoreResult.Loaded(listOf("k1-q1-item0", "k1-q1-item1", "k1-q1-item2", "k1-q1-item3")),
            c1.lastItem,
        )

        queries.getValue("k1").value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("k1-q2-item0", "k1-q2-item1")), c1.lastItem)
        // the other key is untouched: same query, same accumulated pages
        assertResult(StoreResult.Loaded(listOf("k2-q1-item0", "k2-q1-item1")), c2.lastItem)
    }

    @Test
    fun `GIVEN paged keyed external query suspending store WHEN flow emits THEN reloads and saves the first page per key`() = runFlowTest {
        val query = MutableStateFlow("q1")
        val saved = mutableListOf<Triple<String, String, Int>>()
        val store = StoreFactory
            .pagedStoreBuilder<Int, String>(initialKey = 0, itemId = { it })
            .setCoroutineScopeFactory(createStoreScopeFactory())
            .setFetchDistance(1)
            .withKeys<String>()
            .addSuspendingLocalStorage()
            .withQuery { query }
            .build(
                onFetch = { key, q, pageKey -> keyedQueryPage(key, q, pageKey, totalPages = 2) },
                onSaveToStorage = { key, q, pageKey, _ -> saved += Triple(key, q, pageKey) },
                onLoadFromStorage = { _, _, _ -> null },
            )
        val collector = store.observe("k1").startCollecting()
        runCurrent()
        query.value = "q2"
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("k1-q2-item0", "k1-q2-item1")), collector.lastItem)
        assertEquals(Triple("k1", "q2", 0), saved.last())
    }
}
