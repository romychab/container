package com.elveum.store.internal.load

import com.elveum.container.LoadConfig
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.LoadRequestSource
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadRequestBuilderImplTest {

    @Test
    fun `GIVEN new builder WHEN build without configuration THEN default config and source are used`() {
        val request = LoadRequestBuilderImpl().build()

        assertEquals(LoadConfig.Normal, request.config)
        assertEquals(LoadRequestSource.Default, request.requestSource)
    }

    @Test
    fun `GIVEN builder WHEN offlineMode THEN request source is offline`() {
        val request = LoadRequestBuilderImpl().offlineMode().build()

        assertEquals(LoadRequestSource.Offline, request.requestSource)
        assertEquals(LoadConfig.Normal, request.config)
    }

    @Test
    fun `GIVEN builder WHEN freshMode THEN request source is fresh`() {
        val request = LoadRequestBuilderImpl().freshMode().build()

        assertEquals(LoadRequestSource.Fresh, request.requestSource)
        assertEquals(LoadConfig.Normal, request.config)
    }

    @Test
    fun `GIVEN builder WHEN keepContentOnLoad THEN config is silent loading`() {
        val request = LoadRequestBuilderImpl().keepContentOnLoad().build()

        assertEquals(LoadConfig.SilentLoading, request.config)
        assertEquals(LoadRequestSource.Default, request.requestSource)
    }

    @Test
    fun `GIVEN builder WHEN keepContentOnLoadAndError THEN config is silent loading and error`() {
        val request = LoadRequestBuilderImpl().keepContentOnLoadAndError().build()

        assertEquals(LoadConfig.SilentLoadingAndError, request.config)
        assertEquals(LoadRequestSource.Default, request.requestSource)
    }

    @Test
    fun `GIVEN builder WHEN combine source and content config THEN both are applied`() {
        val request = LoadRequestBuilderImpl()
            .offlineMode()
            .keepContentOnLoadAndError()
            .build()

        assertEquals(LoadConfig.SilentLoadingAndError, request.config)
        assertEquals(LoadRequestSource.Offline, request.requestSource)
    }

    @Test
    fun `GIVEN builder WHEN freshMode overrides offlineMode THEN the last source wins`() {
        val request = LoadRequestBuilderImpl()
            .offlineMode()
            .freshMode()
            .build()

        assertEquals(LoadRequestSource.Fresh, request.requestSource)
    }

    @Test
    fun `GIVEN default load request WHEN read configuration THEN normal config and default source are used`() {
        assertEquals(LoadConfig.Normal, LoadRequest.Default.config)
        assertEquals(LoadRequestSource.Default, LoadRequest.Default.requestSource)
    }

    @Test
    fun `GIVEN silent load request WHEN read configuration THEN silent loading config and default source are used`() {
        assertEquals(LoadConfig.SilentLoading, LoadRequest.Silent.config)
        assertEquals(LoadRequestSource.Default, LoadRequest.Silent.requestSource)
    }
}
