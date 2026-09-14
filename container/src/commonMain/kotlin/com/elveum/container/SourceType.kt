package com.elveum.container

import androidx.compose.runtime.Immutable

/**
 * Base interface for all source types.
 */
@Immutable
public interface SourceType

/**
 * Indicates that data has arrived from a local data source.
 */
public data object LocalSourceType : SourceType

/**
 * Indicates that data has arrived from a remote data source.
 */
public data object RemoteSourceType : SourceType

/**
 * Indicates that data has arrived as a result of assignment from
 * some command in-place instead of loading from a data source.
 */
public data object ImmediateSourceType : SourceType

/**
 * Indicates that data has arrived from a fake data source.
 */
public data object FakeSourceType : SourceType

/**
 * Indicates that data has arrived from an unknown data source.
 */
public data object UnknownSourceType : SourceType
