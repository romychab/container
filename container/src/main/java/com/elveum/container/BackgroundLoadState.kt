package com.elveum.container

/**
 * The background load state that can be observed via
 * container's metadata.
 */
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
