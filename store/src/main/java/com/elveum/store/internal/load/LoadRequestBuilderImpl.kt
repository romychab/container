package com.elveum.store.internal.load

import com.elveum.container.LoadConfig
import com.elveum.container.ReplaceErrorsOnReload
import com.elveum.store.load.LoadRequestBuilder
import com.elveum.store.load.LoadRequestSource

internal class LoadRequestBuilderImpl : LoadRequestBuilder {

    private var config: LoadConfig = LoadConfig.Normal
    private var queryConfig: LoadConfig = LoadConfig.Normal
    private var requestSource: LoadRequestSource = LoadRequestSource.Default

    override fun offlineMode() = apply {
        requestSource = LoadRequestSource.Offline
    }

    override fun freshMode() = apply {
        requestSource = LoadRequestSource.Fresh
    }

    override fun keepContentOnLoad(replaceErrorsOnReload: Boolean) = apply {
        config = keepContent(replaceErrorsOnReload)
    }

    override fun keepContentOnLoadAndError(replaceErrorsOnReload: Boolean) = apply {
        config = keepContentAndError(replaceErrorsOnReload)
    }

    override fun keepContentOnQuery(replaceErrorsOnQuery: Boolean) = apply {
        queryConfig = keepContent(replaceErrorsOnQuery)
    }

    override fun keepContentOnQueryAndError(replaceErrorsOnQuery: Boolean) = apply {
        queryConfig = keepContentAndError(replaceErrorsOnQuery)
    }

    override fun build() = LoadRequestImpl(config, queryConfig ?: config, requestSource)

    private fun keepContent(replaceErrors: Boolean) = run {
        if (replaceErrors) {
            LoadConfig.SilentLoading + ReplaceErrorsOnReload
        } else {
            LoadConfig.SilentLoading
        }
    }

    private fun keepContentAndError(replaceErrors: Boolean) = run {
        if (replaceErrors) {
            LoadConfig.SilentLoadingAndError + ReplaceErrorsOnReload
        } else {
            LoadConfig.SilentLoadingAndError
        }
    }
}
