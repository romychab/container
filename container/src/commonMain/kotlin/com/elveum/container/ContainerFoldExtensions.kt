@file:OptIn(ExperimentalContracts::class)

package com.elveum.container

import kotlin.contracts.ExperimentalContracts

public typealias ListContainer<T> = Container<List<T>>

/**
 * - Returns the result of onSuccess() function if this instance is [Container.Success].
 *   If you don't provide onSuccess() implementation, the [defaultValue] is returned.
 * - Returns the result of onError() function if this instance is [Container.Error].
 *   If you don't provide onError() implementation, the [defaultValue] is returned.
 * - Returns the result of onPending() function if this instance is [Container.Pending].
 *   If you don't provide onPending() implementation, the [defaultValue] is returned.
 */
public inline fun <T, R> Container<T>.foldDefault(
    defaultValue: R,
    onPending: () -> R = { defaultValue },
    onError: ContainerMapperScope.(Exception) -> R = { defaultValue },
    onSuccess: ContainerMapperScope.(T) -> R = { defaultValue },
): R {
    return fold(
        onPending = { onPending() },
        onError = { onError(it) },
        onSuccess = { onSuccess(it) },
    )
}

/**
 * - Returns the result of onSuccess() function if this instance is [Container.Success].
 *   If you don't provide onSuccess() implementation, `null` is returned.
 * - Returns the result of onError() function if this instance is [Container.Error].
 *   If you don't provide onError() implementation, `null` is returned.
 * - Returns the result of onPending() function if this instance is [Container.Pending].
 *   If you don't provide onPending() implementation, `null` is returned.
 */
public inline fun <T, R> Container<T>.foldNullable(
    onPending: () -> R? = { null },
    onError: ContainerMapperScope.(Exception) -> R? = { null },
    onSuccess: ContainerMapperScope.(T) -> R? = { null },
): R? {
    return fold(
        onSuccess = { onSuccess(it) },
        onError = { onError(it) },
        onPending = { onPending() },
    )
}
