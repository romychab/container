package com.elveum.store.stores.paged

import androidx.compose.runtime.Immutable

/**
 * Represents a paged chunk of data with optional next page key (if any).
 */
@Immutable
public data class PagedList<PageKey : Any, T : Any>(
    val items: List<T>,
    val nextKey: PageKey?,
)
