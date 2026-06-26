package com.elveum.store.load

import com.elveum.container.CombineContainerFlowScope
import com.elveum.container.Container
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer
import com.elveum.container.update

/**
 * Convert a [Container] instance into [StoreResult].
 */
public fun <T> Container<T>.toStoreResult(): StoreResult<T> {
    return fold(
        onPending = { StoreResult.Loading },
        onSuccess = { StoreResult.Loaded(it, metadata) },
        onError = { StoreResult.Failed(it, metadata) },
    )
}

/**
 * Convert a [StoreResult] instance into low-level [Container].
 */
public fun <T> StoreResult<T>.toContainer(): Container<T> {
    return when (this) {
        is StoreResult.Failed -> errorContainer(exception, metadata)
        is StoreResult.Loaded -> successContainer(value, metadata)
        StoreResult.Loading -> pendingContainer()
    }
}

/**
 * Merge metadata from [origin] result to [this] result.
 */
public fun <T, R> StoreResult<T>.withMetadataFrom(
    origin: StoreResult<R>,
): StoreResult<T> {
    val combiner = CombineContainerFlowScope.create(listOf(origin.toContainer(), this.toContainer()))
    return this.toContainer().update {
        metadata = origin.metadata
        sourceType = combiner.sourceType
        backgroundLoadState = combiner.backgroundLoadState
        reloadFunction = combiner.reloadFunction
    }.toStoreResult()
}
