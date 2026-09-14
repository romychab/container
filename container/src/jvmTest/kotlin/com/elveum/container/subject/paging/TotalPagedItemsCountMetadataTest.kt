package com.elveum.container.subject.paging

import com.elveum.container.EmptyMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class TotalPagedItemsCountMetadataTest {

    @Test
    fun `GIVEN metadata with total count WHEN totalPagedItemsCount THEN the count is returned`() {
        val metadata = EmptyMetadata + TotalPagedItemsCountMetadata(totalPagedItemsCount = 42)

        assertEquals(42, metadata.totalPagedItemsCount)
    }

    @Test
    fun `GIVEN metadata without total count WHEN totalPagedItemsCount THEN minus one is returned`() {
        val metadata = EmptyMetadata

        assertEquals(-1, metadata.totalPagedItemsCount)
    }

    @Test
    fun `GIVEN total count combined with other metadata WHEN totalPagedItemsCount THEN the count is still returned`() {
        val metadata = NextPageStateMetadata(PageState.Idle) +
                TotalPagedItemsCountMetadata(totalPagedItemsCount = 7)

        assertEquals(7, metadata.totalPagedItemsCount)
    }

    @Test
    fun `GIVEN total count replaced by a newer one WHEN totalPagedItemsCount THEN the latest count is returned`() {
        val metadata = TotalPagedItemsCountMetadata(totalPagedItemsCount = 1) +
                TotalPagedItemsCountMetadata(totalPagedItemsCount = 2)

        assertEquals(2, metadata.totalPagedItemsCount)
    }
}
