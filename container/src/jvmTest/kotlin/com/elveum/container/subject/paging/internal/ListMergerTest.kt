package com.elveum.container.subject.paging.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class ListMergerTest {

    @Test
    fun testAllEmptyLists() {
        val base = mutableListOf<Item>()
        val nonFinalOldItems = emptyList<Item>()
        val nonFinalNewItems = emptyList<Item>()
        val merger = createListMerger(base)

        merger.mergeFrom(nonFinalOldItems, nonFinalNewItems)

        assertEquals(
            emptyList<Item>(),
            base
        )
    }

    @Test
    fun nonFinalNewItemsAddedToBase() {
        val base = mutableListOf<Item>()
        val nonFinalOldItems = emptyList<Item>()
        val nonFinalNewItems = listOf(updated(1))
        val merger = createListMerger(base)

        merger.mergeFrom(nonFinalOldItems, nonFinalNewItems)

        assertEquals(
            listOf(updated(1)),
            base
        )
    }

    @Test
    fun deleteOldItemsNotInNew() {
        // targetList = [a, b, c, d, e]
        // nonFinalOldItems = [C, D, E]
        // nonFinalNewItems = [E]
        // RESULT = [a, b, E]
        val base = mutableListOf(item(1), item(2), item(3), item(4), item(5))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = listOf(updated(3), updated(4), updated(5)),
            nonFinalNewItems = listOf(updated(5)),
        )

        assertEquals(
            listOf(item(1), item(2), updated(5)),
            base,
        )
    }

    @Test
    fun addAllNewItemsToEmptyTarget() {
        // targetList = []
        // nonFinalNewItems = [A, B, C]
        // RESULT = [A, B, C]
        val base = mutableListOf<Item>()
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(1), updated(2), updated(3)),
        )

        assertEquals(
            listOf(updated(1), updated(2), updated(3)),
            base,
        )
    }

    @Test
    fun replaceInPlace_allInTarget_sameOrder() {
        // targetList = [a, b, c, d, e]
        // nonFinalNewItems = [B, D, E]
        // RESULT = [a, B, c, D, E]
        val base = mutableListOf(item(1), item(2), item(3), item(4), item(5))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(2), updated(4), updated(5)),
        )

        assertEquals(
            listOf(item(1), updated(2), item(3), updated(4), updated(5)),
            base,
        )
    }

    @Test
    fun appendWhenNoneInTarget() {
        // targetList = [a, b, c, d, e]
        // nonFinalNewItems = [F, G, H]
        // RESULT = [a, b, c, d, e, F, G, H]
        val base = mutableListOf(item(1), item(2), item(3), item(4), item(5))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(6), updated(7), updated(8)),
        )

        assertEquals(
            listOf(item(1), item(2), item(3), item(4), item(5), updated(6), updated(7), updated(8)),
            base,
        )
    }

    @Test
    fun partialOverlap_sameOrder_insertNewItems() {
        // targetList = [a, b, c, d, e]
        // nonFinalNewItems = [K, C, L, M, E]
        // RESULT = [a, b, K, C, d, L, M, E]
        val base = mutableListOf(item(1), item(2), item(3), item(4), item(5))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(11), updated(3), updated(12), updated(13), updated(5)),
        )

        assertEquals(
            listOf(item(1), item(2), updated(11), updated(3), item(4), updated(12), updated(13), updated(5)),
            base,
        )
    }

    @Test
    fun partialOverlap_differentOrder_reorderAndInsert() {
        // targetList = [a, b, c, d, e, f]
        // nonFinalNewItems = [B, K, E, D]
        // RESULT = [a, B, c, K, E, D, f]
        val base = mutableListOf(item(1), item(2), item(3), item(4), item(5), item(6))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(2), updated(11), updated(5), updated(4)),
        )

        assertEquals(
            listOf(item(1), updated(2), item(3), updated(11), updated(5), updated(4), item(6)),
            base,
        )
    }

    @Test
    fun allItemsDeleted() {
        val base = mutableListOf(item(1), item(2), item(3))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = listOf(updated(1), updated(2), updated(3)),
            nonFinalNewItems = emptyList(),
        )

        assertEquals(emptyList<Item>(), base)
    }

    @Test
    fun deleteAndAddNewItems() {
        // Remove old non-final items, add completely new items
        val base = mutableListOf(item(1), item(2), item(3))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = listOf(updated(2)),
            nonFinalNewItems = listOf(updated(10)),
        )

        assertEquals(
            listOf(item(1), item(3), updated(10)),
            base,
        )
    }

    @Test
    fun itemInBothOldAndNew_keptAndReplaced() {
        // Item with same ID in both old and new: should NOT be deleted, should be replaced
        val base = mutableListOf(item(1), item(2), item(3))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = listOf(updated(2), updated(3)),
            nonFinalNewItems = listOf(updated(2)),
        )

        assertEquals(
            listOf(item(1), updated(2)),
            base,
        )
    }

    @Test
    fun deleteNonExistentItems_noEffect() {
        // Old items not in targetList - nothing to remove
        val base = mutableListOf(item(1), item(2))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = listOf(updated(10), updated(20)),
            nonFinalNewItems = emptyList(),
        )

        assertEquals(
            listOf(item(1), item(2)),
            base,
        )
    }

    @Test
    fun fullReverseOrder() {
        // All non-final items in reverse order
        val base = mutableListOf(item(1), item(2), item(3), item(4))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(4), updated(3), updated(2), updated(1)),
        )

        assertEquals(
            listOf(updated(4), updated(3), updated(2), updated(1)),
            base,
        )
    }

    @Test
    fun reverseAnchors_withFinalItemsBetween() {
        // targetList = [a, b, c, d], new = [D, K, B]
        // Anchors b,d reorder to D,B. K inserted between.
        val base = mutableListOf(item(1), item(2), item(3), item(4))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(4), updated(11), updated(2)),
        )

        assertEquals(
            listOf(item(1), updated(4), item(3), updated(11), updated(2)),
            base,
        )
    }

    @Test
    fun newItemsBeforeFirstAnchor() {
        // New items placed before the first existing anchor
        val base = mutableListOf(item(1), item(2), item(3))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(10), updated(11), updated(2)),
        )

        assertEquals(
            listOf(item(1), updated(10), updated(11), updated(2), item(3)),
            base,
        )
    }

    @Test
    fun newItemsAfterLastAnchor() {
        // New items placed after the last existing anchor
        val base = mutableListOf(item(1), item(2), item(3))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(2), updated(10), updated(11)),
        )

        assertEquals(
            listOf(item(1), updated(2), item(3), updated(10), updated(11)),
            base,
        )
    }

    @Test
    fun singleItemReplace() {
        val base = mutableListOf(item(1))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(1)),
        )

        assertEquals(listOf(updated(1)), base)
    }

    @Test
    fun sequentialMerges_simulatePageLoading() {
        // Simulate loading page 1, then page 2
        val base = mutableListOf<Item>()
        val merger = createListMerger(base)

        // Page 1 loads: items 1, 2, 3
        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(1), updated(2), updated(3)),
        )
        assertEquals(
            listOf(updated(1), updated(2), updated(3)),
            base,
        )

        // Page 1 becomes final (not in old/new), page 2 loads: items 4, 5, 6
        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(4), updated(5), updated(6)),
        )
        assertEquals(
            listOf(updated(1), updated(2), updated(3), updated(4), updated(5), updated(6)),
            base,
        )
    }

    @Test
    fun sequentialMerges_pageUpdateWithOverlap() {
        val base = mutableListOf<Item>()
        val merger = createListMerger(base)

        // First merge: items 1, 2, 3 (non-final)
        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(item(1), item(2), item(3)),
        )

        // Second merge: items 1,2,3 were non-final, now items 1,2,3,4,5 are non-final
        // (simulates page 1 still non-final, page 2 added)
        merger.mergeFrom(
            nonFinalOldItems = listOf(item(1), item(2), item(3)),
            nonFinalNewItems = listOf(updated(1), updated(2), updated(3), updated(4), updated(5)),
        )

        assertEquals(
            listOf(updated(1), updated(2), updated(3), updated(4), updated(5)),
            base,
        )
    }

    @Test
    fun sequentialMerges_removeAndAdd() {
        val base = mutableListOf<Item>()
        val merger = createListMerger(base)

        // Load page 1
        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(item(1), item(2), item(3)),
        )

        // Page 1 becomes final. Page 2 loads but item 2 was removed, item 4 added
        // Old non-final: [1,2,3], new non-final: [1,3,4]
        merger.mergeFrom(
            nonFinalOldItems = listOf(item(1), item(2), item(3)),
            nonFinalNewItems = listOf(updated(1), updated(3), updated(4)),
        )

        assertEquals(
            listOf(updated(1), updated(3), updated(4)),
            base,
        )
    }

    @Test
    fun deleteAndInsertWithAnchors() {
        // Some items deleted, some kept, new items inserted
        val base = mutableListOf(item(1), item(2), item(3), item(4), item(5))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = listOf(updated(2), updated(4)),
            nonFinalNewItems = listOf(updated(4), updated(10)),
        )

        // item(2) deleted (in old, not in new)
        // item(4) kept and replaced (in both old and new)
        // updated(10) appended (new, not in target)
        assertEquals(
            listOf(item(1), item(3), updated(4), item(5), updated(10)),
            base,
        )
    }

    @Test
    fun allFinalItemsWithNewNonFinal() {
        // targetList has only final items, nonFinalNewItems are all new
        val base = mutableListOf(item(1), item(2), item(3))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(10), updated(20)),
        )

        assertEquals(
            listOf(item(1), item(2), item(3), updated(10), updated(20)),
            base,
        )
    }

    @Test
    fun emptyNonFinalNewItems_onlyDeletions() {
        val base = mutableListOf(item(1), item(2), item(3), item(4))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = listOf(updated(2), updated(4)),
            nonFinalNewItems = emptyList(),
        )

        assertEquals(
            listOf(item(1), item(3)),
            base,
        )
    }

    @Test
    fun threeAnchors_middleOneNew_reordered() {
        // targetList = [a, b, c, d, e]
        // new = [D, K, B, E] (d and b swapped, K new between them)
        val base = mutableListOf(item(1), item(2), item(3), item(4), item(5))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(4), updated(11), updated(2), updated(5)),
        )

        // Final items: 1, 3
        // Anchors reordered: positions [1,3,4] get [D,E,B] ... wait
        // Anchors in targetList: 2(pos1), 4(pos3), 5(pos4)
        // Anchors in new order: 4, 2, 5
        // Positions sorted: [1, 3, 4], assign: 4→pos1, 2→pos3, 5→pos4
        // After reorder: [1, D, 3, B, E]
        // Interleave with new=[D, K, B, E]:
        //   1(final), D(anchor) -> flush [D], 3(final), B(anchor) -> flush [K, B], E(anchor) -> flush [E]
        // Result: [1, D, 3, K, B, E]
        assertEquals(
            listOf(item(1), updated(4), item(3), updated(11), updated(2), updated(5)),
            base,
        )
    }

    @Test
    fun allNonFinal_completeReorder() {
        // All items are non-final, reverse them
        val base = mutableListOf(item(1), item(2), item(3))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(3), updated(2), updated(1)),
        )

        assertEquals(
            listOf(updated(3), updated(2), updated(1)),
            base,
        )
    }

    @Test
    fun newItemsAtBeginning_noAnchors() {
        // All new items, none in target - should append
        val base = mutableListOf(item(1), item(2))
        val merger = createListMerger(base)

        merger.mergeFrom(
            nonFinalOldItems = emptyList(),
            nonFinalNewItems = listOf(updated(10), updated(20), updated(30)),
        )

        assertEquals(
            listOf(item(1), item(2), updated(10), updated(20), updated(30)),
            base,
        )
    }

    private fun createListMerger(target: MutableList<Item>) =
        ListMerger(target) { it.id }

    private fun item(id: Int) = Item(id, id)

    private fun updated(id: Int) = Item(id, id * 1000)

    data class Item(
        val id: Int,
        val value: Int,
    )

}
