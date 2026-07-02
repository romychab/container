package com.elveum.store.keyed

import com.elveum.store.contracts.SimpleKeyedReactiveNoFetcherContract
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class KeyedStoreNoFetcherTest : AbstractKeyedStoreTest() {

    @Test
    fun `GIVEN no-fetcher keyed store WHEN observe a key THEN emit the value from the local flow`() = runFlowTest {
        val store = storeBuilder()
            .disableFetcher()
            .build(onObserve = { key -> MutableStateFlow("value-$key") })

        val collector = store.observe("k1").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-k1"), collector.lastItem)
    }

    @Test
    fun `GIVEN no-fetcher keyed store WHEN local flow emits a new value THEN observers of that key receive it`() = runFlowTest {
        val storageFlows = mutableMapOf<String, MutableStateFlow<String>>()
        fun storageFlow(key: String) = storageFlows.getOrPut(key) { MutableStateFlow("initial-$key") }
        val store = storeBuilder()
            .disableFetcher()
            .build(onObserve = { key -> storageFlow(key) })
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()
        assertResult(StoreResult.Loaded("initial-k1"), collector1.lastItem)

        storageFlow("k1").value = "updated-k1"
        runCurrent()

        assertResult(StoreResult.Loaded("updated-k1"), collector1.lastItem)
        // observers of other keys are not affected
        assertResult(StoreResult.Loaded("initial-k2"), collector2.lastItem)
    }

    @Test
    fun `GIVEN no-fetcher keyed store built from a contract WHEN observe a key THEN emit the value from the contract flow`() = runFlowTest {
        val contract = object : SimpleKeyedReactiveNoFetcherContract<String, String> {
            override fun observe(key: String): Flow<String> = MutableStateFlow("value-$key")
        }
        val store = storeBuilder()
            .disableFetcher()
            .build(contract)

        val collector = store.observe("k1").startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("value-k1"), collector.lastItem)
    }
}
