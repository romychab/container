package com.elveum.container

@Deprecated(
    message = "Data<T> will be removed in the future releases",
    replaceWith = ReplaceWith("SourceValue"),
)
public typealias Data<T> = SourceValue<T>

/**
 * Represents an unwrapped value from a container along with
 * the [source] where has the value arrived from.
 */
public data class SourceValue<T>(
    val value: T,
    val source: SourceType,
)
