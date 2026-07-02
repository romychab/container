package com.elveum.store.internal.load

import com.elveum.container.LoadConfig
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.LoadRequestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `GIVEN builder WHEN keepContentOnLoad without replacing errors THEN config is plain silent loading`() {
        val request = LoadRequestBuilderImpl().keepContentOnLoad(replaceErrorsOnReload = false).build()

        assertEquals(LoadConfig.SilentLoading, request.config)
        assertEquals(LoadRequestSource.Default, request.requestSource)
    }

    @Test
    fun `GIVEN builder WHEN keepContentOnLoad THEN silent loading replaces errors on reload by default`() {
        // replaceErrorsOnReload defaults to true, so a stale error is not kept silently while reloading.
        val config = LoadRequestBuilderImpl().keepContentOnLoad().build().config

        assertTrue(config.isSilentLoadingEnabled)
        assertFalse(config.isSilentErrorsEnabled)
        assertTrue(config.replaceErrorsOnReload)
    }

    @Test
    fun `GIVEN builder WHEN keepContentOnLoadAndError without replacing errors THEN config is plain silent loading and error`() {
        val request = LoadRequestBuilderImpl().keepContentOnLoadAndError(replaceErrorsOnReload = false).build()

        assertEquals(LoadConfig.SilentLoadingAndError, request.config)
        assertEquals(LoadRequestSource.Default, request.requestSource)
    }

    @Test
    fun `GIVEN builder WHEN keepContentOnLoadAndError THEN silent loading and error replaces errors on reload by default`() {
        val config = LoadRequestBuilderImpl().keepContentOnLoadAndError().build().config

        assertTrue(config.isSilentLoadingEnabled)
        assertTrue(config.isSilentErrorsEnabled)
        assertTrue(config.replaceErrorsOnReload)
    }

    @Test
    fun `GIVEN builder WHEN combine source and content config THEN both are applied`() {
        val request = LoadRequestBuilderImpl()
            .offlineMode()
            .keepContentOnLoadAndError()
            .build()

        assertTrue(request.config.isSilentLoadingEnabled)
        assertTrue(request.config.isSilentErrorsEnabled)
        assertTrue(request.config.replaceErrorsOnReload)
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
    fun `GIVEN silent load request WHEN read configuration THEN silent loading config replacing errors on reload is used`() {
        // LoadRequest.Silent == builder().keepContentOnLoad().build(), i.e. silent loading with
        // the default ReplaceErrorsOnReload flag attached.
        assertTrue(LoadRequest.Silent.config.isSilentLoadingEnabled)
        assertFalse(LoadRequest.Silent.config.isSilentErrorsEnabled)
        assertTrue(LoadRequest.Silent.config.replaceErrorsOnReload)
        assertEquals(LoadRequestSource.Default, LoadRequest.Silent.requestSource)
    }
}
