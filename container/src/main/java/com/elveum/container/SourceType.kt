@file:Suppress("unused")

package com.elveum.container

/**
 * Base interface for all source types
 */
public interface SourceType

/**
 * Indicates that data has arrived from a local data source
 */
public object LocalSourceType : SourceType

/**
 * Indicates that data has arrived from a remote data source
 */
public object RemoteSourceType : SourceType

/**
 * Indicates that data has arrived as a result of assignment from
 * some command in-place instead of loading from a data source
 */
public object ImmediateSourceType : SourceType

/**
 * Indicates that data has arrived from a fake data source
 */
public object FakeSourceType : SourceType

/**
 * Indicates that data has arrived from an unknown data source
 */
public object UnknownSourceType : SourceType

@Deprecated(
    message = "SourceIndicator will be removed in the future releases",
    replaceWith = ReplaceWith("SourceType"),
)
public typealias SourceIndicator = SourceType

@Deprecated(
    message = "LocalSourceIndicator will be removed in the future releases",
    replaceWith = ReplaceWith("SourceType"),
)
public typealias LocalSourceIndicator = LocalSourceType

@Deprecated(
    message = "RemoteSourceIndicator will be removed in the future releases",
    replaceWith = ReplaceWith("RemoteSourceType"),
)
public typealias RemoteSourceIndicator = RemoteSourceType

@Deprecated(
    message = "ImmediatelySourceIndicator will be removed in the future releases",
    replaceWith = ReplaceWith("ImmediateSourceType"),
)
public typealias ImmediatelySourceIndicator = ImmediateSourceType

@Deprecated(
    message = "FakeSourceIndicator will be removed in the future releases",
    replaceWith = ReplaceWith("FakeSourceType"),
)
public typealias FakeSourceIndicator = FakeSourceType

@Deprecated(
    message = "UnknownSourceIndicator will be removed in the future releases",
    replaceWith = ReplaceWith("UnknownSourceType"),
)
public typealias UnknownSourceIndicator = UnknownSourceType
