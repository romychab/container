package com.elveum.container

import kotlinx.coroutines.CancellationException

/**
 * Execute a [block] and wrap its result into [Container.Completed].
 */
@Deprecated("Use containerOf with metadata argument.")
public inline fun <T> containerOf(
    source: SourceType,
    isLoadingInBackground: Boolean? = null,
    noinline reloadFunction: ReloadFunction? = null,
    block: () -> T,
): Container.Completed<T> {
    return containerOf(
        metadata = defaultMetadata(source, isLoadingInBackground, reloadFunction),
        block = block
    )
}

/**
 * Execute a [block] and wrap its result into [Container.Completed].
 */
public inline fun <T> containerOf(
    metadata: ContainerMetadata = defaultMetadata(),
    block: () -> T,
): Container.Completed<T> {
    return try {
        successContainer(block(), metadata)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        errorContainer(e, metadata)
    }
}
