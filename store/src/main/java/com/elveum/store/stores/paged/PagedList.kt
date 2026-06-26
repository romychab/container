package com.elveum.store.stores.paged

import androidx.compose.runtime.Immutable
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.subject.paging.TotalPagedItemsCountMetadata

/**
 * Represents a paged chunk of data with optional next page key (if any).
 *
 * @param PageKey the type of the key identifying a page.
 * @param T the type of the items in the page.
 * @property items the items loaded for this page.
 * @property nextKey the key of the next page to load, or `null` if this is the last page.
 * @property metadata optional metadata attached to this page; it is propagated to the
 *   final output list. Defaults to [EmptyMetadata].
 */
@Immutable
public data class PagedList<PageKey : Any, T : Any>(
    val items: List<T>,
    val nextKey: PageKey?,
    val metadata: ContainerMetadata = EmptyMetadata,
) {

    /**
     * Convenience constructor that attaches the total number of items available across
     * all pages as [TotalPagedItemsCountMetadata]. The value can be read back from the
     * emitted result's metadata via
     * [totalPagedItemsCount][com.elveum.container.subject.paging.totalPagedItemsCount].
     *
     * @param items the items loaded for this page.
     * @param nextKey the key of the next page to load, or `null` if this is the last page.
     * @param totalCount the total number of items reported by the data source.
     */
    public constructor(
        items: List<T>,
        nextKey: PageKey?,
        totalCount: Int,
    ) : this(items, nextKey, TotalPagedItemsCountMetadata(totalCount))

}
