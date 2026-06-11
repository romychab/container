package com.elveum.store.internal.load

import com.elveum.container.EmptyMetadata
import com.elveum.container.SourceTypeMetadata
import com.elveum.container.LocalSourceType
import com.elveum.store.load.LoadRequestSource
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadRequestSourceMetadataTest {

    @Test
    fun `GIVEN metadata with load request source WHEN read loadRequestSource THEN stored source is returned`() {
        val metadata = LoadRequestSourceMetadata(LoadRequestSource.Fresh)

        assertEquals(LoadRequestSource.Fresh, metadata.loadRequestSource)
    }

    @Test
    fun `GIVEN combined metadata containing load request source WHEN read loadRequestSource THEN stored source is returned`() {
        val metadata = SourceTypeMetadata(LocalSourceType) +
                LoadRequestSourceMetadata(LoadRequestSource.Offline)

        assertEquals(LoadRequestSource.Offline, metadata.loadRequestSource)
    }

    @Test
    fun `GIVEN empty metadata WHEN read loadRequestSource THEN default source is returned`() {
        assertEquals(LoadRequestSource.Default, EmptyMetadata.loadRequestSource)
    }

    @Test
    fun `GIVEN metadata without load request source WHEN read loadRequestSource THEN default source is returned`() {
        val metadata = SourceTypeMetadata(LocalSourceType)

        assertEquals(LoadRequestSource.Default, metadata.loadRequestSource)
    }
}
