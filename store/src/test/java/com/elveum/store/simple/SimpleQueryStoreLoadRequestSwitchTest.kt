package com.elveum.store.simple

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration

class SimpleQueryStoreLoadRequestSwitchTest : AbstractSimpleStoreTest() {

    @Test
    fun `GIVEN zero cache timeout WHEN load request changes THEN submitted query is kept`() = runFlowTest {
        val fetchedQueries = mutableListOf<String>()
        val loadRequestFlow = MutableStateFlow(LoadRequest.Default)
        val store = storeBuilder()
            .setCoroutineScopeFactory(createEagerStoreScopeFactory())
            .setInMemoryCacheTimeout(Duration.ZERO)
            .setLoadRequest(loadRequestFlow)
            .withQuery(initialQuery = "q1")
            .build { query ->
                fetchedQueries += query
                "value-$query"
            }
        val collector = store.observe().startCollecting()
        runCurrent()
        store.submitQueryAsync("q2")
        runCurrent()

        loadRequestFlow.value = LoadRequest.Silent
        runCurrent()

        assertEquals("q2", store.queryFlow.value)
        assertResult(StoreResult.Loaded("value-q2"), collector.lastItem)
        assertEquals(listOf("q1", "q2", "q2"), fetchedQueries)
    }

    @Test
    fun `GIVEN zero cache timeout WHEN observed again after last observer stops THEN initial query is used`() = runFlowTest {
        val store = storeBuilder()
            .setCoroutineScopeFactory(createEagerStoreScopeFactory())
            .setInMemoryCacheTimeout(Duration.ZERO)
            .withQuery(initialQuery = "q1")
            .build { query -> "value-$query" }
        val collector = store.observe().startCollecting()
        runCurrent()
        store.submitQueryAsync("q2")
        runCurrent()

        collector.cancel()
        runCurrent()
        val newCollector = store.observe().startCollecting()
        runCurrent()

        assertEquals("q1", store.queryFlow.value)
        assertResult(StoreResult.Loaded("value-q1"), newCollector.lastItem)
    }

}
