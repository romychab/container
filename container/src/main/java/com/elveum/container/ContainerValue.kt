package com.elveum.container

/**
 * Represents an unwrapped value from a container along with
 * the [source] where has the value arrived from.
 */
public data class ContainerValue<out T>(
    val value: T,
    val source: SourceType,
    val isLoadingInBackground: Boolean,
    val reloadFunction: ReloadFunction,
) {

    public fun reload(silently: Boolean) {
        reloadFunction.invoke(silently)
    }

}
