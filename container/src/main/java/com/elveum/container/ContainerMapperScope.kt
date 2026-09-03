package com.elveum.container

import com.elveum.container.subject.paging.PageState
import com.elveum.container.subject.paging.nextPageState
import com.elveum.container.subject.paging.onItemRendered
import com.elveum.container.subject.paging.totalPagedItemsCount

/**
 * Mapping function for converting containers of type `T` into containers
 * of another type `R`.
 */
public typealias ContainerMapper<T, R> = ContainerMapperScope.(T) -> R

/**
 * A scope that can be accessed from [ContainerMapper] functions.
 */
public interface ContainerMapperScope {

    /**
     * Additional metadata attached to the container.
     */
    public val metadata: ContainerMetadata

    /**
     * The original source where the data is loading from.
     */
    public val sourceType: SourceType get() = metadata.sourceType

    /**
     * Whether there is another value load in progress.
     * For example, it may be a value being loaded from the remote source, whereas
     * this container represents a local source.
     */
    public val backgroundLoadState: BackgroundLoadState get() = metadata.backgroundLoadState

    /**
     * Function for reloading data.
     */
    public val reloadFunction: ReloadFunction get() = metadata.reloadFunction

    /**
     * The current state of the next page load, for paged containers.
     *
     * Returns [PageState.Idle] when the container is not paged.
     */
    public val nextPageState: PageState get() = metadata.nextPageState

    /**
     * The total number of items across all pages, for paged containers.
     *
     * Returns `-1` when the total count is unknown.
     */
    public val totalPagedItemsCount: Int get() = metadata.totalPagedItemsCount

    /**
     * Notify the page loader that an item with the specified [index] has been
     * rendered. Does nothing when the container is not paged.
     */
    public fun onItemRendered(index: Int) {
        metadata.onItemRendered(index)
    }

    /**
     * Reload data encapsulated by container.
     */
    public fun reload(config: LoadConfig?, metadata: ContainerMetadata) {
        reloadFunction.invoke(config, metadata)
    }

    /**
     * Reload data encapsulated by container.
     */
    public fun reload(config: LoadConfig?) {
        reloadFunction.invoke(config, EmptyMetadata)
    }

    /**
     * Reload data encapsulated by container.
     */
    public fun reload() {
        reloadFunction.invoke(null, EmptyMetadata)
    }

    /**
     * Create a success container.
     */
    public fun <T> successContainer(
        value: T,
        source: SourceType? = null,
        backgroundLoadState: BackgroundLoadState? = null,
        reloadFunction: ReloadFunction? = null,
    ): Container.Success<T> {
        return com.elveum.container.successContainer(
            value = value,
            metadata = this.metadata + defaultMetadata(source, backgroundLoadState, reloadFunction)
        )
    }

    /**
     * Create a success container.
     */
    public fun <T> successContainer(
        value: T,
        metadata: ContainerMetadata = EmptyMetadata,
    ): Container.Success<T> {
        return com.elveum.container.successContainer(
            value = value,
            metadata = this.metadata + metadata
        )
    }

    /**
     * Create an error container.
     */
    public fun errorContainer(
        exception: Exception,
        source: SourceType? = null,
        backgroundLoadState: BackgroundLoadState? = null,
        reloadFunction: ReloadFunction? = null,
    ): Container.Error {
        return com.elveum.container.errorContainer(
            exception = exception,
            metadata = this.metadata + defaultMetadata(source, backgroundLoadState, reloadFunction)
        )
    }

    /**
     * Create an error container.
     */
    public fun errorContainer(
        exception: Exception,
        metadata: ContainerMetadata = EmptyMetadata,
    ): Container.Error {
        return com.elveum.container.errorContainer(
            exception = exception,
            metadata = this.metadata + metadata
        )
    }

    /**
     * Create a pending container.
     */
    public fun pendingContainer(): Container.Pending = Container.Pending

}
