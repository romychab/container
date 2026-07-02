package com.elveum.store.internal.stores.common

import com.elveum.container.Container
import com.elveum.container.ContainerValue
import com.elveum.container.successContainer
import com.elveum.store.stores.base.OptimisticUpdateScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicReference

internal suspend fun <T : Any> processOptimisticUpdate(
    getter: () -> ContainerValue<T>?,
    setToCache: (Container<T>) -> Unit,
    updater: suspend OptimisticUpdateScope<T>.(T) -> Unit,
) {
    val oldValue = getter() ?: return
    val optimisticValueReference: AtomicReference<Container.Success<T>?> = AtomicReference(null)
    val scope = OptimisticUpdateScope<T> {
        val container = successContainer(it, oldValue.metadata)
        optimisticValueReference.set(container)
        setToCache(container)
    }
    try {
        updater(scope, oldValue.value)
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        // restore old value on error if optimistic value has not been replaced:
        if (optimisticValueReference.get()?.value === getter()?.value) {
            setToCache(successContainer(oldValue.value, oldValue.metadata))
        }
        throw e
    }
}
