package com.elveum.container

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
    public val source: SourceType get() = metadata.sourceType

    /**
     * Whether there is another value load in progress.
     * For example, it may be a value being loaded from the remote source, whereas
     * this container represents a local source.
     */
    public val isLoadingInBackground: Boolean get() = metadata.isLoadingInBackground

    /**
     * Function for reloading data.
     */
    public val reloadFunction: ReloadFunction get() = metadata.reloadFunction

    /**
     * Create a success container.
     */
    public fun <T> successContainer(
        value: T,
        source: SourceType? = null,
        isLoadingInBackground: Boolean? = null,
        reloadFunction: ReloadFunction? = null,
    ): Container.Success<T> {
        return com.elveum.container.successContainer(
            value = value,
            metadata = this.metadata + defaultMetadata(source, isLoadingInBackground, reloadFunction)
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
        isLoadingInBackground: Boolean? = null,
        reloadFunction: ReloadFunction? = null,
    ): Container.Error {
        return com.elveum.container.errorContainer(
            exception = exception,
            metadata = this.metadata + defaultMetadata(source, isLoadingInBackground, reloadFunction)
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
