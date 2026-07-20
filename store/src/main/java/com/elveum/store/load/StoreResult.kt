@file:Suppress("RemoveRedundantQualifierName")

package com.elveum.store.load

import androidx.compose.runtime.Immutable
import com.elveum.container.BackgroundLoadState
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.SourceType
import com.elveum.container.backgroundLoadState
import com.elveum.container.reload
import com.elveum.container.sourceType
import com.elveum.container.subject.paging.PageState
import com.elveum.container.subject.paging.nextPageState
import com.elveum.container.subject.paging.onItemRendered
import com.elveum.container.subject.paging.totalPagedItemsCount

/**
 * The current store result emitted by all stores.
 */
@Immutable
public sealed class StoreResult<out T> {

    /**
     * No data available in the in-memory cache. The store is loading it from
     * the provided data sources (either local, or remote one)
     */
    public data object Loading : StoreResult<Nothing>()

    /**
     * Represents a final state of loaded data, either [Failed] or [Loaded].
     */
    public sealed class Completed<T> : StoreResult<T>()

    /**
     * The store has been loaded data successfully, it can be displayed to the user.
     */
    public data class Loaded<T>(
        public val value: T,
        override val metadata: ContainerMetadata = EmptyMetadata,
    ) : StoreResult.Completed<T>()

    /**
     * The store failed to load data.
     */
    public data class Failed(
        public val exception: Exception,
        override val metadata: ContainerMetadata = EmptyMetadata,
    ) : StoreResult.Completed<Nothing>()


    public open val metadata: ContainerMetadata = EmptyMetadata

    /**
     * The origin source of the emitted values.
     */
    public val sourceType: SourceType get() = metadata.sourceType

    /**
     * The background state of the store. For example, it can load additional data from
     * the remote source while the cached data is available. You can explore such background
     * loads using this property.
     */
    public val backgroundLoadState: BackgroundLoadState get() = metadata.backgroundLoadState

    /**
     * Create a new [StoreResult] instance from the existing one, but with
     * additional [metadata] values.
     */
    public operator fun plus(metadata: ContainerMetadata): StoreResult<T> {
        val combinedMetadata = this.metadata + metadata
        return when (this) {
            is Failed -> Failed(exception, combinedMetadata)
            is Loaded -> Loaded(value, combinedMetadata)
            Loading -> Loading
        }
    }

}

/**
 * Optional state of the next page. Can be emitted by paged store to indicate the current
 * status of loading of the next page.
 */
public val <T> StoreResult<T>.nextPageState: PageState get() = metadata.nextPageState

/**
 * Invalidate an origin store which has been emitted this [StoreResult] instance.
 *
 * @param T the type of the loaded value.
 * @param config defines how the loading state will be propagated to subsequent results.
 * @param metadata custom metadata values to be attached to the reload request and merged
 *   into the emitted result.
 */
public fun <T> StoreResult<T>.invalidate(
    config: LoadConfig? = null,
    metadata: ContainerMetadata = EmptyMetadata,
) {
    this.metadata.reload(config, metadata)
}

/**
 * Notify the store that an item with the specified [index] has been rendered. This may
 * trigger the next page load.
 */
public fun <T> StoreResult<T>.onItemRendered(index: Int) {
    metadata.onItemRendered(index)
}

/**
 * The total number of items available across all pages, as reported by a paged
 * data source via `PagedList(totalCount = ...)` (which attaches
 * `TotalPagedItemsCountMetadata` to the page).
 *
 * A convenience shortcut for `metadata.totalPagedItemsCount`. Returns `-1` when
 * the total count is unknown, i.e. for non-paged results or when no total count
 * was provided by the data source.
 */
public val <T> StoreResult<T>.totalPagedItemsCount: Int get() = metadata.totalPagedItemsCount
