@file:Suppress("unused")

package com.elveum.container

/**
 * Base interface for all source indicators
 */
public interface SourceIndicator

/**
 * Indicates that data has arrived from a local data source
 */
public object LocalSourceIndicator : SourceIndicator

/**
 * Indicates that data has arrived from a remote data source
 */
public object RemoteSourceIndicator : SourceIndicator

/**
 * Indicates that data has arrived as a result of assignment from
 * some command in-place instead of loading from a data source
 */
public object ImmediatelySourceIndicator : SourceIndicator

/**
 * Indicates that data has arrived from a fake data source
 */
public object FakeSourceIndicator : SourceIndicator

/**
 * Indicates that data has arrived from an unknown data source
 */
public object UnknownSourceIndicator : SourceIndicator