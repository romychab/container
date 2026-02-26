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
    val backgroundLoadState: BackgroundLoadState get() = metadata.backgroundLoadState
    val reloadFunction: ReloadFunction get() = metadata.reloadFunction

    public fun reload(config: LoadConfig) {
        reloadFunction.invoke(config)
    }

}
