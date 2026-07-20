package com.elveum.store.simple

import com.elveum.container.ContainerMetadata
import com.elveum.container.get
import com.elveum.store.load.StoreResult
import com.elveum.store.load.invalidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Covers the optional [ContainerMetadata] argument that can be attached to
 * invalidation requests of [com.elveum.store.stores.simple.SimpleStore] and
 * query requests of [com.elveum.store.stores.simple.SimpleQueryStore].
 */
class SimpleStoreMetadataTest : AbstractSimpleStoreTest() {

    @Test
    fun `GIVEN observed store WHEN invalidate with metadata THEN metadata is attached to the emitted result`() = runFlowTest {
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestMetadata>())

        store.invalidate(TestMetadata("m1"))
        runCurrent()

        assertResult(StoreResult.Loaded("value"), collector.lastItem)
        assertEquals("m1", collector.lastItem.metadata.get<TestMetadata>()?.value)
    }

    @Test
    fun `GIVEN observed store WHEN invalidateAsync with metadata THEN metadata is attached to the emitted result`() = runFlowTest {
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.invalidateAsync(TestMetadata("m1"))
        runCurrent()

        assertResult(StoreResult.Loaded("value"), collector.lastItem)
        assertEquals("m1", collector.lastItem.metadata.get<TestMetadata>()?.value)
    }

    @Test
    fun `GIVEN rendered result WHEN invalidate with metadata THEN metadata is attached to the emitted result`() = runFlowTest {
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestMetadata>())

        // reload straight from the rendered result, tagging the reload
        collector.lastItem.invalidate(metadata = TestMetadata("m1"))
        runCurrent()

        assertResult(StoreResult.Loaded("value"), collector.lastItem)
        assertEquals("m1", collector.lastItem.metadata.get<TestMetadata>()?.value)
    }

    @Test
    fun `GIVEN result invalidate with one-shot metadata WHEN invalidate again THEN one-shot metadata is not kept`() = runFlowTest {
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()

        collector.lastItem.invalidate(metadata = TestOneShotMetadata)
        runCurrent()
        assertNotNull(collector.lastItem.metadata.get<TestOneShotMetadata>())

        collector.lastItem.invalidate()
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    @Test
    fun `GIVEN observed store WHEN invalidate without metadata THEN no custom metadata is attached`() = runFlowTest {
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.invalidate()
        runCurrent()

        assertNull(collector.lastItem.metadata.get<TestMetadata>())
    }

    @Test
    fun `GIVEN observed query store WHEN submitQuery with metadata THEN metadata is attached to the emitted result`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query -> "value-$query" }
        val collector = store.observe().startCollecting()
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestMetadata>())

        store.submitQuery("q2", TestMetadata("m1"))
        runCurrent()

        assertResult(StoreResult.Loaded("value-q2"), collector.lastItem)
        assertEquals("m1", collector.lastItem.metadata.get<TestMetadata>()?.value)
    }

    @Test
    fun `GIVEN observed query store WHEN submitQueryAsync with metadata THEN metadata is attached to the emitted result`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query -> "value-$query" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.submitQueryAsync("q2", TestMetadata("m1"))
        runCurrent()

        assertResult(StoreResult.Loaded("value-q2"), collector.lastItem)
        assertEquals("m1", collector.lastItem.metadata.get<TestMetadata>()?.value)
    }

    @Test
    fun `GIVEN invalidate with one-shot metadata WHEN invalidate again THEN one-shot metadata is not kept`() = runFlowTest {
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.invalidate(TestOneShotMetadata)
        runCurrent()
        assertNotNull(collector.lastItem.metadata.get<TestOneShotMetadata>())

        store.invalidate()
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    @Test
    fun `GIVEN invalidateAsync with one-shot metadata WHEN invalidate again THEN one-shot metadata is not kept`() = runFlowTest {
        val store = storeBuilder().build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.invalidateAsync(TestOneShotMetadata)
        runCurrent()
        assertNotNull(collector.lastItem.metadata.get<TestOneShotMetadata>())

        store.invalidateAsync()
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    @Test
    fun `GIVEN one-shot invalidate WHEN re-observe while cache is alive THEN one-shot metadata is retained`() = runFlowTest {
        val store = storeBuilder()
            .setInMemoryCacheTimeout(1.seconds)
            .build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.invalidateAsync(TestOneShotMetadata)
        runCurrent()
        assertNotNull(collector.lastItem.metadata.get<TestOneShotMetadata>())

        // re-observe before the in-memory cache expires: the same cached value is
        // re-emitted, so its one-shot metadata is still attached.
        collector.cancel()
        advanceTimeBy(500) // less than the cache timeout
        val newCollector = store.observe().startCollecting()
        runCurrent()

        assertNotNull(newCollector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    @Test
    fun `GIVEN one-shot invalidate WHEN re-observe after cache expiration THEN one-shot metadata is not kept`() = runFlowTest {
        val store = storeBuilder()
            .setInMemoryCacheTimeout(1.seconds)
            .build { "value" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.invalidateAsync(TestOneShotMetadata)
        runCurrent()

        // re-observe after the in-memory cache has expired: the value is loaded afresh,
        // so the one-shot metadata is dropped.
        collector.cancel()
        advanceTimeBy(1001) // more than the cache timeout
        val newCollector = store.observe().startCollecting()
        runCurrent()

        assertNull(newCollector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    @Test
    fun `GIVEN submitQuery with one-shot metadata WHEN submit a new query THEN one-shot metadata is not kept`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { query -> "value-$query" }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.submitQuery("q2", TestOneShotMetadata)
        runCurrent()
        assertNotNull(collector.lastItem.metadata.get<TestOneShotMetadata>())

        store.submitQuery("q3")
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    private data class TestMetadata(val value: String) : ContainerMetadata

    private data object TestOneShotMetadata : ContainerMetadata, ContainerMetadata.OneShot

}
