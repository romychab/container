package com.elveum.container

import com.elveum.container.internal.Lock
import com.elveum.container.internal.withLock

/**
 * Function that caches the latest result. It returns immediately the cached
 * results if you pass the same [Input] arg as you did in the previous call.
 *
 * Input args are compared by [equals].
 */
public class CachingFunction<Input, T>(
    private val function: (Input) -> T,
) {

    private val lock = Lock()
    private var lastInput: InputValue<Input> = InputValue.Unset
    private var cachedResult: Container<T> = pendingContainer()

    public operator fun invoke(input: Input): T = lock.withLock {
        val lastInput = this.lastInput
        val cachedResult = this.cachedResult
        if (lastInput is InputValue.Set
                && input == lastInput.value
                && cachedResult is Container.Success) {
            cachedResult.value
        } else {
            function(input).also {
                this.lastInput = InputValue.Set(input)
                this.cachedResult = successContainer(it)
            }
        }
    }

    private sealed class InputValue<out T> {
        data object Unset : InputValue<Nothing>()
        data class Set<T>(val value: T) : InputValue<T>()
    }
}
