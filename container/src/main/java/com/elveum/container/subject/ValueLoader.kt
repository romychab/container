package com.elveum.container.subject

import com.elveum.container.Container
import com.elveum.container.Emitter
import com.elveum.container.StatefulEmitter
import com.elveum.container.subject.transformation.LoaderDecorator
import com.elveum.container.subject.transformation.internal.executeOn

/**
 * Loader function for [LazyFlowSubject] which can emit loaded values.
 */
public fun interface ValueLoader<T> {

    /**
     * The body of value loader which can emit new items at any time
     * during the load.
     */
    public suspend operator fun Emitter<T>.invoke()
}

/**
 * Loader function that can manage the full cycle of loading, including
 * manual emitting of the current load state, multiple loads, retain any manual state
 * across multiple loadings, etc.
 */
public interface StatefulValueLoader<T> : ValueLoader<T> {

    public suspend fun StatefulEmitter<T>.statefulInvoke(decorator: LoaderDecorator)

    public fun intercept(container: Container<T>): Container<T> = container

    public companion object {
        internal fun <T> wrap(valueLoader: ValueLoader<T>): StatefulValueLoader<T> {
            return valueLoader as? StatefulValueLoader<T>
                ?: StatefulValueLoaderImpl(valueLoader)
        }
    }
}

internal class StatefulValueLoaderImpl<T>(
    private val origin: ValueLoader<T>
) : StatefulValueLoader<T>, ValueLoader<T> by origin {

    override suspend fun StatefulEmitter<T>.statefulInvoke(decorator: LoaderDecorator) {
        decorator.executeOn(emitter = this) { invoke() }
    }

}
