package com.elveum.store.internal.load

import com.elveum.container.ContainerMetadata
import com.elveum.container.get
import com.elveum.store.load.LoadRequestSource

internal data class LoadRequestSourceMetadata(
    val loadRequestSource: LoadRequestSource,
) : ContainerMetadata, ContainerMetadata.Hidden

internal val ContainerMetadata.loadRequestSource: LoadRequestSource
    get() = get<LoadRequestSourceMetadata>()?.loadRequestSource ?: LoadRequestSource.Default
