package com.elveum.container.reducer.owner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

/**
 * Convert a cold flow into a hot [StateFlow] using [CoroutineScope]
 * and [SharingStarted] values provided by [ReducerOwner].
 */
context(owner: ReducerOwner)
public fun <T> Flow<T>.stateIn(initialValue: T): StateFlow<T> {
    return stateIn(
        scope = owner.reducerScope,
        started = owner.sharingStarted,
        initialValue = initialValue,
    )
}

/**
 * Convert a cold flow into a hot [SharedFlow] using [CoroutineScope]
 * and [SharingStarted] values provided by [ReducerOwner].
 */
context(owner: ReducerOwner)
public fun <T> Flow<T>.shareIn(replay: Int = 0): SharedFlow<T> {
    return shareIn(
        scope = owner.reducerScope,
        started = owner.sharingStarted,
        replay = replay,
    )
}
