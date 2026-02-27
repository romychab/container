package com.elveum.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerMetadataTest {

    @Test
    fun plus_withNull_returnsSelf() {
        val metadata = BackgroundLoadMetadata(BackgroundLoadState.Loading)
        val result = metadata + null
        assertSame(metadata, result)
    }

    @Test
    fun plus_twoDistinctSingleMetadata_returnsCombinedMetadata() {
        val first = BackgroundLoadMetadata(BackgroundLoadState.Loading)
        val second = SourceTypeMetadata(LocalSourceType)

        val result = first + second

        assertTrue(result is CombinedMetadata)
        assertEquals(BackgroundLoadMetadata(BackgroundLoadState.Loading), result.get<BackgroundLoadMetadata>())
        assertEquals(SourceTypeMetadata(LocalSourceType), result.get<SourceTypeMetadata>())
    }

    @Test
    fun plus_combinedMetadataPlusSingleMetadata_includesAllEntries() {
        val combined = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(LocalSourceType)
        val extra = ReloadFunctionMetadata { }

        val result = combined + extra

        assertNotNull(result.get<BackgroundLoadMetadata>())
        assertNotNull(result.get<SourceTypeMetadata>())
        assertNotNull(result.get<ReloadFunctionMetadata>())
    }

    @Test
    fun plus_singleMetadataPlusCombinedMetadata_includesAllEntries() {
        val single = BackgroundLoadMetadata(BackgroundLoadState.Loading)
        val combined = SourceTypeMetadata(LocalSourceType) + ReloadFunctionMetadata { }

        val result = single + combined

        assertNotNull(result.get<BackgroundLoadMetadata>())
        assertNotNull(result.get<SourceTypeMetadata>())
        assertNotNull(result.get<ReloadFunctionMetadata>())
    }

    @Test
    fun plus_twoCombinedMetadata_includesAllEntries() {
        val first = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(LocalSourceType)
        val second = ReloadFunctionMetadata { } + CustomMetadata("uuid-1")

        val result = first + second

        assertNotNull(result.get<BackgroundLoadMetadata>())
        assertNotNull(result.get<SourceTypeMetadata>())
        assertNotNull(result.get<ReloadFunctionMetadata>())
        assertNotNull(result.get<CustomMetadata>())
    }

    @Test
    fun plus_sameType_latestReplacesEarlier() {
        val first = BackgroundLoadMetadata(BackgroundLoadState.Idle)
        val second = BackgroundLoadMetadata(BackgroundLoadState.Loading)

        val result = first + second

        assertEquals(BackgroundLoadMetadata(BackgroundLoadState.Loading), result.get<BackgroundLoadMetadata>())
    }

    @Test
    fun plus_sameTypeInCombined_latestReplacesEarlier() {
        val base = BackgroundLoadMetadata(BackgroundLoadState.Idle) + SourceTypeMetadata(LocalSourceType)
        val override = BackgroundLoadMetadata(BackgroundLoadState.Loading)

        val result = base + override

        assertEquals(BackgroundLoadMetadata(BackgroundLoadState.Loading), result.get<BackgroundLoadMetadata>())
        assertEquals(SourceTypeMetadata(LocalSourceType), result.get<SourceTypeMetadata>())
    }

    @Test
    fun plus_allSameType_resultIsSingleMetadata() {
        val first = BackgroundLoadMetadata(BackgroundLoadState.Idle)
        val second = BackgroundLoadMetadata(BackgroundLoadState.Loading)

        val result = first + second

        assertTrue(result is BackgroundLoadMetadata)
    }

    @Test
    fun filter_predicateTrue_returnsSelf() {
        val metadata = BackgroundLoadMetadata(BackgroundLoadState.Loading)

        val result = metadata.filter { true }

        assertSame(metadata, result)
    }

    @Test
    fun filter_predicateFalse_returnsEmptyMetadata() {
        val metadata = BackgroundLoadMetadata(BackgroundLoadState.Loading)

        val result = metadata.filter { false }

        assertSame(EmptyMetadata, result)
    }

    @Test
    fun combinedMetadataFilter_predicateMatchesSome_returnsMatchingEntries() {
        val combined = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(LocalSourceType)

        val result = combined.filter { it is BackgroundLoadMetadata }

        assertEquals(BackgroundLoadMetadata(BackgroundLoadState.Loading), result.get<BackgroundLoadMetadata>())
        assertNull(result.get<SourceTypeMetadata>())
    }

    @Test
    fun combinedMetadataFilter_predicateMatchesAll_returnsAllEntries() {
        val combined = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(LocalSourceType)

        val result = combined.filter { true }

        assertNotNull(result.get<BackgroundLoadMetadata>())
        assertNotNull(result.get<SourceTypeMetadata>())
    }

    @Test
    fun combinedMetadataFilter_predicateMatchesNone_returnsEmptyMetadata() {
        val combined = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(LocalSourceType)

        val result = combined.filter { false }

        assertSame(EmptyMetadata, result)
    }

    @Test
    fun combinedMetadataFilter_singleEntryLeft_returnsSingleMetadata() {
        val combined = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(LocalSourceType)

        val result = combined.filter { it is SourceTypeMetadata }

        assertTrue(result is SourceTypeMetadata)
    }

    @Test
    fun get_directTypeMatch_returnsInstance() {
        val metadata = BackgroundLoadMetadata(BackgroundLoadState.Loading)

        val result = metadata.get<BackgroundLoadMetadata>()

        assertEquals(BackgroundLoadMetadata(BackgroundLoadState.Loading), result)
    }

    @Test
    fun get_noDirectTypeMatch_returnsNull() {
        val metadata = BackgroundLoadMetadata(BackgroundLoadState.Loading)

        val result = metadata.get<SourceTypeMetadata>()

        assertNull(result)
    }

    @Test
    fun get_combinedMetadataWithMatchingType_returnsMatchingInstance() {
        val metadata = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(RemoteSourceType)

        val result = metadata.get<SourceTypeMetadata>()

        assertEquals(SourceTypeMetadata(RemoteSourceType), result)
    }

    @Test
    fun get_combinedMetadataWithoutMatchingType_returnsNull() {
        val metadata = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(RemoteSourceType)

        val result = metadata.get<CustomMetadata>()

        assertNull(result)
    }

    @Test
    fun emptyMetadataGet_anyType_returnsNull() {
        val result = EmptyMetadata.get<BackgroundLoadMetadata>()

        assertNull(result)
    }

    @Test
    fun defaultMetadata_allNull_returnsEmptyMetadata() {
        val result = defaultMetadata()

        assertSame(EmptyMetadata, result)
    }

    @Test
    fun defaultMetadata_withSource_containsSourceTypeMetadata() {
        val result = defaultMetadata(source = LocalSourceType)

        assertEquals(SourceTypeMetadata(LocalSourceType), result.get<SourceTypeMetadata>())
    }

    @Test
    fun defaultMetadata_withBackgroundLoadLoading_containsBackgroundLoadMetadata() {
        val result = defaultMetadata(backgroundLoadState = BackgroundLoadState.Loading)

        assertEquals(BackgroundLoadMetadata(BackgroundLoadState.Loading), result.get<BackgroundLoadMetadata>())
    }

    @Test
    fun defaultMetadata_withBackgroundLoadEmpty_containsBackgroundLoadMetadata() {
        val result = defaultMetadata(backgroundLoadState = BackgroundLoadState.Idle)

        assertEquals(BackgroundLoadMetadata(BackgroundLoadState.Idle), result.get<BackgroundLoadMetadata>())
    }

    @Test
    fun defaultMetadata_withReloadFunction_containsReloadFunctionMetadata() {
        val fn: ReloadFunction = { }

        val result = defaultMetadata(reloadFunction = fn)

        assertEquals(fn, result.get<ReloadFunctionMetadata>()?.reloadFunction)
    }

    @Test
    fun defaultMetadata_withAllArgs_containsAllMetadata() {
        val fn: ReloadFunction = { }

        val result = defaultMetadata(
            source = RemoteSourceType,
            backgroundLoadState = BackgroundLoadState.Loading,
            reloadFunction = fn,
        )

        assertEquals(SourceTypeMetadata(RemoteSourceType), result.get<SourceTypeMetadata>())
        assertEquals(BackgroundLoadMetadata(BackgroundLoadState.Loading), result.get<BackgroundLoadMetadata>())
        assertEquals(fn, result.get<ReloadFunctionMetadata>()?.reloadFunction)
    }

    @Test
    fun defaultMetadata_withOnlySource_doesNotContainOtherMetadata() {
        val result = defaultMetadata(source = LocalSourceType)

        assertNull(result.get<BackgroundLoadMetadata>())
        assertNull(result.get<ReloadFunctionMetadata>())
    }

    @Test
    fun defaultMetadata_withOnlyLoading_doesNotContainOtherMetadata() {
        val result = defaultMetadata(backgroundLoadState = BackgroundLoadState.Loading)

        assertNull(result.get<SourceTypeMetadata>())
        assertNull(result.get<ReloadFunctionMetadata>())
    }


    @Test
    fun defaultMetadata_withOnlyReloadFunction_doesNotContainOtherMetadata() {
        val result = defaultMetadata(reloadFunction = {})

        assertNull(result.get<BackgroundLoadMetadata>())
        assertNull(result.get<SourceTypeMetadata>())
    }

    @Test
    fun backgroundLoad_emptyMetadata_returnsEmpty() {
        assertEquals(BackgroundLoadState.Idle, EmptyMetadata.backgroundLoadState)
    }

    @Test
    fun backgroundLoad_withLoadingValue_returnsLoading() {
        val metadata = BackgroundLoadMetadata(BackgroundLoadState.Loading)

        assertEquals(BackgroundLoadState.Loading, metadata.backgroundLoadState)
    }

    @Test
    fun backgroundLoad_withEmptyValue_returnsEmpty() {
        val metadata = BackgroundLoadMetadata(BackgroundLoadState.Idle)

        assertEquals(BackgroundLoadState.Idle, metadata.backgroundLoadState)
    }

    @Test
    fun backgroundLoad_combinedMetadataWithLoading_returnsLoading() {
        val metadata = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(LocalSourceType)

        assertEquals(BackgroundLoadState.Loading, metadata.backgroundLoadState)
    }

    @Test
    fun backgroundLoad_combinedMetadataWithoutBackgroundLoadMetadata_returnsEmpty() {
        val metadata = SourceTypeMetadata(LocalSourceType) + CustomMetadata("uuid")

        assertEquals(BackgroundLoadState.Idle, metadata.backgroundLoadState)
    }

    @Test
    fun backgroundLoad_unrelatedMetadataType_returnsEmpty() {
        val metadata = SourceTypeMetadata(RemoteSourceType)

        assertEquals(BackgroundLoadState.Idle, metadata.backgroundLoadState)
    }

    @Test
    fun reloadFunction_emptyMetadata_returnsEmptyReloadFunction() {
        assertEquals(EmptyReloadFunction, EmptyMetadata.reloadFunction)
    }

    @Test
    fun reloadFunction_unrelatedMetadata_returnsEmptyReloadFunction() {
        val metadata = SourceTypeMetadata(LocalSourceType)

        assertEquals(EmptyReloadFunction, metadata.reloadFunction)
    }

    @Test
    fun reloadFunction_withReloadFunctionMetadata_returnsStoredFunction() {
        val fn: ReloadFunction = { }
        val metadata = ReloadFunctionMetadata(fn)

        assertSame(fn, metadata.reloadFunction)
    }

    @Test
    fun reloadFunction_combinedMetadataWithReloadFunction_returnsStoredFunction() {
        val fn: ReloadFunction = { }
        val metadata = SourceTypeMetadata(LocalSourceType) + ReloadFunctionMetadata(fn)

        assertSame(fn, metadata.reloadFunction)
    }

    @Test
    fun sourceType_emptyMetadata_returnsUnknownSourceType() {
        assertEquals(UnknownSourceType, EmptyMetadata.sourceType)
    }

    @Test
    fun sourceType_unrelatedMetadata_returnsUnknownSourceType() {
        val metadata = BackgroundLoadMetadata(BackgroundLoadState.Loading)

        assertEquals(UnknownSourceType, metadata.sourceType)
    }

    @Test
    fun sourceType_withLocalSourceType_returnsLocalSourceType() {
        val metadata = SourceTypeMetadata(LocalSourceType)

        assertEquals(LocalSourceType, metadata.sourceType)
    }

    @Test
    fun sourceType_withRemoteSourceType_returnsRemoteSourceType() {
        val metadata = SourceTypeMetadata(RemoteSourceType)

        assertEquals(RemoteSourceType, metadata.sourceType)
    }

    @Test
    fun sourceType_combinedMetadataWithSource_returnsCorrectSourceType() {
        val metadata = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(FakeSourceType)

        assertEquals(FakeSourceType, metadata.sourceType)
    }

    @Test
    fun emptyMetadataPlus_null_returnsEmptyMetadata() {
        val result = EmptyMetadata + null

        assertSame(EmptyMetadata, result)
    }

    @Test
    fun emptyMetadataPlus_otherMetadata_returnsOther() {
        val other = BackgroundLoadMetadata(BackgroundLoadState.Loading)

        val result = EmptyMetadata + other

        assertSame(other, result)
    }

    @Test
    fun emptyMetadataPlus_emptyMetadata_returnsEmptyMetadata() {
        val result = EmptyMetadata + EmptyMetadata

        assertSame(EmptyMetadata, result)
    }

    @Test
    fun emptyMetadataPlus_combinedMetadata_returnsCombinedMetadata() {
        val combined = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(LocalSourceType)

        val result = EmptyMetadata + combined

        assertSame(combined, result)
    }

    @Test
    fun combinedMetadataEquals_sameEntriesDifferentOrder_returnsTrue() {
        val first = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(LocalSourceType)
        val second = SourceTypeMetadata(LocalSourceType) + BackgroundLoadMetadata(BackgroundLoadState.Loading)

        assertEquals(first, second)
    }

    @Test
    fun combinedMetadataEquals_differentEntries_returnsFalse() {
        val first = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(LocalSourceType)
        val second = BackgroundLoadMetadata(BackgroundLoadState.Idle) + SourceTypeMetadata(LocalSourceType)

        assertTrue(first != second)
    }

    @Test
    fun combinedMetadataHashCode_sameEntriesDifferentOrder_returnsSameHash() {
        val first = BackgroundLoadMetadata(BackgroundLoadState.Loading) + SourceTypeMetadata(LocalSourceType)
        val second = SourceTypeMetadata(LocalSourceType) + BackgroundLoadMetadata(BackgroundLoadState.Loading)

        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun plus_allEntriesAreEmptyMetadata_returnsEmptyMetadata() {
        val result = EmptyMetadata + EmptyMetadata

        assertSame(EmptyMetadata, result)
    }

    @Test
    fun plus_emptyMetadataWithSingleEntry_returnsSingleEntry() {
        val single = BackgroundLoadMetadata(BackgroundLoadState.Loading)

        val result = EmptyMetadata + single

        assertSame(single, result)
    }

    @Test
    fun plus_duplicateTypeAcrossCombined_latestValueWins() {
        val first = BackgroundLoadMetadata(BackgroundLoadState.Idle) + SourceTypeMetadata(LocalSourceType)
        val second = SourceTypeMetadata(RemoteSourceType) + CustomMetadata("uuid-1")

        val result = first + second

        assertEquals(RemoteSourceType, result.sourceType)
        assertEquals(BackgroundLoadState.Idle, result.backgroundLoadState)
        assertNotNull(result.get<CustomMetadata>())
    }

    private data class CustomMetadata(
        val id: String,
    ) : ContainerMetadata
}
