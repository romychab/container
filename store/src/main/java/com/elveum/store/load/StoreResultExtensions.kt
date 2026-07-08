@file:OptIn(ExperimentalContracts::class)

package com.elveum.store.load

import com.elveum.container.BackgroundLoadState
import kotlinx.coroutines.CancellationException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Map the [StoreResult] of type [T] into a new [StoreResult] of type [R]
 * using the [mapper] function.
 */
public inline fun <T, R> StoreResult<T>.map(mapper: (T) -> R): StoreResult<R> {
    return when (this) {
        is StoreResult.Failed -> this
        is StoreResult.Loaded -> {
            try {
                StoreResult.Loaded(mapper(this.value), this.metadata)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                StoreResult.Failed(e, this.metadata)
            }
        }
        is StoreResult.Loading -> StoreResult.Loading
    }
}

/**
 * Convenient call for extracting successful value from the store result (if any; otherwise
 * `null` is returned).
 */
public fun <T> StoreResult<T>.getOrNull(): T? {
    return (this as? StoreResult.Loaded<T>)?.value
}

/**
 * Convenient call for extracting a failure from the store result (if any; otherwise
 * `null` is returned).
 */
public fun <T> StoreResult<T>.failureOrNull(): Exception? {
    return (this as? StoreResult.Failed)?.exception
}

/**
 * Whether the current [StoreResult] contains already loaded value.
 *
 * Compatible with Kotlin smart-casts:
 *
 * ```
 * if (result.isLoaded()) {
 *     println(result.value)
 * }
 * ```
 */
public fun <T> StoreResult<T>.isLoaded(): Boolean {
    contract {
        returns(true) implies (this@isLoaded is StoreResult.Loaded<T>)
    }
    return this is StoreResult.Loaded<T>
}

/**
 * Whether the current [StoreResult] represents a loaded state - either error, or success.
 */
public fun <T> StoreResult<T>.isCompleted(): Boolean {
    contract {
        returns(true) implies (this@isCompleted is StoreResult.Completed<T>)
    }
    return this is StoreResult.Completed<T>
}

/**
 * Whether the current [StoreResult] represents a failed load.
 *
 * Compatible with Kotlin smart-casts:
 *
 * ```
 * if (result.isFailed()) {
 *     println(result.exception)
 * }
 * ```
 */
public fun <T> StoreResult<T>.isFailed(): Boolean {
    contract {
        returns(true) implies (this@isFailed is StoreResult.Failed)
    }
    return this is StoreResult.Failed
}

/**
 * Whether the current [StoreResult] represents in-progress loading state displayed
 * instead of any content.
 */
public fun <T> StoreResult<T>.isForegroundLoading(): Boolean {
    contract {
        returns(true) implies (this@isForegroundLoading is StoreResult.Loading)
    }
    return this is StoreResult.Loading
}

/**
 * Whether the current [StoreResult] represents background loading state in addition
 * to other completed state.
 */
public fun <T> StoreResult<T>.isBackgroundLoading(): Boolean =
    isCompleted() && backgroundLoadState == BackgroundLoadState.Loading

/**
 * Whether the current [StoreResult] represents any loading state (either background,
 * or foreground).
 */
public fun <T> StoreResult<T>.hasAnyLoading(): Boolean = isForegroundLoading() || isBackgroundLoading()

/**
 * Exclude all metadata from the [StoreResult]. Useful for comparing results and testing.
 */
public fun <T> StoreResult<T>.raw(): StoreResult<T> {
    return when (this) {
        is StoreResult.Failed -> StoreResult.Failed(exception)
        is StoreResult.Loaded -> StoreResult.Loaded(value)
        StoreResult.Loading -> StoreResult.Loading
    }
}
