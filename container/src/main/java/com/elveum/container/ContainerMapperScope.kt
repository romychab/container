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
     * The original source where the data is loading from.
     */
    public val source: SourceType

    /**
     * Whether there is another value load in progress.
     * For example, it may be a value being loaded from the remote source, whereas
     * this container represents a local source.
     */
    public val isLoadingInBackground: Boolean

    /**
     * Function for reloading data.
     */
    public val reloadFunction: ReloadFunction

    /**
     * Reload data encapsulated by container.
     */
    public fun reload(silently: Boolean = false) {
        reloadFunction.invoke(silently)
    }

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
            source = source ?: this.source,
            isLoadingInBackground = isLoadingInBackground ?: this.isLoadingInBackground,
            reloadFunction = reloadFunction ?: this.reloadFunction,
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
            source = source ?: this.source,
            isLoadingInBackground = isLoadingInBackground ?: this.isLoadingInBackground,
            reloadFunction = reloadFunction ?: this.reloadFunction,
        )
    }

    /**
     * Create a pending container.
     */
    public fun pendingContainer(): Container.Pending = Container.Pending

}
