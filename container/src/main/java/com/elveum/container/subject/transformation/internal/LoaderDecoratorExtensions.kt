package com.elveum.container.subject.transformation.internal

import com.elveum.container.StatefulEmitter
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.subject.transformation.LoaderDecorator

internal suspend fun LoaderDecorator.executeOn(
    emitter: StatefulEmitter<*>,
    onStart: suspend () -> Unit = emitter::emitPendingState,
    onComplete: suspend () -> Unit = emitter::emitDefaultCompletedState,
    onError: suspend (Exception) -> Unit = emitter::emitFailureState,
    onTerminated: suspend () -> Unit = {},
    originLoader: suspend () -> Unit,
) {
    val composer = DecoratedFlowComposerImpl(originComposer = emitter)
    onStart()
    try {
        composer.decorate(originLoader)
        onComplete()
    } catch (e: TerminatedDecorationException) {
        val terminationContainer = when (e.result) {
            TerminatedDecorationResult.ClearCache -> pendingContainer()
            is TerminatedDecorationResult.Failure -> errorContainer(e.result.exception)
        }
        emitter.terminateWith(terminationContainer)
        onTerminated()
    } catch (e: Exception) {
        onError(e)
    }
}

private suspend fun StatefulEmitter<*>.emitDefaultCompletedState() {
    if (!hasEmittedValues) throwNoEmittedItemsException()
    emitCompletedState()
}

private fun throwNoEmittedItemsException(): Nothing {
    throw IllegalStateException("Value Loader should emit at least one item or " +
            "throw exception. If you don't want to emit values (e.g. it's okay for " +
            "you to have an infinite Container.Pending state), you can call " +
            "awaitCancellation() in the end of your loader function.")
}
