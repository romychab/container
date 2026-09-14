package com.elveum.container.reducer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

/**
 * Convert a cold flow into a hot [StateFlow] using [kotlinx.coroutines.CoroutineScope]
 * and [kotlinx.coroutines.flow.SharingStarted] values provided by [ReducerOwner].
 */
context(owner: ReducerOwner)
public fun <T> Flow<T>.stateIn(initialValue: T): StateFlow<T> {
    return stateIn(
        scope = owner.reducerCoroutineScope,
        started = owner.reducerSharingStarted,
        initialValue = initialValue,
    )
}

/**
 * Convert a cold flow into a hot [SharedFlow] using [kotlinx.coroutines.CoroutineScope]
 * and [kotlinx.coroutines.flow.SharingStarted] values provided by [ReducerOwner].
 */
context(owner: ReducerOwner)
public fun <T> Flow<T>.shareIn(replay: Int = 0): SharedFlow<T> {
    return shareIn(
        scope = owner.reducerCoroutineScope,
        started = owner.reducerSharingStarted,
        replay = replay,
    )
}
