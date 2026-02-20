package com.elveum.container

/**
 * Represents an unwrapped value from a container along with
 * the [source] where has the value arrived from.
 */
public data class ContainerValue<out T>(
    val value: T,
    val metadata: ContainerMetadata,
) {

    val source: SourceType get() = metadata.sourceType
    val isLoadingInBackground: Boolean get() = metadata.isLoadingInBackground
    val reloadFunction: ReloadFunction get() = metadata.reloadFunction

    public fun reload(silently: Boolean) {
        reloadFunction.invoke(silently)
    }

}
