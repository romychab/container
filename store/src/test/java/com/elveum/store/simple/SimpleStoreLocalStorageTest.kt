package com.elveum.store.simple

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

class SimpleStoreLocalStorageTest : AbstractSimpleStoreTest() {

    @Test
    fun `GIVEN suspending storage with data WHEN observe THEN emit local value first and then remote one`() = runFlowTest {
        val store = storeBuilder()
            .addSuspendingLocalStorage()
            .build(
                onFetch = {
                    delay(10)
                    "remote"
                },
                onSaveToStorage = { },
                onLoadFromStorage = { "local" },
            )

        val collector = store.observe().startCollecting()
        runCurrent()

        // the local value is emitted first, while the remote load is in progress
        assertResult(StoreResult.Loaded("local"), collector.lastItem)
        assertEquals(LocalSourceType, collector.lastItem.sourceType)

        advanceTimeBy(11)
        // then the remote value replaces the local one
        assertResult(StoreResult.Loaded("remote"), collector.lastItem)
        assertEquals(RemoteSourceType, collector.lastItem.sourceType)
    }

    @Test
    fun `GIVEN suspending storage without data WHEN observe THEN emit only remote value`() = runFlowTest {
        val store = storeBuilder()
            .addSuspendingLocalStorage()
            .build(
                onFetch = { "remote" },
                onSaveToStorage = { },
                onLoadFromStorage = { null },
            )

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("remote"), collector.lastItem)
        assertEquals(RemoteSourceType, collector.lastItem.sourceType)
    }

    @Test
    fun `GIVEN suspending storage WHEN remote value is fetched THEN it is saved to storage`() = runFlowTest {
        var savedValue: String? = null
        val store = storeBuilder()
            .addSuspendingLocalStorage()
            .build(
                onFetch = { "remote" },
                onSaveToStorage = { savedValue = it },
                onLoadFromStorage = { null },
            )

        store.observe().startCollecting()
        runCurrent()

        assertEquals("remote", savedValue)
    }

    @Test
    fun `GIVEN reactive storage WHEN storage emits new value THEN observers receive it automatically`() = runFlowTest {
        val storageFlow = MutableStateFlow<String?>(null)
        val store = storeBuilder()
            .addReactiveLocalStorage()
            .build(
                onFetch = { "remote" },
                onSaveToStorage = { storageFlow.value = it },
                onObserveStorage = { storageFlow },
            )
        val collector = store.observe().startCollecting()
        runCurrent()
        assertResult(StoreResult.Loaded("remote"), collector.lastItem)

        // an external write to the local storage
        storageFlow.value = "external-update"
        runCurrent()

        assertResult(StoreResult.Loaded("external-update"), collector.lastItem)
    }

    @Test
    fun `GIVEN non-empty cache WHEN observe in offline mode THEN emit only cached value`() = runFlowTest {
        val store = storeBuilder()
            .addSuspendingLocalStorage()
            .build(
                onFetch = { "remote" },
                onSaveToStorage = {},
                onLoadFromStorage = { "stored" }
            )
        val offlineRequest = LoadRequest.builder().offlineMode().build()

        val collector = store.observe(offlineRequest).startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded("stored"), collector.lastItem)
        assertEquals(LocalSourceType, collector.lastItem.sourceType)
    }

}
