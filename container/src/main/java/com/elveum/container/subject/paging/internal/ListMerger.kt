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
        val state = State(nonFinalOldItems, nonFinalNewItems)

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

            // step 4 - replace data in targetList by result list
            targetList.clear()
            targetList += result
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
        for (i in nonFinalPositions.indices) {
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

    private inner class State(
        val nonFinalOldItems: List<T>,
        val nonFinalNewItems: List<T>,
    ) {
        val oldIds: HashSet<Any> = nonFinalOldItems.mapTo(HashSet()) { itemId(it) }
        val newIds: HashSet<Any> = nonFinalNewItems.mapTo(HashSet()) { itemId(it) }
    }

}
