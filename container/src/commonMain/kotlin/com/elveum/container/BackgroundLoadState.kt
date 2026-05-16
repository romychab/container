package com.elveum.container

import androidx.compose.runtime.Immutable

/**
 * The background load state that can be observed via
 * container's metadata.
 */
@Immutable
public sealed class BackgroundLoadState {

    /**
     * No active pending background loads.
     */
    public data object Idle : BackgroundLoadState()

    /**
     * New data is being loaded right now in background.
     */
    public data object Loading : BackgroundLoadState()

    /**
     * The previous background load has been failed.
     */
    public data class Error(
        val exception: Exception
    ) : BackgroundLoadState()

}
