package com.elveum.container

/**
 * This config defines how the next data load will be propagated to subsequent
 * emitted containers.
 */
public enum class LoadConfig {

    /**
     * While loading new data, the Loading status is emitted.
     */
    Normal,

    /**
     * While loading new data, the old one remains observable, but with additional
     * attached metadata [BackgroundLoadState.Loading].
     */
    SilentLoading,

    /**
     * The old data remains observable if a new load is in progress or fails. You
     * can observe silent loading / error states using [BackgroundLoadMetadata].
     */
    SilentLoadingAndError;

    internal val isSilentLoadingEnabled: Boolean
        get() = this == SilentLoading || this == SilentLoadingAndError

    internal val isSilentErrorsEnabled: Boolean
        get() = this == SilentLoadingAndError

}
