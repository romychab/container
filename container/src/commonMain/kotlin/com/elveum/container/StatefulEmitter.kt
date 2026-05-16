package com.elveum.container

public interface StatefulEmitter<T> : Emitter<T> {

    public val hasEmittedValues: Boolean

    public suspend fun emitPendingState()

    public suspend fun emitCompletedState()

    public suspend fun emitFailureState(exception: Exception)

}
