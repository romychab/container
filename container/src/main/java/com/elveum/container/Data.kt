package com.elveum.container

/**
 * Represents an unwrapped value from a container along with
 * the [source] where has the value arrived from.
 */
data class Data<T>(
    val value: T,
    val source: SourceIndicator,
)