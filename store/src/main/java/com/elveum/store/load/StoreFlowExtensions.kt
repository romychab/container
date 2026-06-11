@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.store.load

import com.elveum.store.internal.load.withMetadataFrom
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Convert the Flow with [StoreResult] of type [T] into a new Flow with [StoreResult]
 * of type [R].
 */
public fun <T, R> Flow<StoreResult<T>>.storeMap(mapper: (T) -> R): Flow<StoreResult<R>> {
    return map { result ->
        result.map(mapper)
    }
}

/**
 * Observe the latest [StoreResult] of type [T] and for each successful result
 * observe a new flow returned by [mapper] function.
 *
 * This function is useful when fetching parts of data from different flows which
 * depends on each other (for example: basic info + details from other flow, 1-N relations, etc.)
 */
public fun <T, R> Flow<StoreResult<T>>.storeFlatMapResultLatest(
    mapper: (T) -> Flow<StoreResult<R>>,
): Flow<StoreResult<R>> {
    return flatMapLatest { originResult ->
        when (originResult) {
            is StoreResult.Failed -> flowOf(StoreResult.Failed(originResult.exception, originResult.metadata))
            StoreResult.Loading -> flowOf(StoreResult.Loading)
            is StoreResult.Loaded -> {
                try {
                    mapper(originResult.value)
                        .map { newResult -> newResult.withMetadataFrom(originResult) }
                        .catch { throwable ->
                            if (throwable is Exception) {
                                emit(StoreResult.Failed(throwable, originResult.metadata))
                            } else {
                                throw throwable
                            }
                        }
                } catch (e: Exception) {
                    currentCoroutineContext().ensureActive()
                    flowOf(StoreResult.Failed(e))
                }
            }
        }
    }
}

/**
 * The same as [storeFlatMapResultLatest], but for inner flows that return just a plain
 * value instead of [StoreResult].
 */
public fun <T, R> Flow<StoreResult<T>>.storeFlatMapLatest(
    mapper: (T) -> Flow<R>,
): Flow<StoreResult<R>> {
    return storeFlatMapResultLatest { input ->
        mapper(input).map { StoreResult.Loaded(it) }
    }
}

/**
 * For each item in the list emitted by the origin flow, observe an inner flow returned
 * by the [observer] function and then merge the item with additional data provided by
 * inner flow.
 *
 * In short, this function fetches items plus details for each item.
 */
public inline fun <T, V, reified R> Flow<StoreResult<List<T>>>.storeListFlatMapLatest(
    crossinline observer: (T) -> Flow<StoreResult<V>>,
    crossinline mapper: (T, StoreResult<V>) -> R
): Flow<StoreResult<List<R>>> {
    return storeFlatMapLatest { items ->
        if (items.isEmpty()) {
            flowOf(emptyList())
        } else {
            val itemFlows = items.map { item ->
                observer(item).map { result -> mapper(item, result) }
            }
            combine<R, List<R>>(
                flows = itemFlows,
                transform = { it.toList() }
            )
        }
    }
}
