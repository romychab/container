package com.elveum.container.subject

import com.elveum.container.Emitter
import java.util.concurrent.atomic.AtomicBoolean


internal class OneShotValueLoader<T>(
    val previousValueLoader: ValueLoader<T>,
    val valueLoader: ValueLoader<T>
) : ValueLoader<T> {

    private val isLaunched = AtomicBoolean(false)

    override suspend fun invoke(emitter: Emitter<T>) {
        if (isLaunched.compareAndSet(false, true)) {
            valueLoader(emitter)
        } else {
            previousValueLoader(emitter)
        }
    }

}
