package com.elveum.container.subject.paging.internal

import kotlin.collections.plusAssign

internal class ListMerger<T>(
    val targetList: MutableList<T>,
    val itemId: (T) -> Any,
) {

    fun mergeFrom(
        nonFinalOldItems: List<T>,
        nonFinalNewItems: List<T>,
    ) {
        // Items are identified by itemId(). Duplicate ids (within a page or from
        // the same logical item appearing on more than one page) break the 1:1
        // position/anchor correspondence in reorderNonFinalItems() and previously
        // caused an IndexOutOfBoundsException. Deduplicate incoming new items by
        // id (first occurrence wins) so the merge always operates on unique ids.
        val state = State(nonFinalOldItems, nonFinalNewItems.distinctById())

        state.apply {
            // step 1 - remove items from targetList that exist in old but not in new
            removeExpiredItems()

            // step 2 - reorder non-final items in targetList to match nonFinalNewItems order;
            // collect anchor positions of non-final items, then place the matching
            // nonFinalNewItems at those positions in nonFinalNewItems order.
            reorderNonFinalItems()

            // step 3 - build result by interleaving final items with nonFinalNewItems;
            // when an anchor is encountered, flush all nonFinalNewItems up to that anchor
            val result = buildFinalList()

            // step 4 - replace data in targetList by result list, keeping ids
            // unique so the target never accumulates duplicates across merges
            targetList.clear()
            targetList += result.distinctById()
        }
    }

    private fun State.removeExpiredItems() {
        targetList.removeAll { itemId(it) in oldIds && itemId(it) !in newIds }
    }

    private fun State.reorderNonFinalItems() {
        val nonFinalPositions = ArrayList<Int>()
        val existingIds = HashSet<Any>()
        for (i in targetList.indices) {
            val id = itemId(targetList[i])
            existingIds.add(id)
            if (id in newIds) {
                nonFinalPositions.add(i)
            }
        }
        val anchors = nonFinalNewItems.filter { itemId(it) in existingIds }
        // With unique ids the two counts match; guard against any residual
        // asymmetry (e.g. a target list that already contained duplicates) so a
        // stale duplicate can never cause an out-of-bounds access. Leftover
        // duplicate positions are cleaned up by distinctById() in mergeFrom().
        val count = minOf(nonFinalPositions.size, anchors.size)
        for (i in 0 until count) {
            targetList[nonFinalPositions[i]] = anchors[i]
        }
    }

    private fun State.buildFinalList(): List<T> {
        val result = ArrayList<T>(targetList.size + nonFinalNewItems.size)
        var newIdx = 0
        for (item in targetList) {
            val id = itemId(item)
            if (id in newIds) {
                while (newIdx < nonFinalNewItems.size) {
                    val newItem = nonFinalNewItems[newIdx++]
                    result.add(newItem)
                    if (itemId(newItem) == id) break
                }
            } else {
                result.add(item)
            }
        }
        while (newIdx < nonFinalNewItems.size) {
            result.add(nonFinalNewItems[newIdx++])
        }
        return result
    }

    private fun List<T>.distinctById(): List<T> = distinctBy { itemId(it) }

    private inner class State(
        val nonFinalOldItems: List<T>,
        val nonFinalNewItems: List<T>,
    ) {
        val oldIds: HashSet<Any> = nonFinalOldItems.mapTo(HashSet()) { itemId(it) }
        val newIds: HashSet<Any> = nonFinalNewItems.mapTo(HashSet()) { itemId(it) }
    }

}
