package com.elveum.container.subject.paging

import com.elveum.container.ContainerMetadata
import com.elveum.container.get

/**
 * Notify the page loader that an item with the specified [index] has
 * been rendered to the user. The loader start loading the next page (if
 * it hasn't been started yet).
 */
public fun ContainerMetadata.onItemRendered(index: Int) {
    get<OnItemRenderedCallbackMetadata>()?.onItemRendered?.invoke(index)
}

public data class OnItemRenderedCallbackMetadata(
    val onItemRendered: (index: Int) -> Unit
) : ContainerMetadata

