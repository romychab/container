// list indices below mirror the positional type parameters of each overload
@file:Suppress("UNCHECKED_CAST", "MagicNumber")

package com.elveum.store.load

import com.elveum.container.combineContainerFlows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Combine 2 flows emitting [StoreResult] into one flow. Success states are combined,
 * failures and loading states are not changed.
 */
public fun <T1, T2, R> combineStores(
    result1: Flow<StoreResult<T1>>,
    result2: Flow<StoreResult<T2>>,
    transform: (T1, T2) -> R,
): Flow<StoreResult<R>> {
    return combineStores(listOf(result1, result2)) {
        transform(it[0] as T1, it[1] as T2)
    }
}

/**
 * Combine 3 flows emitting [StoreResult] into one flow. Success states are combined,
 * failures and loading states are not changed.
 */
public fun <T1, T2, T3, R> combineStores(
    result1: Flow<StoreResult<T1>>,
    result2: Flow<StoreResult<T2>>,
    result3: Flow<StoreResult<T3>>,
    transform: (T1, T2, T3) -> R,
): Flow<StoreResult<R>> {
    return combineStores(listOf(result1, result2, result3)) {
        transform(it[0] as T1, it[1] as T2, it[2] as T3)
    }
}

/**
 * Combine 4 flows emitting [StoreResult] into one flow. Success states are combined,
 * failures and loading states are not changed.
 */
public fun <T1, T2, T3, T4, R> combineStores(
    result1: Flow<StoreResult<T1>>,
    result2: Flow<StoreResult<T2>>,
    result3: Flow<StoreResult<T3>>,
    result4: Flow<StoreResult<T4>>,
    transform: (T1, T2, T3, T4) -> R,
): Flow<StoreResult<R>> {
    return combineStores(listOf(result1, result2, result3, result4)) {
        transform(it[0] as T1, it[1] as T2, it[2] as T3, it[3] as T4)
    }
}

/**
 * Combine 5 flows emitting [StoreResult] into one flow. Success states are combined,
 * failures and loading states are not changed.
 */
public fun <T1, T2, T3, T4, T5, R> combineStores(
    result1: Flow<StoreResult<T1>>,
    result2: Flow<StoreResult<T2>>,
    result3: Flow<StoreResult<T3>>,
    result4: Flow<StoreResult<T4>>,
    result5: Flow<StoreResult<T5>>,
    transform: (T1, T2, T3, T4, T5) -> R,
): Flow<StoreResult<R>> {
    return combineStores(listOf(result1, result2, result3, result4, result5)) {
        transform(it[0] as T1, it[1] as T2, it[2] as T3, it[3] as T4, it[4] as T5)
    }
}

/**
 * Combine any number of flows emitting [StoreResult] into one flow. Success states are combined,
 * failures and loading states are not changed.
 */
public fun <R> combineStores(
    flows: List<Flow<StoreResult<*>>>,
    transform: (List<*>) -> R,
): Flow<StoreResult<R>> {
    val containerFlows = flows.map { flow -> flow.map { it.toContainer() } }
    return combineContainerFlows(containerFlows) {
        transform(it)
    }.map { it.toStoreResult() }
}
