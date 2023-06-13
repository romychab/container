package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.SourceIndicator
import com.elveum.container.UnknownSourceIndicator
import kotlinx.coroutines.flow.first


/**
 * Simple loader function for [LazyFlowSubject].
 */
typealias SimpleValueLoader<T> = suspend () -> T

/**
 * Start a new simple load which will replace the existing value in the flow
 * returned by [LazyFlowSubject.listen].
 *
 * Please note that the load starts only when at least one subscriber listens
 * for flow returned by [LazyFlowSubject.listen] method.
 *
 * May throw exception if the load fails.
 *
 * May throw [LoadCancelledException] if the load has been cancelled by
 * submitting a new load.
 */
suspend fun <T> LazyFlowSubject<T>.newSimpleLoad(
    silently: Boolean = false,
    once: Boolean = false,
    source: SourceIndicator = UnknownSourceIndicator,
    valueLoader: SimpleValueLoader<T>,
): T {
    val multipleLoader: ValueLoader<T> = {
        emit(valueLoader(), source)
    }
    val flow = newLoad(silently, once, multipleLoader)
    return flow.first()
}

/**
 * The same as [newSimpleLoad] but do not wait for the load result.
 *
 * Please note that the load starts only when at least one subscriber listens
 * for flow returned by [LazyFlowSubject.listen] method.
 */
fun <T> LazyFlowSubject<T>.newSimpleAsyncLoad(
    silently: Boolean = false,
    once: Boolean = false,
    source: SourceIndicator = UnknownSourceIndicator,
    valueLoader: SimpleValueLoader<T>
) {
    val multipleLoader: ValueLoader<T> = {
        emit(valueLoader(), source)
    }
    newAsyncLoad(silently, once, multipleLoader)
}

/**
 * Update the value only if there is already successfully loaded old value.
 */
fun <T> LazyFlowSubject<T>.updateIfSuccess(source: SourceIndicator? = null, updater: (T) -> T) {
    updateWith { container ->
        if (container is Container.Success) {
            Container.Success(updater(container.value), source ?: container.source)
        } else {
            container
        }
    }
}
