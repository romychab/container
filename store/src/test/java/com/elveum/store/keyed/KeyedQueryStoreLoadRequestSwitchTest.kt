package com.elveum.store.keyed

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration

class KeyedQueryStoreLoadRequestSwitchTest : AbstractKeyedStoreTest() {

    @Test
    fun `GIVEN zero cache timeout WHEN load request changes THEN each key keeps its own query`() = runFlowTest {
        val loadRequestFlow = MutableStateFlow(LoadRequest.Default)
        val store = storeBuilder()
            .setCoroutineScopeFactory(createEagerStoreScopeFactory())
            .setInMemoryCacheTimeout(Duration.ZERO)
            .setLoadRequest(loadRequestFlow)
            .withQuery(initialQuery = "q1")
            .build { key, query -> "$key-$query" }
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()
        store.submitQueryAsync("k1", "q2")
        runCurrent()

        loadRequestFlow.value = LoadRequest.Silent
        runCurrent()

        assertEquals("q2", store.observeQueryFlow("k1").value)
        assertEquals("q1", store.observeQueryFlow("k2").value)
        assertResult(StoreResult.Loaded("k1-q2"), collector1.lastItem)
        assertResult(StoreResult.Loaded("k2-q1"), collector2.lastItem)
    }

    @Test
    fun `GIVEN zero cache timeout and single observed key WHEN load request changes THEN query is kept`() = runFlowTest {
        val loadRequestFlow = MutableStateFlow(LoadRequest.Default)
        val store = storeBuilder()
            .setCoroutineScopeFactory(createEagerStoreScopeFactory())
            .setInMemoryCacheTimeout(Duration.ZERO)
            .setLoadRequest(loadRequestFlow)
            .withQuery(initialQuery = "q1")
            .build { key, query -> "$key-$query" }
        val collector = store.observe("k1").startCollecting()
        runCurrent()
        store.submitQueryAsync("k1", "q2")
        runCurrent()

        loadRequestFlow.value = LoadRequest.Silent
        runCurrent()
        loadRequestFlow.value = LoadRequest.Default
        runCurrent()

        assertEquals("q2", store.observeQueryFlow("k1").value)
        assertResult(StoreResult.Loaded("k1-q2"), collector.lastItem)
    }

}
