package com.elveum.store.paged

import com.elveum.store.StoreFactory
import com.elveum.store.base.AbstractStoreTest
import com.elveum.store.builders.PagedBuilder
import com.elveum.store.stores.paged.PagedList

abstract class AbstractPagedStoreTest : AbstractStoreTest() {

    protected fun storeBuilder(): PagedBuilder<Int, String> = StoreFactory
        .pagedStoreBuilder<Int, String>(
            initialKey = 0,
            itemId = { it },
        )
        .setCoroutineScopeFactory(createStoreScopeFactory())
        .setFetchDistance(1)

    /**
     * Build pages of 2 items per page: page 0 -> ["item0", "item1"],
     * page 1 -> ["item2", "item3"], etc. The last page has no next key.
     */
    protected fun page(pageIndex: Int, totalPages: Int): PagedList<Int, String> {
        val items = listOf("item${pageIndex * 2}", "item${pageIndex * 2 + 1}")
        val nextKey = (pageIndex + 1).takeIf { it < totalPages }
        return PagedList(items, nextKey)
    }

}
