package com.elveum.store.internal.load

import com.elveum.container.LoadConfig
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.LoadRequestSource

internal data class LoadRequestImpl(
    override val config: LoadConfig = LoadConfig.Normal,
    override val requestSource: LoadRequestSource = LoadRequestSource.Default
) : LoadRequest
