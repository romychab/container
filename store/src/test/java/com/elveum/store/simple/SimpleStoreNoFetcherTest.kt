package com.elveum.store.simple

import com.elveum.store.contracts.SimpleQueryReactiveNoFetcherContract
import com.elveum.store.contracts.SimpleReactiveNoFetcherContract
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class SimpleStoreNoFetcherTest : AbstractSimpleStoreTest() {

    @Test
    fun `GIVEN no-fetcher store WHEN observe THEN emit the value from the local flow`() = runFlowTest {
        val storageFlow = MutableStateFlow("local")
        val store = storeBuilder()
            .disableFetcher()
            .build(onObserve = { storageFlow })

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("local"), collector.lastItem)
    }

    @Test
    fun `GIVEN no-fetcher store WHEN local flow emits a new value THEN observers receive it`() = runFlowTest {
        val storageFlow = MutableStateFlow("first")
        val store = storeBuilder()
            .disableFetcher()
            .build(onObserve = { storageFlow })
        val collector = store.observe().startCollecting()
        runCurrent()
        assertResult(StoreResult.Loaded("first"), collector.lastItem)

        storageFlow.value = "second"
        runCurrent()

        assertResult(StoreResult.Loaded("second"), collector.lastItem)
    }

    @Test
    fun `GIVEN no-fetcher store built from a contract WHEN observe THEN emit the value from the contract flow`() = runFlowTest {
        val storageFlow = MutableStateFlow("contract-value")
        val contract = object : SimpleReactiveNoFetcherContract<String> {
            override fun observe(): Flow<String> = storageFlow
        }
        val store = storeBuilder()
            .disableFetcher()
            .build(contract)

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("contract-value"), collector.lastItem)
    }

    @Test
    fun `GIVEN no-fetcher query store WHEN observe THEN emit the value for the current query`() = runFlowTest {
        val store = storeBuilder()
            .disableFetcher()
            .withQuery(initialQuery = "q1")
            .build(onObserve = { query -> MutableStateFlow("value-$query") })

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-q1"), collector.lastItem)
    }

    @Test
    fun `GIVEN no-fetcher query store WHEN query changes THEN the new query value is observed`() = runFlowTest {
        val store = storeBuilder()
            .disableFetcher()
            .withQuery(initialQuery = "q1")
            .build(onObserve = { query -> MutableStateFlow("value-$query") })
        val collector = store.observe().startCollecting()
        runCurrent()
        assertResult(StoreResult.Loaded("value-q1"), collector.lastItem)

        store.submitQueryAsync("q2")
        runCurrent()

        assertResult(StoreResult.Loaded("value-q2"), collector.lastItem)
    }

    @Test
    fun `GIVEN no-fetcher query store built from a contract WHEN observe THEN emit the value for the query`() = runFlowTest {
        val contract = object : SimpleQueryReactiveNoFetcherContract<String, String> {
            override fun observe(query: String): Flow<String> = MutableStateFlow("value-$query")
        }
        val store = storeBuilder()
            .disableFetcher()
            .withQuery(initialQuery = "q1")
            .build(contract)

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-q1"), collector.lastItem)
    }
}
