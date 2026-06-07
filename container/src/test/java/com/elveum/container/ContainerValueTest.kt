package com.elveum.container

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class ContainerValueTest {

    @Test
    fun sourceType_returnsValueFromMetadata() {
        val value = ContainerValue("", SourceTypeMetadata(LocalSourceType))
        assertEquals(LocalSourceType, value.sourceType)
    }

    @Test
    fun backgroundLoadState_returnsValueFromMetadata() {
        val value = ContainerValue("", BackgroundLoadMetadata(BackgroundLoadState.Loading))
        assertEquals(BackgroundLoadState.Loading, value.backgroundLoadState)
    }

    @Test
    fun reloadFunction_returnsValueFromMetadata() {
        val expectedReloadFunction = { _: LoadConfig -> }
        val value = ContainerValue("", ReloadFunctionMetadata(expectedReloadFunction))
        assertEquals(expectedReloadFunction, value.reloadFunction)
    }

    @Test
    fun reload_callsReloadFunctionFromMetadata() {
        val expectedReloadFunction = mockk<(LoadConfig) -> Unit>(relaxed = true)
        val value = ContainerValue("", ReloadFunctionMetadata(expectedReloadFunction))

        value.reload(LoadConfig.SilentLoadingAndError)

        verify(exactly = 1) {
            expectedReloadFunction(LoadConfig.SilentLoadingAndError)
        }
    }

}
