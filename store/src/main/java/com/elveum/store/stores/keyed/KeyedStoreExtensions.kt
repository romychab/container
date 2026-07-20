package com.elveum.store.stores.keyed

import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata

/**
 * Invalidate all keys in the [KeyedStore].
 */
public fun <Key : Any, T : Any> KeyedStore<Key, T>.invalidateAllAsync(
    metadata: ContainerMetadata = EmptyMetadata,
) {
    activeKeys.value.forEach {
        invalidateAsync(it, metadata)
    }
}
