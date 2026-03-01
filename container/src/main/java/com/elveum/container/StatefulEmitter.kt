package com.elveum.container

public interface StatefulEmitter<T> : Emitter<T> {

    public suspend fun emitPendingState()

    public suspend fun emitCompletedState()

    public suspend fun emitFailureState(exception: Exception)

    public fun <T> saveState(key: String, state: T)

    public fun <T> restoreState(key: String): T?

}
