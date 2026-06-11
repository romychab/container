package com.elveum.store.internal.builders.paged

import com.elveum.store.internal.builders.SharedConfig

internal open class SharedPageConfig<P : Any, T : Any>(
    val initialKey: P,
    val itemId: (T) -> Any,
) : SharedConfig() {
    var fetchDistance: Int = DEFAULT_FETCH_DISTANCE
}

private const val DEFAULT_FETCH_DISTANCE = 20
