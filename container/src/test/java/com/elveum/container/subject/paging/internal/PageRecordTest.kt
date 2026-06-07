package com.elveum.container.subject.paging.internal

import com.elveum.container.isPending
import com.elveum.container.isSuccess
import com.elveum.container.successContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PageRecordTest {

    @Test
    fun safeContainer_returnsOwnContainer_whenContainerIsSet() {
        val expected = successContainer(listOf("a", "b"))
        val record = MutablePageRecord(pageIndex = 0, pageKey = 0, priority = 1, container = expected)

        assertTrue(record.safeContainer.isSuccess())
        assertEquals(expected, record.safeContainer)
    }

    @Test
    fun safeContainer_returnsPendingContainer_whenContainerIsNull() {
        val record = MutablePageRecord<Int, String>(pageIndex = 0, pageKey = 0, priority = 1, container = null)

        assertTrue(record.safeContainer.isPending())
    }

    @Test
    fun mutableRecord_immutable_returnsImmutableWithSameFields() {
        val container = successContainer(listOf("x"))
        val mutable = MutablePageRecord(
            pageIndex = 1,
            pageKey = "key",
            priority = 5L,
            container = container,
            isCompleted = true,
        )

        val immutable = mutable.immutable()

        assertEquals(1, immutable.pageIndex)
        assertEquals("key", immutable.pageKey)
        assertEquals(5L, immutable.priority)
        assertEquals(container, immutable.container)
        assertEquals(true, immutable.isCompleted)
    }

    @Test
    fun immutableRecord_immutable_returnsSelf() {
        val record = ImmutablePageRecord<Int, String>(
            pageIndex = 0, pageKey = 0, priority = 1, container = null, isCompleted = false,
        )

        assertSame(record, record.immutable())
    }

}
