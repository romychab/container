package com.elveum.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerMetadataTest {

    @Test
    fun plus_withNull_returnsSelf() {
        val metadata = IsLoadingInBackgroundMetadata(true)
        val result = metadata + null
        assertSame(metadata, result)
    }

    @Test
    fun plus_twoDistinctSingleMetadata_returnsCombinedMetadata() {
        val first = IsLoadingInBackgroundMetadata(true)
        val second = SourceTypeMetadata(LocalSourceType)

        val result = first + second

        assertTrue(result is CombinedMetadata)
        assertEquals(IsLoadingInBackgroundMetadata(true), result.get<IsLoadingInBackgroundMetadata>())
        assertEquals(SourceTypeMetadata(LocalSourceType), result.get<SourceTypeMetadata>())
    }

    @Test
    fun plus_combinedMetadataPlusSingleMetadata_includesAllEntries() {
        val combined = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(LocalSourceType)
        val extra = ReloadFunctionMetadata { }

        val result = combined + extra

        assertNotNull(result.get<IsLoadingInBackgroundMetadata>())
        assertNotNull(result.get<SourceTypeMetadata>())
        assertNotNull(result.get<ReloadFunctionMetadata>())
    }

    @Test
    fun plus_singleMetadataPlusCombinedMetadata_includesAllEntries() {
        val single = IsLoadingInBackgroundMetadata(true)
        val combined = SourceTypeMetadata(LocalSourceType) + ReloadFunctionMetadata { }

        val result = single + combined

        assertNotNull(result.get<IsLoadingInBackgroundMetadata>())
        assertNotNull(result.get<SourceTypeMetadata>())
        assertNotNull(result.get<ReloadFunctionMetadata>())
    }

    @Test
    fun plus_twoCombinedMetadata_includesAllEntries() {
        val first = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(LocalSourceType)
        val second = ReloadFunctionMetadata { } + LoadUuidMetadata("uuid-1")

        val result = first + second

        assertNotNull(result.get<IsLoadingInBackgroundMetadata>())
        assertNotNull(result.get<SourceTypeMetadata>())
        assertNotNull(result.get<ReloadFunctionMetadata>())
        assertNotNull(result.get<LoadUuidMetadata>())
    }

    @Test
    fun plus_sameType_latestReplacesEarlier() {
        val first = IsLoadingInBackgroundMetadata(false)
        val second = IsLoadingInBackgroundMetadata(true)

        val result = first + second

        assertEquals(IsLoadingInBackgroundMetadata(true), result.get<IsLoadingInBackgroundMetadata>())
    }

    @Test
    fun plus_sameTypeInCombined_latestReplacesEarlier() {
        val base = IsLoadingInBackgroundMetadata(false) + SourceTypeMetadata(LocalSourceType)
        val override = IsLoadingInBackgroundMetadata(true)

        val result = base + override

        assertEquals(IsLoadingInBackgroundMetadata(true), result.get<IsLoadingInBackgroundMetadata>())
        assertEquals(SourceTypeMetadata(LocalSourceType), result.get<SourceTypeMetadata>())
    }

    @Test
    fun plus_allSameType_resultIsSingleMetadata() {
        val first = IsLoadingInBackgroundMetadata(false)
        val second = IsLoadingInBackgroundMetadata(true)

        val result = first + second

        assertTrue(result is IsLoadingInBackgroundMetadata)
    }

    @Test
    fun filter_predicateTrue_returnsSelf() {
        val metadata = IsLoadingInBackgroundMetadata(true)

        val result = metadata.filter { true }

        assertSame(metadata, result)
    }

    @Test
    fun filter_predicateFalse_returnsEmptyMetadata() {
        val metadata = IsLoadingInBackgroundMetadata(true)

        val result = metadata.filter { false }

        assertSame(EmptyMetadata, result)
    }

    @Test
    fun combinedMetadataFilter_predicateMatchesSome_returnsMatchingEntries() {
        val combined = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(LocalSourceType)

        val result = combined.filter { it is IsLoadingInBackgroundMetadata }

        assertEquals(IsLoadingInBackgroundMetadata(true), result.get<IsLoadingInBackgroundMetadata>())
        assertNull(result.get<SourceTypeMetadata>())
    }

    @Test
    fun combinedMetadataFilter_predicateMatchesAll_returnsAllEntries() {
        val combined = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(LocalSourceType)

        val result = combined.filter { true }

        assertNotNull(result.get<IsLoadingInBackgroundMetadata>())
        assertNotNull(result.get<SourceTypeMetadata>())
    }

    @Test
    fun combinedMetadataFilter_predicateMatchesNone_returnsEmptyMetadata() {
        val combined = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(LocalSourceType)

        val result = combined.filter { false }

        assertSame(EmptyMetadata, result)
    }

    @Test
    fun combinedMetadataFilter_singleEntryLeft_returnsSingleMetadata() {
        val combined = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(LocalSourceType)

        val result = combined.filter { it is SourceTypeMetadata }

        assertTrue(result is SourceTypeMetadata)
    }

    @Test
    fun get_directTypeMatch_returnsInstance() {
        val metadata = IsLoadingInBackgroundMetadata(true)

        val result = metadata.get<IsLoadingInBackgroundMetadata>()

        assertEquals(IsLoadingInBackgroundMetadata(true), result)
    }

    @Test
    fun get_noDirectTypeMatch_returnsNull() {
        val metadata = IsLoadingInBackgroundMetadata(true)

        val result = metadata.get<SourceTypeMetadata>()

        assertNull(result)
    }

    @Test
    fun get_combinedMetadataWithMatchingType_returnsMatchingInstance() {
        val metadata = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(RemoteSourceType)

        val result = metadata.get<SourceTypeMetadata>()

        assertEquals(SourceTypeMetadata(RemoteSourceType), result)
    }

    @Test
    fun get_combinedMetadataWithoutMatchingType_returnsNull() {
        val metadata = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(RemoteSourceType)

        val result = metadata.get<LoadUuidMetadata>()

        assertNull(result)
    }

    @Test
    fun emptyMetadataGet_anyType_returnsNull() {
        val result = EmptyMetadata.get<IsLoadingInBackgroundMetadata>()

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
    fun defaultMetadata_withIsLoadingInBackgroundTrue_containsIsLoadingInBackgroundMetadata() {
        val result = defaultMetadata(isLoadingInBackground = true)

        assertEquals(IsLoadingInBackgroundMetadata(true), result.get<IsLoadingInBackgroundMetadata>())
    }

    @Test
    fun defaultMetadata_withIsLoadingInBackgroundFalse_containsIsLoadingInBackgroundMetadata() {
        val result = defaultMetadata(isLoadingInBackground = false)

        assertEquals(IsLoadingInBackgroundMetadata(false), result.get<IsLoadingInBackgroundMetadata>())
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
            isLoadingInBackground = true,
            reloadFunction = fn,
        )

        assertEquals(SourceTypeMetadata(RemoteSourceType), result.get<SourceTypeMetadata>())
        assertEquals(IsLoadingInBackgroundMetadata(true), result.get<IsLoadingInBackgroundMetadata>())
        assertEquals(fn, result.get<ReloadFunctionMetadata>()?.reloadFunction)
    }

    @Test
    fun defaultMetadata_withOnlySource_doesNotContainOtherMetadata() {
        val result = defaultMetadata(source = LocalSourceType)

        assertNull(result.get<IsLoadingInBackgroundMetadata>())
        assertNull(result.get<ReloadFunctionMetadata>())
    }

    @Test
    fun defaultMetadata_withOnlyLoading_doesNotContainOtherMetadata() {
        val result = defaultMetadata(isLoadingInBackground = true)

        assertNull(result.get<SourceTypeMetadata>())
        assertNull(result.get<ReloadFunctionMetadata>())
    }


    @Test
    fun defaultMetadata_withOnlyReloadFunction_doesNotContainOtherMetadata() {
        val result = defaultMetadata(reloadFunction = {})

        assertNull(result.get<IsLoadingInBackgroundMetadata>())
        assertNull(result.get<SourceTypeMetadata>())
    }

    @Test
    fun isLoadingInBackground_emptyMetadata_returnsFalse() {
        assertFalse(EmptyMetadata.isLoadingInBackground)
    }

    @Test
    fun isLoadingInBackground_withTrueValue_returnsTrue() {
        val metadata = IsLoadingInBackgroundMetadata(true)

        assertTrue(metadata.isLoadingInBackground)
    }

    @Test
    fun isLoadingInBackground_withFalseValue_returnsFalse() {
        val metadata = IsLoadingInBackgroundMetadata(false)

        assertFalse(metadata.isLoadingInBackground)
    }

    @Test
    fun isLoadingInBackground_combinedMetadataWithTrue_returnsTrue() {
        val metadata = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(LocalSourceType)

        assertTrue(metadata.isLoadingInBackground)
    }

    @Test
    fun isLoadingInBackground_combinedMetadataWithoutIsLoadingMetadata_returnsFalse() {
        val metadata = SourceTypeMetadata(LocalSourceType) + LoadUuidMetadata("uuid")

        assertFalse(metadata.isLoadingInBackground)
    }

    @Test
    fun isLoadingInBackground_unrelatedMetadataType_returnsFalse() {
        val metadata = SourceTypeMetadata(RemoteSourceType)

        assertFalse(metadata.isLoadingInBackground)
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
        val metadata = IsLoadingInBackgroundMetadata(true)

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
        val metadata = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(FakeSourceType)

        assertEquals(FakeSourceType, metadata.sourceType)
    }

    @Test
    fun emptyMetadataPlus_null_returnsEmptyMetadata() {
        val result = EmptyMetadata + null

        assertSame(EmptyMetadata, result)
    }

    @Test
    fun emptyMetadataPlus_otherMetadata_returnsOther() {
        val other = IsLoadingInBackgroundMetadata(true)

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
        val combined = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(LocalSourceType)

        val result = EmptyMetadata + combined

        assertSame(combined, result)
    }

    @Test
    fun combinedMetadataEquals_sameEntriesDifferentOrder_returnsTrue() {
        val first = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(LocalSourceType)
        val second = SourceTypeMetadata(LocalSourceType) + IsLoadingInBackgroundMetadata(true)

        assertEquals(first, second)
    }

    @Test
    fun combinedMetadataEquals_differentEntries_returnsFalse() {
        val first = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(LocalSourceType)
        val second = IsLoadingInBackgroundMetadata(false) + SourceTypeMetadata(LocalSourceType)

        assertTrue(first != second)
    }

    @Test
    fun combinedMetadataHashCode_sameEntriesDifferentOrder_returnsSameHash() {
        val first = IsLoadingInBackgroundMetadata(true) + SourceTypeMetadata(LocalSourceType)
        val second = SourceTypeMetadata(LocalSourceType) + IsLoadingInBackgroundMetadata(true)

        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun plus_allEntriesAreEmptyMetadata_returnsEmptyMetadata() {
        val result = EmptyMetadata + EmptyMetadata

        assertSame(EmptyMetadata, result)
    }

    @Test
    fun plus_emptyMetadataWithSingleEntry_returnsSingleEntry() {
        val single = IsLoadingInBackgroundMetadata(true)

        val result = EmptyMetadata + single

        assertSame(single, result)
    }

    @Test
    fun plus_duplicateTypeAcrossCombined_latestValueWins() {
        val first = IsLoadingInBackgroundMetadata(false) + SourceTypeMetadata(LocalSourceType)
        val second = SourceTypeMetadata(RemoteSourceType) + LoadUuidMetadata("uuid-1")

        val result = first + second

        assertEquals(RemoteSourceType, result.sourceType)
        assertFalse(result.isLoadingInBackground)
        assertNotNull(result.get<LoadUuidMetadata>())
    }

}
