package com.elveum.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadConfigTest {

    @Test
    fun `Normal preset has all flags disabled`() {
        assertFalse(LoadConfig.Normal.isSilentLoadingEnabled)
        assertFalse(LoadConfig.Normal.isSilentErrorsEnabled)
        assertFalse(LoadConfig.Normal.replaceErrorsOnReload)
    }

    @Test
    fun `SilentLoading preset keeps content while loading only`() {
        assertTrue(LoadConfig.SilentLoading.isSilentLoadingEnabled)
        assertFalse(LoadConfig.SilentLoading.isSilentErrorsEnabled)
        assertFalse(LoadConfig.SilentLoading.replaceErrorsOnReload)
    }

    @Test
    fun `SilentLoadingAndError preset keeps content while loading and on error`() {
        assertTrue(LoadConfig.SilentLoadingAndError.isSilentLoadingEnabled)
        assertTrue(LoadConfig.SilentLoadingAndError.isSilentErrorsEnabled)
        assertFalse(LoadConfig.SilentLoadingAndError.replaceErrorsOnReload)
    }

    @Test
    fun `plus ReplaceErrorsOnReload enables the flag on a silent config`() {
        val result = LoadConfig.SilentLoading + ReplaceErrorsOnReload

        assertTrue(result.isSilentLoadingEnabled)
        assertTrue(result.replaceErrorsOnReload)
    }

    @Test
    fun `plus ReplaceErrorsOnReload enables the flag on SilentLoadingAndError`() {
        val result = LoadConfig.SilentLoadingAndError + ReplaceErrorsOnReload

        assertTrue(result.isSilentLoadingEnabled)
        assertTrue(result.isSilentErrorsEnabled)
        assertTrue(result.replaceErrorsOnReload)
    }

    @Test
    fun `plus ReplaceErrorsOnReload is a no-op for a non-silent config`() {
        val result = LoadConfig.Normal + ReplaceErrorsOnReload

        // there is no kept content to replace, so the receiver is returned unchanged
        assertSame(LoadConfig.Normal, result)
        assertFalse(result.replaceErrorsOnReload)
    }
}
