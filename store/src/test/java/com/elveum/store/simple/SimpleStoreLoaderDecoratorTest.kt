package com.elveum.store.simple

import com.elveum.container.subject.transformation.LoaderDecorator
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import org.junit.Assert.assertEquals
import org.junit.Test

class SimpleStoreLoaderDecoratorTest : AbstractSimpleStoreTest() {

    @Test
    fun `GIVEN decorator completing with cache clean up WHEN observe THEN emit loading result`() = runFlowTest {
        val store = storeBuilder()
            .setLoaderDecorator(LoaderDecorator { completeWithCacheCleanUp() })
            .build { "value" }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loading, collector.lastItem)
    }

    @Test
    fun `GIVEN decorator completing with cache clean up WHEN observe THEN fetch is not executed`() = runFlowTest {
        var fetchCount = 0
        val store = storeBuilder()
            .setLoaderDecorator(LoaderDecorator { completeWithCacheCleanUp() })
            .build { fetchCount++; "value" }

        store.observe().startCollecting()
        runCurrent()

        assertEquals(0, fetchCount)
    }

    @Test
    fun `GIVEN loaded value WHEN decorator completes with cache clean up THEN cached value is dropped`() = runFlowTest {
        var terminate = false
        val store = storeBuilder()
            .setLoaderDecorator(LoaderDecorator { originLoader ->
                if (terminate) completeWithCacheCleanUp() else originLoader()
            })
            .build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()
        assertResult(StoreResult.Loaded("value"), collector.lastItem)

        terminate = true
        store.invalidate()
        runCurrent()

        assertResult(StoreResult.Loading, collector.lastItem)
    }

    @Test
    fun `GIVEN keepContentOnLoadAndError request WHEN decorator completes with failure THEN emit failed result`() = runFlowTest {
        val expectedException = IllegalStateException("terminated")
        var terminate = false
        val store = storeBuilder()
            .setLoadRequest(silentErrorsRequest())
            .setLoaderDecorator(LoaderDecorator { originLoader ->
                if (terminate) completeWithFailure(expectedException) else originLoader()
            })
            .build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()
        assertResult(StoreResult.Loaded("value"), collector.lastItem)

        terminate = true
        // invalidate() rethrows the load failure, so the async form is used here
        store.invalidateAsync()
        runCurrent()

        assertResult(StoreResult.Failed(expectedException), collector.lastItem)
    }

    @Test
    fun `GIVEN keepContentOnLoadAndError request WHEN decorator throws THEN cached value is kept`() = runFlowTest {
        // contrast with completeWithFailure: a regular failure honours the silent policy
        var fail = false
        val store = storeBuilder()
            .setLoadRequest(silentErrorsRequest())
            .setLoaderDecorator(LoaderDecorator { originLoader ->
                if (fail) throw IllegalStateException("failed") else originLoader()
            })
            .build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()

        fail = true
        store.invalidateAsync()
        runCurrent()

        assertResult(StoreResult.Loaded("value"), collector.lastItem)
    }

    // LoadRequest.Silent keeps content while loading only; keeping it on errors
    // as well requires keepContentOnLoadAndError()
    private fun silentErrorsRequest(): LoadRequest = LoadRequest.builder()
        .keepContentOnLoadAndError()
        .build()

}
