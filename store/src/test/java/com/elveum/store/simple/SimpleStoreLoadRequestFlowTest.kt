package com.elveum.store.simple

import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.load.isBackgroundLoading
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleStoreLoadRequestFlowTest : AbstractSimpleStoreTest() {

    @Test
    fun `GIVEN store configured with a Silent load request flow WHEN invalidate THEN content is kept while reloading`() = runFlowTest {
        var counter = 0
        val store = storeBuilder()
            .setLoadRequest(flowOf(LoadRequest.Silent))
            .build {
                delay(10)
                "value${++counter}"
            }
        val collector = store.observe().startCollecting()
        advanceTimeBy(11) // first load

        store.invalidateAsync()

        advanceTimeBy(10) // reload in progress
        // the observer keeps the previous content because the configured default request is Silent
        assertResult(StoreResult.Loaded("value1"), collector.lastItem)
        assertTrue(collector.lastItem.isBackgroundLoading())

        advanceTimeBy(1)
        assertResult(StoreResult.Loaded("value2"), collector.lastItem)
    }
}
