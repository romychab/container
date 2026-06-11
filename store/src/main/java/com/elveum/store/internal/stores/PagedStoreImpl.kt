package com.elveum.store.internal.stores

import com.elveum.store.stores.paged.PagedQueryStore
import com.elveum.store.stores.paged.PagedStore
import com.elveum.store.stores.base.BasePagedStore

internal class PagedStoreImpl<T : Any>(
    private val pagedQueryStore: PagedQueryStore<Unit, T>
) : PagedStore<T>, BasePagedStore<T> by pagedQueryStore {

    override fun whenActive(block: suspend PagedStore<T>.() -> Unit): PagedStore<T> = apply {
        pagedQueryStore.whenActive {
            block(this@PagedStoreImpl)
        }
    }

}
