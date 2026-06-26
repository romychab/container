package com.elveum.store.load

import com.elveum.container.Container
import com.elveum.container.LocalSourceType
import com.elveum.container.defaultMetadata
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreResultContainerExtensionsTest {

    private val exception = IllegalStateException("boom")

    @Test
    fun `GIVEN pending container WHEN toStoreResult THEN loading is returned`() {
        val container: Container<String> = pendingContainer()

        assertEquals(StoreResult.Loading, container.toStoreResult())
    }

    @Test
    fun `GIVEN success container WHEN toStoreResult THEN loaded with the same value and metadata is returned`() {
        val metadata = defaultMetadata(sourceType = LocalSourceType)
        val container: Container<String> = successContainer("value", metadata)

        assertEquals(StoreResult.Loaded("value", metadata), container.toStoreResult())
    }

    @Test
    fun `GIVEN error container WHEN toStoreResult THEN failed with the same exception and metadata is returned`() {
        val metadata = defaultMetadata(sourceType = LocalSourceType)
        val container: Container<String> = errorContainer(exception, metadata)

        assertEquals(StoreResult.Failed(exception, metadata), container.toStoreResult())
    }

    @Test
    fun `GIVEN loading result WHEN toContainer THEN pending container is returned`() {
        val result: StoreResult<String> = StoreResult.Loading

        assertEquals(pendingContainer(), result.toContainer())
    }

    @Test
    fun `GIVEN loaded result WHEN toContainer THEN success container with the same value and metadata is returned`() {
        val metadata = defaultMetadata(sourceType = LocalSourceType)
        val result: StoreResult<String> = StoreResult.Loaded("value", metadata)

        assertEquals(successContainer("value", metadata), result.toContainer())
    }

    @Test
    fun `GIVEN failed result WHEN toContainer THEN error container with the same exception and metadata is returned`() {
        val metadata = defaultMetadata(sourceType = LocalSourceType)
        val result: StoreResult<String> = StoreResult.Failed(exception, metadata)

        assertEquals(errorContainer(exception, metadata), result.toContainer())
    }

    @Test
    fun `GIVEN loaded result WHEN converted to container and back THEN it stays the same`() {
        val metadata = defaultMetadata(sourceType = LocalSourceType)
        val result: StoreResult<String> = StoreResult.Loaded("value", metadata)

        assertEquals(result, result.toContainer().toStoreResult())
    }

    @Test
    fun `GIVEN loaded result WHEN withMetadataFrom another result THEN value and loaded type are preserved`() {
        val origin: StoreResult<Int> = StoreResult.Loaded(1, defaultMetadata(sourceType = LocalSourceType))
        val result: StoreResult<String> = StoreResult.Loaded("value")

        val combined = result.withMetadataFrom(origin)

        assertTrue(combined.isLoaded())
        assertEquals("value", combined.getOrNull())
    }

    @Test
    fun `GIVEN failed result WHEN withMetadataFrom another result THEN failed type is preserved`() {
        val origin: StoreResult<Int> = StoreResult.Loaded(1, defaultMetadata(sourceType = LocalSourceType))
        val result: StoreResult<String> = StoreResult.Failed(exception)

        val combined = result.withMetadataFrom(origin)

        assertTrue(combined.isFailed())
        assertEquals(exception, (combined as StoreResult.Failed).exception)
    }
}
