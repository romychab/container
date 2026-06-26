package com.elveum.container.subject.paging

import com.elveum.container.ContainerMetadata
import com.elveum.container.get

/**
 * Get the total number of items available across all pages, as reported by the
 * data source via [TotalPagedItemsCountMetadata].
 *
 * Returns `-1` when the total count is unknown (i.e. no [TotalPagedItemsCountMetadata]
 * has been attached to the metadata).
 *
 * @see TotalPagedItemsCountMetadata
 */
public val ContainerMetadata.totalPagedItemsCount: Int
    get() = get<TotalPagedItemsCountMetadata>()?.totalPagedItemsCount ?: -1

/**
 * Metadata holding the total number of items available across all pages.
 *
 * Attach it to a page via the `metadata` argument of `PageEmitter.emitPage` so that
 * the total count is propagated to the final output list and can be read back via
 * [totalPagedItemsCount].
 *
 * @property totalPagedItemsCount the total number of items reported by the data source.
 */
public data class TotalPagedItemsCountMetadata(
    val totalPagedItemsCount: Int,
) : ContainerMetadata
