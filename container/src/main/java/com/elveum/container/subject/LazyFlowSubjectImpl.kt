package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.Emitter
import com.elveum.container.SourceIndicator
import com.elveum.container.subject.factories.LoadingScopeFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

// todo: thing about re-writing the implementation to SharedFlow

public interface Creator {
    public fun <T> createDefault(
        loadingDispatcher: CoroutineDispatcher,
        cacheTimeoutMillis: Long,
        loadingScopeFactory: LoadingScopeFactory? = null,
    ): LazyFlowSubject<T>
}

internal object CreatorImpl : Creator {
    override fun <T> createDefault(
        loadingDispatcher: CoroutineDispatcher,
        cacheTimeoutMillis: Long,
        loadingScopeFactory: LoadingScopeFactory?
    ): LazyFlowSubject<T> {
        return LazyFlowSubjectImpl(
            loadingScopeFactory = loadingScopeFactory ?:
                LoadingScopeFactory {
                    CoroutineScope(SupervisorJob() + it)
                },
            loadingDispatcher = loadingDispatcher,
            cacheTimeoutMillis = cacheTimeoutMillis,
        )
    }
}

internal class LazyFlowSubjectImpl<T>(
    private val loadingScopeFactory: LoadingScopeFactory,
    private val loadingDispatcher: CoroutineDispatcher,
    private val cacheTimeoutMillis: Long,
) : LazyFlowSubject<T> {

    private var count = 0
    private var scope: CoroutineScope? = null
    private var cancellationJob: Job? = null
    private var isLoadingFlow = MutableStateFlow(false)
    private var inputFlow = MutableStateFlow<LoadTask<T>>(LoadTask.Instant(Container.Pending))
    private val outputFlow = MutableStateFlow<Container<T>>(Container.Pending)

    override fun listen(): Flow<Container<T>> = callbackFlow {
        onStart()

        val job = scope?.launch {
            outputFlow.collect {
                trySend(it)
            }
        }

        awaitClose {
            onStop(job)
        }
    }

    override fun newLoad(
        silently: Boolean,
        once: Boolean,
        valueLoader: ValueLoader<T>
    ): Flow<T> = synchronized(this) {
        val flowSubject = FlowSubject.create<T>()
        val newTask = LoadTask.Load(buildNewValueLoader(once, valueLoader), silently, flowSubject)
        processLoadCancellation()
        inputFlow.value = newTask
        return flowSubject.flow()
    }

    override fun newAsyncLoad(
        silently: Boolean,
        once: Boolean,
        valueLoader: ValueLoader<T>
    ) = synchronized(this) {
        val newTask = LoadTask.Load(buildNewValueLoader(once, valueLoader), silently)
        processLoadCancellation()
        inputFlow.value = newTask
    }

    override fun updateWith(container: Container<T>) = synchronized(this) {
        processLoadCancellation()
        inputFlow.value = LoadTask.Instant(container, getLastRealLoader())
    }

    override fun updateWith(updater: (Container<T>) -> Container<T>) = synchronized(this) {
        val oldValue = outputFlow.value
        val newValue = updater(oldValue)
        if (newValue === oldValue) return@synchronized
        processLoadCancellation()
        inputFlow.value = LoadTask.Instant(newValue, getLastRealLoader())
    }

    private fun onStart() = synchronized(this) {
        count++
        if (count == 1) {
            cancellationJob?.cancel()
            cancellationJob = null
            startLoading()
        }
    }

    override fun isValueLoading(): StateFlow<Boolean> {
        return isLoadingFlow
    }

    private fun onStop(job: Job?) = synchronized(this) {
        count--
        job?.cancel()
        if (count == 0) {
            scheduleStopLoading()
        }
    }

    @Suppress("OPT_IN_USAGE")
    private fun startLoading() {
        if (scope != null) return

        outputFlow.value = Container.Pending
        scope = loadingScopeFactory.createScope(loadingDispatcher)
        scope?.launch {
            inputFlow
                .flatMapLatest { loadTask -> doLoad(loadTask) }
                .collectLatest {
                    outputFlow.value = it
                }
        }
    }

    private fun scheduleStopLoading() {
        cancellationJob = scope?.launch {
            delay(cacheTimeoutMillis)
            synchronized(this@LazyFlowSubjectImpl) {
                cancellationJob = null
                if (count == 0) { // double check required
                    outputFlow.value = Container.Pending
                    scope?.cancel()
                    scope = null

                    val currentLoad = inputFlow.value
                    if (currentLoad is LoadTask.Instant && currentLoad.lastLoader != null) {
                        inputFlow.value = LoadTask.Load(currentLoad.lastLoader)
                    }
                }
            }
        }
    }

    private fun doLoad(loadTask: LoadTask<T>): Flow<Container<T>> {
        return when (loadTask) {
            is LoadTask.Instant -> flowOf(loadTask.container)
            is LoadTask.Load -> flow {
                try {
                    if (!loadTask.silent) {
                        isLoadingFlow.value = true
                        emit(Container.Pending)
                    }
                    val emitter = FlowEmitter(this, loadTask.flowSubject)
                    loadTask.loader(emitter)
                    if (!emitter.hasEmittedItems) {
                        throw IllegalStateException("Value Loader should emit at least one item or throw exception")
                    }
                    loadTask.flowSubject?.onComplete()
                } catch (e: Throwable) {
                    isLoadingFlow.value = false
                    if (e is CancellationException) throw e
                    loadTask.flowSubject?.onError(e)
                    emit(Container.Error(e))
                }
            }
        }
    }

    private fun processLoadCancellation() {
        val loadTask = inputFlow.value
        if (loadTask is LoadTask.Load) {
            loadTask.flowSubject?.onError(LoadCancelledException())
        }
    }

    private fun getLastRealLoader(): ValueLoader<T>? {
        return when (val load = inputFlow.value) {
            is LoadTask.Instant -> load.lastLoader
            is LoadTask.Load -> load.loader
        }
    }

    private fun buildNewValueLoader(once: Boolean, valueLoader: ValueLoader<T>): ValueLoader<T> {
        if (!once) return valueLoader
        if (count == 0) return valueLoader
        val lastLoader = when (val currentLoadTask = inputFlow.value) {
            is LoadTask.Instant -> currentLoadTask.lastLoader
            is LoadTask.Load -> currentLoadTask.loader
        } ?: return valueLoader
        val lastRealLoader = if (lastLoader is OneShotValueLoader) {
            lastLoader.previousValueLoader
        } else {
            lastLoader
        }
        return OneShotValueLoader(lastRealLoader, valueLoader)
    }

    sealed class LoadTask<T> {
        class Instant<T> constructor(
            val container: Container<T>,
            val lastLoader: ValueLoader<T>? = null,
        ) : LoadTask<T>()
        class Load<T>(
            val loader: ValueLoader<T>,
            val silent: Boolean = false,
            val flowSubject: FlowSubject<T>? = null,
        ) : LoadTask<T>()
    }

    private class FlowEmitter<T>(
        private val flowCollector: FlowCollector<Container<T>>,
        private val flowSubject: FlowSubject<T>? = null,
    ) : Emitter<T> {

        private var _hasEmittedItems = false
        val hasEmittedItems get() = _hasEmittedItems

        override suspend fun emit(item: T, source: SourceIndicator) {
            flowSubject?.onNext(item)
            flowCollector.emit(Container.Success(item, source))
            _hasEmittedItems = true
        }
    }
}