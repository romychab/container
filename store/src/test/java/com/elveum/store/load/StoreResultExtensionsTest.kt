package com.elveum.store.load

import com.elveum.container.BackgroundLoadState
import com.elveum.container.LocalSourceType
import com.elveum.container.UnknownSourceType
import com.elveum.container.defaultMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreResultExtensionsTest {

    private val exception = IllegalStateException("boom")

    @Test
    fun `GIVEN loaded result WHEN map THEN value is transformed and metadata is preserved`() {
        val metadata = defaultMetadata(sourceType = LocalSourceType)
        val result: StoreResult<Int> = StoreResult.Loaded(2, metadata)

        val mapped = result.map { it * 10 }

        assertEquals(StoreResult.Loaded(20, metadata), mapped)
    }

    @Test
    fun `GIVEN failed result WHEN map THEN the same failed result is returned`() {
        val result: StoreResult<Int> = StoreResult.Failed(exception)

        val mapped = result.map { it * 10 }

        assertSame(result, mapped)
    }

    @Test
    fun `GIVEN loading result WHEN map THEN loading is returned`() {
        val result: StoreResult<Int> = StoreResult.Loading

        val mapped = result.map { it * 10 }

        assertSame(StoreResult.Loading, mapped)
    }

    @Test
    fun `GIVEN loaded result WHEN map throws THEN failed result with the same metadata is returned`() {
        val metadata = defaultMetadata(sourceType = LocalSourceType)
        val result: StoreResult<Int> = StoreResult.Loaded(2, metadata)

        val mapped = result.map<Int, Int> { throw exception }

        assertEquals(StoreResult.Failed(exception, metadata), mapped)
    }

    @Test
    fun `GIVEN loaded result WHEN getOrNull THEN value is returned`() {
        val result: StoreResult<String> = StoreResult.Loaded("value")

        assertEquals("value", result.getOrNull())
    }

    @Test
    fun `GIVEN failed result WHEN getOrNull THEN null is returned`() {
        val result: StoreResult<String> = StoreResult.Failed(exception)

        assertNull(result.getOrNull())
    }

    @Test
    fun `GIVEN loading result WHEN getOrNull THEN null is returned`() {
        val result: StoreResult<String> = StoreResult.Loading

        assertNull(result.getOrNull())
    }

    @Test
    fun `GIVEN failed result WHEN failureOrNull THEN exception is returned`() {
        val result: StoreResult<String> = StoreResult.Failed(exception)

        assertSame(exception, result.failureOrNull())
    }

    @Test
    fun `GIVEN loaded result WHEN failureOrNull THEN null is returned`() {
        val result: StoreResult<String> = StoreResult.Loaded("value")

        assertNull(result.failureOrNull())
    }

    @Test
    fun `GIVEN loading result WHEN failureOrNull THEN null is returned`() {
        val result: StoreResult<String> = StoreResult.Loading

        assertNull(result.failureOrNull())
    }

    @Test
    fun `GIVEN different results WHEN isLoaded THEN true only for loaded`() {
        assertTrue(StoreResult.Loaded("value").isLoaded())
        assertFalse(StoreResult.Failed(exception).isLoaded())
        assertFalse(StoreResult.Loading.isLoaded())
    }

    @Test
    fun `GIVEN different results WHEN isCompleted THEN true for loaded and failed`() {
        assertTrue(StoreResult.Loaded("value").isCompleted())
        assertTrue(StoreResult.Failed(exception).isCompleted())
        assertFalse(StoreResult.Loading.isCompleted())
    }

    @Test
    fun `GIVEN different results WHEN isFailed THEN true only for failed`() {
        assertTrue(StoreResult.Failed(exception).isFailed())
        assertFalse(StoreResult.Loaded("value").isFailed())
        assertFalse(StoreResult.Loading.isFailed())
    }

    @Test
    fun `GIVEN different results WHEN isForegroundLoading THEN true only for loading`() {
        assertTrue(StoreResult.Loading.isForegroundLoading())
        assertFalse(StoreResult.Loaded("value").isForegroundLoading())
        assertFalse(StoreResult.Failed(exception).isForegroundLoading())
    }

    @Test
    fun `GIVEN completed result with background loading metadata WHEN isBackgroundLoading THEN true`() {
        val metadata = defaultMetadata(backgroundLoadState = BackgroundLoadState.Loading)
        val loaded: StoreResult<String> = StoreResult.Loaded("value", metadata)
        val failed: StoreResult<String> = StoreResult.Failed(exception, metadata)

        assertTrue(loaded.isBackgroundLoading())
        assertTrue(failed.isBackgroundLoading())
    }

    @Test
    fun `GIVEN completed result without background loading WHEN isBackgroundLoading THEN false`() {
        val loaded: StoreResult<String> = StoreResult.Loaded("value")

        assertFalse(loaded.isBackgroundLoading())
    }

    @Test
    fun `GIVEN loading result WHEN isBackgroundLoading THEN false`() {
        assertFalse(StoreResult.Loading.isBackgroundLoading())
    }

    @Test
    fun `GIVEN foreground loading WHEN hasAnyLoading THEN true`() {
        assertTrue(StoreResult.Loading.hasAnyLoading())
    }

    @Test
    fun `GIVEN background loading WHEN hasAnyLoading THEN true`() {
        val metadata = defaultMetadata(backgroundLoadState = BackgroundLoadState.Loading)
        val loaded: StoreResult<String> = StoreResult.Loaded("value", metadata)

        assertTrue(loaded.hasAnyLoading())
    }

    @Test
    fun `GIVEN idle completed result WHEN hasAnyLoading THEN false`() {
        val loaded: StoreResult<String> = StoreResult.Loaded("value")

        assertFalse(loaded.hasAnyLoading())
    }

    @Test
    fun `GIVEN loaded result with source metadata WHEN read sourceType THEN it is returned`() {
        val loaded: StoreResult<String> = StoreResult.Loaded("value", defaultMetadata(sourceType = LocalSourceType))

        assertEquals(LocalSourceType, loaded.sourceType)
    }

    @Test
    fun `GIVEN result without source metadata WHEN read sourceType THEN unknown is returned`() {
        val loaded: StoreResult<String> = StoreResult.Loaded("value")

        assertEquals(UnknownSourceType, loaded.sourceType)
    }

    @Test
    fun `GIVEN result without background metadata WHEN read backgroundLoadState THEN idle is returned`() {
        val loaded: StoreResult<String> = StoreResult.Loaded("value")

        assertEquals(BackgroundLoadState.Idle, loaded.backgroundLoadState)
    }
}
