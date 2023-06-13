package com.elveum.container

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

interface Emitter<T> {

    suspend fun emit(item: T, source: SourceIndicator = UnknownSourceIndicator)

}

/**
 * Load a value by using the [loader] function and return a flow which
 * emits the status of loading operation.
 *
 * While the load is in progress, [Container.Pending] is emitted.
 * If the load finished successfully, [Container.Success] is emitted and flow completes.
 * If the load failed, [Container.Error] is emitted and flow completes.
 *
 * The returned flow is cold. This means that data loading is started
 * each time when someone starts collecting the flow.
 */
fun <T> containerOf(
    source: SourceIndicator = UnknownSourceIndicator,
    loader: suspend () -> T
): Flow<Container<T>> {
    return containerOfMany { emit(loader(), source) }
}

/**
 * Load a couple of values by using the [loader] function and return a flow
 * which emits the status of loading operation.
 *
 * While the load is in progress and no values are loaded, [Container.Pending] is
 * emitted. After the first call of `emit()` method inside the [loader],
 * [Container.Success] is emitted.
 *
 * If the load fails with exception, [Container.Error] is emitted and the returned flow completes.
 *
 * If the [loader] completes successfully, the returned flow also completes.
 *
 * The returned flow is cold. This means that data loading is started
 * each time when someone starts collecting the flow.
 */
fun <T> containerOfMany(loader: suspend Emitter<T>.() -> Unit): Flow<Container<T>> {
    return flow {
        try {
            val emitter = object : Emitter<T> {
                override suspend fun emit(item: T, source: SourceIndicator) {
                    val collector = this@flow
                    collector.emit(Container.Success(item, source))
                }
            }
            emit(Container.Pending)
            emitter.loader()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            emit(Container.Error(e))
        }
    }
}
