package com.elveum.store.load

import com.elveum.container.LoadConfig
import com.elveum.store.internal.load.LoadRequestBuilderImpl

/**
 * Configure the load request - which data sources should be used and how
 * the load state is propagated to downstream collectors.
 */
public interface LoadRequest {

    /**
     * How the load state is propagated to collectors.
     */
    public val config: LoadConfig

    /**
     * How the load state is propagated to collectors when the load is triggered by a query.
     */
    public val queryConfig: LoadConfig

    /**
     * Which source should be used when loading data.
     */
    public val requestSource: LoadRequestSource

    public companion object {

        /**
         * Default load request triggers data fetching only on demand if data
         * is not available in the in-memory cache. Two the same requests fetch
         * data only once.
         */
        public val Default: LoadRequest = builder().build()

        /**
         * Silent load request which does not replace the existing already loaded
         * data. The background load state can be read using [StoreResult.backgroundLoadState]
         * property, or via extension: [StoreResult.isBackgroundLoading].
         */
        public val Silent: LoadRequest = builder()
            .keepContentOnLoad()
            .keepContentOnQuery()
            .build()

        /**
         * Create a custom [LoadRequest] instance. See [LoadRequestBuilder]
         * for more details.
         *
         * @see LoadRequestBuilder
         */
        public fun builder(): LoadRequestBuilder {
            return LoadRequestBuilderImpl()
        }
    }
}
