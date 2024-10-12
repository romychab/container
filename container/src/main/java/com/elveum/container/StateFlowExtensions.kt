package com.elveum.container

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Cast [MutableStateFlow] to [StateFlow].
 */
public fun <T> MutableStateFlow<T>.public(): StateFlow<T> {
    return this
}

/**
 * Set a new [value] to the [StateFlow] if it is [MutableStateFlow].
 * Otherwise, do nothing.
 */
public fun <T> StateFlow<T>.tryUpdate(value: T) {
    (this as? MutableStateFlow)?.value = value
}

/**
 * Calculate a new value for the [StateFlow] by using [updater] lambda
 * function if this [StateFlow] is [MutableStateFlow]. Otherwise, do nothing.
 */
public inline fun <T> StateFlow<T>.tryUpdate(updater: (T) -> T) {
    if (this is MutableStateFlow) {
        update(updater)
    }
}
