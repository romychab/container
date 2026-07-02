package com.elveum.store.internal.load

import com.elveum.container.LoadConfig
import com.elveum.container.ReplaceErrorsOnReload
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

    override fun keepContentOnLoad(replaceErrorsOnReload: Boolean) = apply {
        config = if (replaceErrorsOnReload) {
            LoadConfig.SilentLoading + ReplaceErrorsOnReload
        } else {
            LoadConfig.SilentLoading
        }
    }

    override fun keepContentOnLoadAndError(replaceErrorsOnReload: Boolean) = apply {
        config = if (replaceErrorsOnReload) {
            LoadConfig.SilentLoadingAndError + ReplaceErrorsOnReload
        } else {
            LoadConfig.SilentLoadingAndError
        }
    }

    override fun build() = LoadRequestImpl(config, requestSource)

}
