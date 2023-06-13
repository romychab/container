@file:Suppress("unused")

package com.elveum.container

/**
 * Base interface for all source indicators
 */
interface SourceIndicator

/**
 * Indicates that data has arrived from a local data source
 */
object LocalSourceIndicator : SourceIndicator

/**
 * Indicates that data has arrived from a remote data source
 */
object RemoteSourceIndicator : SourceIndicator

/**
 * Indicates that data has arrived as a result of assignment from
 * some command in-place instead of loading from a data source
 */
object ImmediatelySourceIndicator : SourceIndicator

/**
 * Indicates that data has arrived from a fake data source
 */
object FakeSourceIndicator : SourceIndicator

/**
 * Indicates that data has arrived from an unknown data source
 */
object UnknownSourceIndicator : SourceIndicator