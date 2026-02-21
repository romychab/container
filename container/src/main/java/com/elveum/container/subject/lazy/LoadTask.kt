package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.errorContainer
import com.elveum.container.subject.FlowSubject
import com.elveum.container.subject.ValueLoader
import com.elveum.container.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

internal interface LoadTask<T> {
    val metadata: ContainerMetadata
    val initialContainer: Container<T>?
    val lastRealLoader: ValueLoader<T>?
    val lastRealMetadata: ContainerMetadata
    fun execute(executeParams: ExecuteParams<T> = ExecuteParams()): Flow<Container<T>>
    fun cancel()
    fun restoreLoadTask(metadata: ContainerMetadata): LoadTask<T>

    class ExecuteParams<T>(
        val loadUuid: String = "",
        val currentContainer: () -> Container<T>? = { null }
    )

    open class FlowEmitterCreator<T>(
        private val flowSubject: FlowSubject<T>?,
        private val metadata: ContainerMetadata,
    ) {
        open fun create(
            flowCollector: FlowCollector<Container<T>>,
            executeParams: ExecuteParams<T>,
        ): FlowEmitter<T> {
            return FlowEmitter(
                metadata = metadata,
                flowCollector = flowCollector,
                executeParams = executeParams,
                flowSubject = flowSubject,
            )
        }
    }

    class Instant<T>(
        override val initialContainer: Container<T>,
        override val lastRealLoader: ValueLoader<T>? = null,
        override val lastRealMetadata: ContainerMetadata = EmptyMetadata,
    ) : LoadTask<T> {
        override val metadata: ContainerMetadata = initialContainer.metadata
        override fun execute(executeParams: ExecuteParams<T>) = flowOf(initialContainer)
        override fun cancel() = Unit
        override fun restoreLoadTask(metadata: ContainerMetadata): LoadTask<T> {
            return if (lastRealLoader == null) {
                this
            } else {
                Load(lastRealLoader, lastRealMetadata + metadata)
            }
        }
    }

    class Load<T> private constructor(
        override val metadata: ContainerMetadata,
        private val loader: ValueLoader<T>,
        private val silent: Boolean = false,
        private val flowSubject: FlowSubject<T>? = null,
        private val flowEmitterCreator: FlowEmitterCreator<T> = FlowEmitterCreator(flowSubject, metadata),
    ) : LoadTask<T> {

        override val initialContainer: Container<T>? =
            if (silent) null else Container.Pending

        override val lastRealLoader: ValueLoader<T> = loader
        override val lastRealMetadata: ContainerMetadata = metadata

        constructor(
            loader: ValueLoader<T>,
            metadata: ContainerMetadata,
            silent: Boolean = false,
            flowSubject: FlowSubject<T>? = null,
        ) : this(metadata, loader, silent, flowSubject)

        constructor(
            loader: ValueLoader<T>,
            metadata: ContainerMetadata,
            silent: Boolean = false,
            flowSubject: FlowSubject<T>? = null,
            flowEmitterCreator: FlowEmitterCreator<T>,
        ) : this(metadata, loader, silent, flowSubject, flowEmitterCreator)

        override fun execute(
            executeParams: ExecuteParams<T>,
        ) = flow {
            try {
                if (!silent) {
                    emit(Container.Pending)
                } else {
                    executeParams.currentContainer()?.update(isLoadingInBackground = true)?.let {
                        emit(it)
                    }
                }
                val emitter = flowEmitterCreator.create(this, executeParams)
                loader(emitter)
                if (!emitter.hasEmittedValues) {
                    throw IllegalStateException("Value Loader should emit at least one item or " +
                            "throw exception. If you don't want to emit values (e.g. it's okay for " +
                            "you to have an infinite Container.Pending state), you can call " +
                            "awaitCancellation() in the end of your loader function.")
                }
                flowSubject?.onComplete()
                emitter.emitLastItem()
            } catch (e: Exception) {
                flowSubject?.onError(e)
                if (e is CancellationException) throw e
                emit(errorContainer(e))
            }
        }

        override fun cancel() {
            flowSubject?.onError(CancellationException())
        }

        override fun restoreLoadTask(metadata: ContainerMetadata): LoadTask<T> {
            return LoadTask.Load(
                metadata = this.metadata + metadata,
                loader = loader,
            )
        }

    }

}
