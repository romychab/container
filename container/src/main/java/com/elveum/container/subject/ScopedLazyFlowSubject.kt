package com.elveum.container.subject

import com.elveum.container.Container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Scoped LazyFlowSubject accessible within [LazyFlowSubject.whenActive]
 * call.
 */
public interface ScopedLazyFlowSubject<T> : LazyFlowSubject<T>, CoroutineScope {

    override fun listen(
        configuration: ContainerConfiguration,
    ): StateFlow<Container<T>>

}
