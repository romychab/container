package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.SourceType
import com.elveum.container.SourceTypeMetadata
import com.elveum.container.transform
import kotlinx.coroutines.flow.Flow
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
    metadata: ContainerMetadata = EmptyMetadata,
    valueLoader: ValueLoader<T>,
) {
    @Suppress("UnusedFlow")
    newLoad(silently, metadata, valueLoader)
}

/**
 * The same as [newAsyncLoad] but using the previous loader function
 * to update a value held by this subject.
 * @see newAsyncLoad
 */
public fun <T> LazyFlowSubject<T>.reloadAsync(
    silently: Boolean = false,
    metadata: ContainerMetadata = EmptyMetadata,
) {
    @Suppress("UnusedFlow")
    reload(silently, metadata)
}

/**
 * Update the value immediately in a flow returned by [LazyFlowSubject.listen] by
 * using the [updater] function which accepts an old value in arguments.
 *
 * This method cancels the current load.
 */
public inline fun <T> LazyFlowSubject<T>.updateWith(
    configuration: ContainerConfiguration = ContainerConfiguration(),
    updater: (Container<T>) -> Container<T>,
) {
    do {
        val previousValue = currentValue(configuration)
        val nextValue = updater(previousValue)
    } while (!compareAndSet(configuration,previousValue, nextValue))
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
    source: SourceType,
    valueLoader: SimpleValueLoader<T>,
): T = newSimpleLoad(silently, SourceTypeMetadata(source), valueLoader)

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
    metadata: ContainerMetadata = EmptyMetadata,
    valueLoader: SimpleValueLoader<T>,
): T {
    val multipleLoader: ValueLoader<T> = {
        emit(valueLoader(), metadata, isLastValue = true)
    }
    val flow = newLoad(silently, valueLoader = multipleLoader)
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
    source: SourceType,
    valueLoader: SimpleValueLoader<T>
): Unit = newSimpleAsyncLoad(silently, SourceTypeMetadata(source), valueLoader)

/**
 * The same as [newSimpleLoad] but do not wait for the load result.
 *
 * Please note that the load starts only when at least one subscriber listens
 * for flow returned by [LazyFlowSubject.listen] method.
 */
public fun <T> LazyFlowSubject<T>.newSimpleAsyncLoad(
    silently: Boolean = false,
    metadata: ContainerMetadata,
    valueLoader: SimpleValueLoader<T>
) {
    val multipleLoader: ValueLoader<T> = {
        emit(valueLoader(), metadata, isLastValue = true)
    }
    newAsyncLoad(silently, valueLoader = multipleLoader)
}

/**
 * Update the value only if there is already successfully loaded old value.
 */
public inline fun <T> LazyFlowSubject<T>.updateIfSuccess(
    source: SourceType?,
    updater: (T) -> T,
): Unit = updateIfSuccess(source?.let(::SourceTypeMetadata) ?: EmptyMetadata, updater)

/**
 * Update the value only if there is already successfully loaded old value.
 */
public inline fun <T> LazyFlowSubject<T>.updateIfSuccess(
    metadata: ContainerMetadata = EmptyMetadata,
    updater: (T) -> T,
) {
    updateWith { oldContainer ->
        oldContainer.transform(
            onSuccess = {
                successContainer(updater(it), metadata)
            }
        )
    }
}

/**
 * Listen for values loaded by this subject and for each emitted container:
 * - attach a valid reload function
 * - re-emit background load indicator if data is being reloaded
 */
public fun <T> LazyFlowSubject<T>.listenReloadable(
    emitReloadFunction: Boolean = true,
    emitBackgroundLoads: Boolean = true,
): Flow<Container<T>> {
    return listen(
        configuration = ContainerConfiguration(
            emitBackgroundLoads = emitBackgroundLoads,
            emitReloadFunction = emitReloadFunction,
        )
    )
}
