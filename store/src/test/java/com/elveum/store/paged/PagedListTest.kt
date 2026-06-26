package com.elveum.store.paged

import com.elveum.container.EmptyMetadata
import com.elveum.container.subject.paging.TotalPagedItemsCountMetadata
import com.elveum.container.subject.paging.totalPagedItemsCount
import com.elveum.store.stores.paged.PagedList
import org.junit.Assert.assertEquals
import org.junit.Test

class PagedListTest {

    @Test
    fun `GIVEN totalCount constructor WHEN created THEN metadata holds the total count`() {
        val pagedList = PagedList(
            items = listOf("a", "b"),
            nextKey = 1,
            totalCount = 42,
        )

        assertEquals(TotalPagedItemsCountMetadata(42), pagedList.metadata)
        assertEquals(42, pagedList.metadata.totalPagedItemsCount)
    }

    @Test
    fun `GIVEN default constructor WHEN created THEN metadata is empty`() {
        val pagedList = PagedList(
            items = listOf("a", "b"),
            nextKey = 1,
        )

        assertEquals(EmptyMetadata, pagedList.metadata)
    }
}
