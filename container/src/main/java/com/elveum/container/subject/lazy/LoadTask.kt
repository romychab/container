package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.pendingContainer
import com.elveum.container.isReloadDependencies
import com.elveum.container.subject.FlowSubject
import com.elveum.container.subject.StatefulValueLoader
import com.elveum.container.subject.ValueLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
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
        val currentContainer: () -> Container<T> = { pendingContainer() },
    )

    open class FlowEmitterCreator<T>(
        protected val flowSubject: FlowSubject<T>?,
        protected val metadata: ContainerMetadata,
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
        ) = channelFlow {
            val channelFlowCollector = ChannelFlowCollector<Container<T>>(this)
            val emitter = flowEmitterCreator.create(channelFlowCollector, executeParams)
            val statefulEmitter = StatefulEmitterImpl(
                emitter = emitter,
                executeParams = executeParams,
                loadConfig = config,
                flowCollector = channelFlowCollector,
                flowSubject = flowSubject,
            )
            val statefulLoader = StatefulValueLoader.wrap(loader)
            try {
                executeParams.flowDependencyStore.begin(
                    reloadDependencies = metadata.isReloadDependencies,
                    loadConfig = config,
                )
                with(statefulLoader) { statefulEmitter.statefulInvoke() }
            } finally {
                executeParams.flowDependencyStore.end()
            }
        }.conflate()

        override fun cancel(reason: String) {
            flowSubject?.onError(CancellationException(reason))
        }

        override fun restoreLoadTask(metadata: ContainerMetadata): LoadTask<T> {
            return LoadTask.Load(
                metadata = this.metadata + metadata,
                loader = loader,
            )
        }

    }

}
