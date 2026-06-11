package com.elveum.store.internal.load

import com.elveum.container.LoadConfig
import com.elveum.store.load.LoadRequestBuilder
import com.elveum.store.load.LoadRequestKeepContentBuilder
import com.elveum.store.load.LoadRequestSource
import com.elveum.store.load.LoadRequestSourceBuilder

internal class LoadRequestBuilderImpl : LoadRequestBuilder,
    LoadRequestKeepContentBuilder,
    LoadRequestSourceBuilder {

    private var config: LoadConfig = LoadConfig.Normal
    private var requestSource: LoadRequestSource = LoadRequestSource.Default

    override fun offlineMode() = apply {
        requestSource = LoadRequestSource.Offline
    }

    override fun freshMode() = apply {
        requestSource = LoadRequestSource.Fresh
    }

    override fun keepContentOnLoad() = apply {
        config = LoadConfig.SilentLoading
    }

    override fun keepContentOnLoadAndError() = apply {
        config = LoadConfig.SilentLoadingAndError
    }

    override fun build() = LoadRequestImpl(config, requestSource)

}
