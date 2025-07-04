package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.LoadTrigger
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
    val initialContainer: Container<T>?
    val lastRealLoader: ValueLoader<T>?
    fun execute(currentContainer: () -> Container<T>? = { null }): Flow<Container<T>>
    fun cancel()
    fun setLoadTrigger(loadTrigger: LoadTrigger)

    class Instant<T>(
        override val initialContainer: Container<T>,
        override val lastRealLoader: ValueLoader<T>? = null,
    ) : LoadTask<T> {
        override fun execute(currentContainer: () -> Container<T>?) = flowOf(initialContainer)
        override fun cancel() = Unit
        override fun setLoadTrigger(loadTrigger: LoadTrigger) = Unit
    }

    class Load<T> private constructor(
        private val loader: ValueLoader<T>,
        private val loadTrigger: Holder<LoadTrigger>,
        private val silent: Boolean = false,
        private val flowSubject: FlowSubject<T>? = null,
        private val flowEmitterCreator: (FlowCollector<Container<T>>) -> FlowEmitter<T> = {
            FlowEmitter(loadTrigger.value, it, flowSubject)
        },
    ) : LoadTask<T> {

        override val initialContainer: Container<T>? =
            if (silent) null else Container.Pending

        override val lastRealLoader: ValueLoader<T> = loader

        constructor(
            loader: ValueLoader<T>,
            loadTrigger: LoadTrigger,
            silent: Boolean = false,
            flowSubject: FlowSubject<T>? = null,
        ) : this(loader, Holder(loadTrigger), silent, flowSubject)

        constructor(
            loader: ValueLoader<T>,
            loadTrigger: LoadTrigger,
            silent: Boolean = false,
            flowSubject: FlowSubject<T>? = null,
            flowEmitterCreator: (FlowCollector<Container<T>>) -> FlowEmitter<T>,
        ) : this(loader, Holder(loadTrigger), silent, flowSubject, flowEmitterCreator)

        override fun setLoadTrigger(loadTrigger: LoadTrigger) {
            this.loadTrigger.value = loadTrigger
        }

        override fun execute(currentContainer: () -> Container<T>?) = flow {
            try {
                if (!silent) {
                    emit(Container.Pending)
                } else {
                    currentContainer()?.update(isLoadingInBackground = true)?.let {
                        emit(it)
                    }
                }
                val emitter = flowEmitterCreator(this)
                loader(emitter)
                if (!emitter.hasEmittedValues) {
                    throw IllegalStateException("Value Loader should emit at least one item or throw exception")
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

        private class Holder<T>(var value: T)
    }

}
