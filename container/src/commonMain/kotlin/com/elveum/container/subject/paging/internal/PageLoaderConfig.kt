package com.elveum.container.subject.paging.internal

import com.elveum.container.subject.paging.PageEmitter
import kotlin.math.max

internal data class PageLoaderConfig<Key, T>(
    val initialKey: Key,
    val fetchDistance: Int,
    val emitMetadata: Boolean,
    val itemId: (T) -> Any,
    val block: suspend PageEmitter<Key, T>.(Key) -> Unit,
) {
    val finalFetchDistance = max(fetchDistance, 1)
}

