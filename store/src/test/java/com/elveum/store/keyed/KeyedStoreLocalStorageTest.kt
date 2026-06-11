package com.elveum.store.keyed

import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.store.exceptions.NoCachedDataException
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.load.isFailed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class KeyedStoreLocalStorageTest : AbstractKeyedStoreTest() {

    @Test
    fun `GIVEN suspending storage with data WHEN observe key THEN emit local value first and then remote one`() = runFlowTest {
        val store = storeBuilder()
            .addSuspendingLocalStorage()
            .build(
                onFetch = { key ->
                    delay(10)
                    "remote-$key"
                },
                onSaveToStorage = { _, _ -> },
                onLoadFromStorage = { key -> "local-$key" },
            )

        val collector = store.observe("k1").startCollecting()
        runCurrent()

        // the local value is emitted first, while the remote load is in progress
        assertResult(StoreResult.Loaded("local-k1"), collector.lastItem)
        assertEquals(LocalSourceType, collector.lastItem.sourceType)

        advanceTimeBy(11)
        // then the remote value replaces the local one
        assertResult(StoreResult.Loaded("remote-k1"), collector.lastItem)
        assertEquals(RemoteSourceType, collector.lastItem.sourceType)
    }

    @Test
    fun `GIVEN suspending storage WHEN remote value is fetched THEN it is saved to storage with its key`() = runFlowTest {
        val savedValues = mutableMapOf<String, String>()
        val store = storeBuilder()
            .addSuspendingLocalStorage()
            .build(
                onFetch = { key -> "remote-$key" },
                onSaveToStorage = { key, value -> savedValues[key] = value },
                onLoadFromStorage = { null },
            )

        store.observe("k1").startCollecting()
        runCurrent()

        assertEquals(mapOf("k1" to "remote-k1"), savedValues)
    }

    @Test
    fun `GIVEN reactive storage WHEN storage emits new value for a key THEN observers of that key receive it`() = runFlowTest {
        val storageFlows = mutableMapOf<String, MutableStateFlow<String?>>()
        fun storageFlow(key: String) = storageFlows.getOrPut(key) { MutableStateFlow(null) }
        val store = storeBuilder()
            .addReactiveLocalStorage()
            .build(
                onFetch = { key -> "remote-$key" },
                onSaveToStorage = { key, value -> storageFlow(key).value = value },
                onObserveStorage = { key -> storageFlow(key) },
            )
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()
        assertResult(StoreResult.Loaded("remote-k1"), collector1.lastItem)

        // an external write to the local storage of "k1"
        storageFlow("k1").value = "external-update"
        runCurrent()

        assertResult(StoreResult.Loaded("external-update"), collector1.lastItem)
        // observers of other keys are not affected
        assertResult(StoreResult.Loaded("remote-k2"), collector2.lastItem)
    }

    @Test
    fun `GIVEN empty cache WHEN observe in offline mode THEN emit no-cached-data error`() = runFlowTest {
        val store = storeBuilder().build { "value-$it" }
        val offlineRequest = LoadRequest.builder().offlineMode().build()

        val collector = store.observe("key", offlineRequest).startCollecting()
        runCurrent()

        val lastItem = collector.lastItem
        assertTrue(lastItem.isFailed())
        assertTrue((lastItem as StoreResult.Failed).exception is NoCachedDataException)
    }

    @Test
    fun `GIVEN non-empty cache WHEN observe in offline mode THEN emit only cached value`() = runFlowTest {
        val store = storeBuilder()
            .addSuspendingLocalStorage()
            .build(
                onFetch = { "remote-$it" },
                onSaveToStorage = { _, _ -> },
                onLoadFromStorage = { "stored-$it" }
            )
        val offlineRequest = LoadRequest.builder().offlineMode().build()

        val collector = store.observe("key", offlineRequest).startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("stored-key"), collector.lastItem)
        assertEquals(LocalSourceType, collector.lastItem.sourceType)
    }

    @Test
    fun `GIVEN offline request after expired fresh THEN do not fetch data from remote`() = runFlowTest {
        var fetchSeq = 0
        var savedValue: String? = null
        val store = storeBuilder()
            .addSuspendingLocalStorage()
            .setInMemoryCacheTimeout(10.milliseconds)
            .build(
                onFetch = { "remote-$it-${++fetchSeq}" },
                onSaveToStorage = { _, value -> savedValue = value },
                onLoadFromStorage = { savedValue },
            )
        val collector1 = store.observe("key", LoadRequest.Default).startCollecting()
        runCurrent()
        collector1.cancel()
        advanceTimeBy(11)

        val offlineRequest = LoadRequest.builder().offlineMode().build()
        val collector2 = store.observe("key", offlineRequest).startCollecting()
        runCurrent()

        assertEquals(2, collector2.count)
        assertResult(StoreResult.Loading, collector1.collectedItems[0])
        assertResult(StoreResult.Loaded("remote-key-1"), collector1.collectedItems[1])
    }

    @Test
    fun `GIVEN offline request after fresh request THEN do not fetch data from remote`() = runFlowTest {
        var fetchSeq = 0
        var savedValue: String? = null
        val store = storeBuilder()
            .addSuspendingLocalStorage()
            .build(
                onFetch = { "remote-$it-${++fetchSeq}" },
                onSaveToStorage = { _, value -> savedValue = value },
                onLoadFromStorage = { savedValue },
            )
        val collector1 = store.observe("key", LoadRequest.Default).startCollecting()
        runCurrent()

        val offlineRequest = LoadRequest.builder().offlineMode().build()
        val collector2 = store.observe("key", offlineRequest).startCollecting()
        runCurrent()

        assertEquals(2, collector2.count)
        assertResult(StoreResult.Loading, collector1.collectedItems[0])
        assertResult(StoreResult.Loaded("remote-key-1"), collector1.collectedItems[1])
    }

}
