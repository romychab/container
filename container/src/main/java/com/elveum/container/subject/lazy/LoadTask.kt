package com.elveum.container.subject.lazy

import com.elveum.container.BackgroundLoadState
import com.elveum.container.BackgroundLoadMetadata
import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.reloadDependencies
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
    fun execute(executeParams: ExecuteParams<T>): Flow<Container<T>>
    fun cancel(reason: String)
    fun restoreLoadTask(metadata: ContainerMetadata): LoadTask<T>

    class ExecuteParams<T>(
        val flowDependencyStore: FlowDependencyStore,
        val currentContainer: () -> Container<T> = { pendingContainer() }
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
        override fun cancel(reason: String) = Unit
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
        private val config: LoadConfig = LoadConfig.Normal,
        private val flowSubject: FlowSubject<T>? = null,
        private val flowEmitterCreator: FlowEmitterCreator<T> = FlowEmitterCreator(flowSubject, metadata),
    ) : LoadTask<T> {

        override val initialContainer: Container<T>? =
            if (config.isSilentLoadingEnabled) null else Container.Pending

        override val lastRealLoader: ValueLoader<T> = loader
        override val lastRealMetadata: ContainerMetadata = metadata

        constructor(
            loader: ValueLoader<T>,
            metadata: ContainerMetadata,
            config: LoadConfig = LoadConfig.Normal,
            flowSubject: FlowSubject<T>? = null,
        ) : this(metadata, loader, config, flowSubject)

        constructor(
            loader: ValueLoader<T>,
            metadata: ContainerMetadata,
            config: LoadConfig = LoadConfig.Normal,
            flowSubject: FlowSubject<T>? = null,
            flowEmitterCreator: FlowEmitterCreator<T>,
        ) : this(metadata, loader, config, flowSubject, flowEmitterCreator)

        override fun execute(
            executeParams: ExecuteParams<T>,
        ) = flow {
            try {
                handleSilentLoadingConfig(executeParams)
                val emitter = flowEmitterCreator.create(this, executeParams)
                try {
                    executeParams.flowDependencyStore.begin(
                        reloadDependencies = metadata.reloadDependencies,
                        loadConfig = config,
                    )
                    loader(emitter)
                } finally {
                    executeParams.flowDependencyStore.end()
                }
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
                handleSilentErrorConfig(executeParams, e)
            }
        }

        override fun cancel(reason: String) {
            flowSubject?.onError(CancellationException(reason))
        }

        override fun restoreLoadTask(metadata: ContainerMetadata): LoadTask<T> {
            return LoadTask.Load(
                metadata = this.metadata + metadata,
                loader = loader,
            )
        }

        private suspend fun FlowCollector<Container<T>>.handleSilentLoadingConfig(
            executeParams: ExecuteParams<T>,
        ) {
            if (!config.isSilentLoadingEnabled) {
                emit(Container.Pending)
            } else {
                executeParams.currentContainer()
                    .update { backgroundLoadState = BackgroundLoadState.Loading }
                    .let { emit(it) }
            }
        }

        private suspend fun FlowCollector<Container<T>>.handleSilentErrorConfig(
            executeParams: ExecuteParams<T>,
            exception: Exception,
        ) {
            if (config.isSilentErrorsEnabled) {
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

}
