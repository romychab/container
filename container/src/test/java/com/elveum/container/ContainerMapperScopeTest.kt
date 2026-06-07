package com.elveum.container

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class ContainerMapperScopeTest {

    @Test
    fun sourceType_returnsValueFromMetadata() {
        val scope = createScope(SourceTypeMetadata(LocalSourceType))
        assertEquals(LocalSourceType, scope.sourceType)
    }

    @Test
    fun backgroundLoadState_returnsValueFromMetadata() {
        val scope = createScope(BackgroundLoadMetadata(BackgroundLoadState.Loading))
        assertEquals(BackgroundLoadState.Loading, scope.backgroundLoadState)
    }

    @Test
    fun reloadFunction_returnsValueFromMetadata() {
        val reloadFunction: (LoadConfig) -> Unit = {}
        val scope = createScope(ReloadFunctionMetadata(reloadFunction))
        assertEquals(reloadFunction, scope.reloadFunction)
    }

    @Test
    fun reload_withoutArgs_callsReloadFunctionWithNormalArg() {
        val reloadFunction = mockk<(LoadConfig) -> Unit>(relaxed = true)
        val scope = createScope(ReloadFunctionMetadata(reloadFunction))

        scope.reload()

        verify(exactly = 1) {
            reloadFunction(LoadConfig.Normal)
        }
    }

    @Test
    fun reload_withArg_callsReloadFunctionWithSpecifiedArg() {
        val reloadFunction = mockk<(LoadConfig) -> Unit>(relaxed = true)
        val scope = createScope(ReloadFunctionMetadata(reloadFunction))

        scope.reload(LoadConfig.SilentLoadingAndError)

        verify(exactly = 1) {
            reloadFunction(LoadConfig.SilentLoadingAndError)
        }
    }

    @Test
    fun successContainer_withDefaultMetadata_usesArgs() {
        val reloadFunction: (LoadConfig) -> Unit = {}
        val scope = createScope(
            LoadTriggerMetadata(LoadTrigger.CacheExpired) + SourceTypeMetadata(LocalSourceType)
        )

        val updatedContainer = scope.successContainer(
            "v2", RemoteSourceType, BackgroundLoadState.Loading, reloadFunction
        )

        assertEquals("v2", updatedContainer.value)
        assertEquals(LoadTrigger.CacheExpired, updatedContainer.metadata.loadTrigger)
        assertEquals(reloadFunction, updatedContainer.reloadFunction)
        assertEquals(RemoteSourceType, updatedContainer.sourceType)
        assertEquals(BackgroundLoadState.Loading, updatedContainer.backgroundLoadState)
    }

    @Test
    fun successContainer_withCustomMetadata_usesArgs() {
        val scope = createScope(
            LoadTriggerMetadata(LoadTrigger.CacheExpired) + SourceTypeMetadata(LocalSourceType)
        )

        val updatedContainer = scope.successContainer(
            "v2", metadata = SourceTypeMetadata(RemoteSourceType)
        )

        assertEquals("v2", updatedContainer.value)
        assertEquals(LoadTrigger.CacheExpired, updatedContainer.metadata.loadTrigger)
        assertEquals(RemoteSourceType, updatedContainer.sourceType)
    }

    @Test
    fun errorContainer_withDefaultMetadata_usesArgs() {
        val reloadFunction: (LoadConfig) -> Unit = {}
        val exception = IllegalArgumentException("test")
        val scope = createScope(
            LoadTriggerMetadata(LoadTrigger.CacheExpired) + SourceTypeMetadata(LocalSourceType)
        )

        val updatedContainer = scope.errorContainer(
            exception, RemoteSourceType, BackgroundLoadState.Loading, reloadFunction
        )

        assertEquals(exception, updatedContainer.exception)
        assertEquals(LoadTrigger.CacheExpired, updatedContainer.metadata.loadTrigger)
        assertEquals(reloadFunction, updatedContainer.reloadFunction)
        assertEquals(RemoteSourceType, updatedContainer.sourceType)
        assertEquals(BackgroundLoadState.Loading, updatedContainer.backgroundLoadState)
    }

    @Test
    fun errorContainer_withCustomMetadata_usesArgs() {
        val scope = createScope(
            LoadTriggerMetadata(LoadTrigger.CacheExpired) + SourceTypeMetadata(LocalSourceType)
        )
        val exception = IllegalArgumentException("test")

        val updatedContainer = scope.errorContainer(
            exception, metadata = SourceTypeMetadata(RemoteSourceType)
        )

        assertEquals(exception, updatedContainer.exception)
        assertEquals(LoadTrigger.CacheExpired, updatedContainer.metadata.loadTrigger)
        assertEquals(RemoteSourceType, updatedContainer.sourceType)
    }

    @Test
    fun pendingContainer_returnsPendingValue() {
        val scope = createScope()
        assertEquals(pendingContainer(), scope.pendingContainer())
    }

    private fun createScope(metadata: ContainerMetadata = EmptyMetadata): ContainerMapperScope {
        return successContainer("", metadata)
    }

}