package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.SourceType
import com.elveum.container.UnknownSourceType
import com.elveum.container.map
import kotlinx.coroutines.flow.first

/**
 * Simple loader function for [LazyFlowSubject].
 */
public typealias SimpleValueLoader<T> = suspend () -> T

/**
 * Start a new load which will replace existing value in the flow
 * returned by [LazyFlowSubject.listen].
 * The load is managed by [valueLoader] which can emit more than one value.
 *
 * Use this method instead of [LazyFlowSubject.newLoad] if you don't want to listen for the
 * current load results.
 *
 * @param silently if set to TRUE, [Container.Pending] is not emitted by the [LazyFlowSubject.listen] flow.
 *
 * Please note that the load starts only when at least one subscriber listens
 * for flow returned by [LazyFlowSubject.listen] method.
 */
public fun <T> LazyFlowSubject<T>.newAsyncLoad(
    silently: Boolean = false,
    valueLoader: ValueLoader<T>,
) {
    newLoad(silently, valueLoader)
}

/**
 * The same as [newAsyncLoad] but using the previous loader function
 * to update a value held by this subject.
 * @see newAsyncLoad
 */
public fun <T> LazyFlowSubject<T>.reloadAsync(
    silently: Boolean = false,
) {
    reload(silently)
}

/**
 * Update the value immediately in a flow returned by [LazyFlowSubject.listen] by
 * using the [updater] function which accepts an old value in arguments.
 *
 * This method cancels the current load.
 */
public inline fun <T> LazyFlowSubject<T>.updateWith(updater: (Container<T>) -> Container<T>) {
    val oldValue = currentValue()
    val newValue = updater(oldValue)
    if (newValue == oldValue) return
    updateWith(newValue)
}


/**
 * Start a new simple load which will replace the existing value in the flow
 * returned by [LazyFlowSubject.listen].
 *
 * Please note that the load starts only when at least one subscriber listens
 * for flow returned by [LazyFlowSubject.listen] method.
 *
 * May throw exception if the load fails.
 */
public suspend fun <T> LazyFlowSubject<T>.newSimpleLoad(
    silently: Boolean = false,
    source: SourceType = UnknownSourceType,
    valueLoader: SimpleValueLoader<T>,
): T {
    val multipleLoader: ValueLoader<T> = {
        emit(valueLoader(), source, isLastValue = true)
    }
    val flow = newLoad(silently, multipleLoader)
    return flow.first()
}

/**
 * The same as [newSimpleLoad] but do not wait for the load result.
 *
 * Please note that the load starts only when at least one subscriber listens
 * for flow returned by [LazyFlowSubject.listen] method.
 */
public fun <T> LazyFlowSubject<T>.newSimpleAsyncLoad(
    silently: Boolean = false,
    source: SourceType = UnknownSourceType,
    valueLoader: SimpleValueLoader<T>
) {
    val multipleLoader: ValueLoader<T> = {
        emit(valueLoader(), source, isLastValue = true)
    }
    newAsyncLoad(silently, multipleLoader)
}

/**
 * Update the value only if there is already successfully loaded old value.
 */
public inline fun <T> LazyFlowSubject<T>.updateIfSuccess(
    crossinline updater: (T) -> T,
) {
    updateWith { container ->
        container.map { updater(it) }
    }
}
