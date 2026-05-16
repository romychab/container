package com.elveum.container

import androidx.compose.runtime.Immutable

/**
 * Represents an unwrapped value from a container along with
 * the [sourceType] where has the value arrived from.
 */
@Immutable
public data class ContainerValue<out T>(
    val value: T,
    val metadata: ContainerMetadata,
) {

    val sourceType: SourceType get() = metadata.sourceType
    val backgroundLoadState: BackgroundLoadState get() = metadata.backgroundLoadState
    val reloadFunction: ReloadFunction get() = metadata.reloadFunction

    public fun reload(config: LoadConfig) {
        reloadFunction.invoke(config)
    }

}
