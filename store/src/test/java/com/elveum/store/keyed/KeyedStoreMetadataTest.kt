package com.elveum.store.keyed

import com.elveum.container.ContainerMetadata
import com.elveum.container.get
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.keyed.invalidateAllAsync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the optional [ContainerMetadata] argument that can be attached to
 * invalidation requests of [com.elveum.store.stores.keyed.KeyedStore] and query
 * requests of [com.elveum.store.stores.keyed.KeyedQueryStore].
 */
class KeyedStoreMetadataTest : AbstractKeyedStoreTest() {

    @Test
    fun `GIVEN observed key WHEN invalidate with metadata THEN metadata is attached to the emitted result`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }
        val collector = store.observe("k1").startCollecting()
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestMetadata>())

        store.invalidate("k1", TestMetadata("m1"))
        runCurrent()

        assertResult(StoreResult.Loaded("value-k1"), collector.lastItem)
        assertEquals("m1", collector.lastItem.metadata.get<TestMetadata>()?.value)
    }

    @Test
    fun `GIVEN observed key WHEN invalidateAsync with metadata THEN metadata is attached to the emitted result`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }
        val collector = store.observe("k1").startCollecting()
        runCurrent()

        store.invalidateAsync("k1", TestMetadata("m1"))
        runCurrent()

        assertResult(StoreResult.Loaded("value-k1"), collector.lastItem)
        assertEquals("m1", collector.lastItem.metadata.get<TestMetadata>()?.value)
    }

    @Test
    fun `GIVEN invalidate for one key WHEN another key is observed THEN metadata is attached only to the invalidated key`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()

        store.invalidate("k1", TestMetadata("m1"))
        runCurrent()

        assertEquals("m1", collector1.lastItem.metadata.get<TestMetadata>()?.value)
        assertNull(collector2.lastItem.metadata.get<TestMetadata>())
    }

    @Test
    fun `GIVEN multiple observed keys WHEN invalidateAllAsync with metadata THEN metadata is attached to every key`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()

        store.invalidateAllAsync(TestMetadata("m1"))
        runCurrent()

        assertEquals("m1", collector1.lastItem.metadata.get<TestMetadata>()?.value)
        assertEquals("m1", collector2.lastItem.metadata.get<TestMetadata>()?.value)
    }

    @Test
    fun `GIVEN observed key WHEN submitQuery with metadata THEN metadata is attached to the emitted result`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { key, query -> "$key-$query" }
        val collector = store.observe("k1").startCollecting()
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestMetadata>())

        store.submitQuery("k1", "q2", TestMetadata("m1"))
        runCurrent()

        assertResult(StoreResult.Loaded("k1-q2"), collector.lastItem)
        assertEquals("m1", collector.lastItem.metadata.get<TestMetadata>()?.value)
    }

    @Test
    fun `GIVEN observed key WHEN submitQueryAsync with metadata THEN metadata is attached to the emitted result`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { key, query -> "$key-$query" }
        val collector = store.observe("k1").startCollecting()
        runCurrent()

        store.submitQueryAsync("k1", "q2", TestMetadata("m1"))
        runCurrent()

        assertResult(StoreResult.Loaded("k1-q2"), collector.lastItem)
        assertEquals("m1", collector.lastItem.metadata.get<TestMetadata>()?.value)
    }

    @Test
    fun `GIVEN invalidate with one-shot metadata WHEN invalidate again THEN one-shot metadata is not kept`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }
        val collector = store.observe("k1").startCollecting()
        runCurrent()

        store.invalidate("k1", TestOneShotMetadata)
        runCurrent()
        assertNotNull(collector.lastItem.metadata.get<TestOneShotMetadata>())

        store.invalidate("k1")
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    @Test
    fun `GIVEN invalidateAllAsync with one-shot metadata WHEN invalidate again THEN one-shot metadata is not kept`() = runFlowTest {
        val store = storeBuilder().build { key -> "value-$key" }
        val collector1 = store.observe("k1").startCollecting()
        val collector2 = store.observe("k2").startCollecting()
        runCurrent()

        store.invalidateAllAsync(TestOneShotMetadata)
        runCurrent()
        assertNotNull(collector1.lastItem.metadata.get<TestOneShotMetadata>())
        assertNotNull(collector2.lastItem.metadata.get<TestOneShotMetadata>())

        store.invalidateAllAsync()
        runCurrent()
        assertNull(collector1.lastItem.metadata.get<TestOneShotMetadata>())
        assertNull(collector2.lastItem.metadata.get<TestOneShotMetadata>())
    }

    @Test
    fun `GIVEN submitQuery with one-shot metadata WHEN submit a new query THEN one-shot metadata is not kept`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .build { key, query -> "$key-$query" }
        val collector = store.observe("k1").startCollecting()
        runCurrent()

        store.submitQuery("k1", "q2", TestOneShotMetadata)
        runCurrent()
        assertNotNull(collector.lastItem.metadata.get<TestOneShotMetadata>())

        store.submitQuery("k1", "q3")
        runCurrent()
        assertNull(collector.lastItem.metadata.get<TestOneShotMetadata>())
    }

    private data class TestMetadata(val value: String) : ContainerMetadata

    private data object TestOneShotMetadata : ContainerMetadata, ContainerMetadata.OneShot

}
