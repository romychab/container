package com.elveum.container

/**
 * Execute a [block] and wrap its result into [Container.Completed].
 */
public inline fun <T> containerOf(
    source: SourceType = UnknownSourceType,
    isLoadingInBackground: Boolean = false,
    noinline reloadFunction: ReloadFunction = EmptyReloadFunction,
    block: () -> T,
): Container.Completed<T> {
    return try {
        successContainer(block(), source, isLoadingInBackground, reloadFunction)
    } catch (e: Exception) {
        errorContainer(e, source, isLoadingInBackground, reloadFunction)
    }
}
