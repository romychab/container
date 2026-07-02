package com.elveum.store.internal.stores.common

import com.elveum.store.load.LoadRequest

internal data class KeyRecord<Key>(
    val key: Key,
    val loadRequest: LoadRequest,
)
