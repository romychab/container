@file:Suppress("RemoveRedundantQualifierName")

package com.elveum.store.load

import androidx.compose.runtime.Immutable
import com.elveum.container.BackgroundLoadState
import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.SourceType
import com.elveum.container.backgroundLoadState
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.sourceType
import com.elveum.container.subject.paging.PageState
import com.elveum.container.subject.paging.nextPageState
import com.elveum.container.successContainer

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
}

/**
 * Optional state of the next page. Can be emitted by paged store to indicate the current
 * status of loading of the next page.
 */
public val <T> StoreResult<T>.nextPageState: PageState get() = metadata.nextPageState
