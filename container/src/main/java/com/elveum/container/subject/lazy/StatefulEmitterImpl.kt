package com.elveum.container.subject.lazy

import com.elveum.container.BackgroundLoadMetadata
import com.elveum.container.BackgroundLoadState
import com.elveum.container.Container
import com.elveum.container.Emitter
import com.elveum.container.LoadConfig
import com.elveum.container.errorContainer
import com.elveum.container.subject.FlowSubject
import com.elveum.container.StatefulEmitter
import com.elveum.container.subject.lazy.LoadTask.ExecuteParams
import com.elveum.container.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.atomic.AtomicBoolean

internal class StatefulEmitterImpl<T>(
    private val emitter: FlowEmitter<T>,
    private val executeParams: ExecuteParams<T>,
    private val loadConfig: LoadConfig,
    private val flowCollector: FlowCollector<Container<T>>,
    private val flowSubject: FlowSubject<T>?,
) : StatefulEmitter<T>, Emitter<T> by emitter {

    private val isCompleted = AtomicBoolean(false)

    override val hasEmittedValues: Boolean get() = emitter.hasEmittedValues

    override suspend fun emitPendingState() = with(flowCollector) {
        if (!loadConfig.isSilentLoadingEnabled) {
            emit(Container.Pending)
        } else {
            executeParams.currentContainer()
                .update { backgroundLoadState = BackgroundLoadState.Loading }
                .let { emit(it) }
        }
    }

    override suspend fun emitCompletedState() {
        if (isCompleted.compareAndSet(false, true)) {
            flowSubject?.onComplete()
            emitter.emitLastItem()
        }
    }

    override suspend fun emitFailureState(exception: Exception) {
        if (isCompleted.compareAndSet(false, true)) {
            flowSubject?.onError(exception)
            if (exception is CancellationException) throw exception
            handleSilentErrorConfig(executeParams, exception)
        }
    }

    private suspend fun handleSilentErrorConfig(
        executeParams: ExecuteParams<T>,
        exception: Exception,
    ) = with(flowCollector) {
        if (loadConfig.isSilentErrorsEnabled) {
            executeParams.currentContainer()
                .let { currentContainer ->
                    if (currentContainer is Container.Success) {
                        val errorBackgroundMetadata = BackgroundLoadMetadata(BackgroundLoadState.Error(exception))
                        emit(currentContainer + errorBackgroundMetadata + metadata)
                    } else {
                        emit(errorContainer(exception, metadata))
                    }
                }
        } else {
            emit(errorContainer(exception, metadata))
        }
    }

}
