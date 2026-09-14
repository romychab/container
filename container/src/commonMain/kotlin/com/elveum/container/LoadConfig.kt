package com.elveum.container

/**
 * Defines how the next data load is propagated to the containers emitted while it runs.
 *
 * Use the predefined presets - [Normal], [SilentLoading] and [SilentLoadingAndError] - and,
 * for the silent ones, optionally attach the [ReplaceErrorsOnReload] flag via the `+` operator:
 *
 * ```
 * LoadConfig.SilentLoading + ReplaceErrorsOnReload
 * ```
 *
 * The configuration is described by three orthogonal flags rather than a fixed enumeration, so
 * new presets can be added without breaking existing code that references the presets above.
 */
@ConsistentCopyVisibility
public data class LoadConfig internal constructor(
    /**
     * While new data is loading, the currently loaded value stays observable (with additional
     * [BackgroundLoadState.Loading] metadata) instead of being replaced by [Container.Pending].
     */
    public val isSilentLoadingEnabled: Boolean,

    /**
     * When a new load fails, the currently loaded value stays observable (with additional
     * [BackgroundLoadState.Error] metadata) instead of being replaced by an error container.
     */
    public val isSilentErrorsEnabled: Boolean,

    /**
     * Applies only to silent configs: while reloading, a current *error* state is replaced by
     * [Container.Pending] instead of being kept visible. In other words, content is kept
     * silently only while it is actually loaded - a stale error is not. Set it with the
     * [ReplaceErrorsOnReload] flag.
     */
    public val replaceErrorsOnReload: Boolean,
) {

    /**
     * Attaches the [ReplaceErrorsOnReload] flag to a silent config. For a non-silent config
     * (such as [Normal]) there is no kept content to replace, so the receiver is returned
     * unchanged.
     *
     * ```
     * LoadConfig.SilentLoading + ReplaceErrorsOnReload
     * ```
     */
    public operator fun plus(flag: ReplaceErrorsOnReload): LoadConfig {
        if (!isSilentLoadingEnabled) return this
        return copy(replaceErrorsOnReload = true)
    }

    public companion object {

        /**
         * While loading new data, the [Container.Pending] status is emitted.
         */
        public val Normal: LoadConfig = LoadConfig(
            isSilentLoadingEnabled = false,
            isSilentErrorsEnabled = false,
            replaceErrorsOnReload = false,
        )

        /**
         * While loading new data, the old one remains observable, but with additional
         * attached metadata [BackgroundLoadState.Loading].
         */
        public val SilentLoading: LoadConfig = LoadConfig(
            isSilentLoadingEnabled = true,
            isSilentErrorsEnabled = false,
            replaceErrorsOnReload = false,
        )

        /**
         * The old data remains observable if a new load is in progress or fails. You
         * can observe silent loading / error states using [BackgroundLoadMetadata].
         */
        public val SilentLoadingAndError: LoadConfig = LoadConfig(
            isSilentLoadingEnabled = true,
            isSilentErrorsEnabled = true,
            replaceErrorsOnReload = false,
        )
    }
}

/**
 * Flag that can be attached to a silent [LoadConfig] ([LoadConfig.SilentLoading] or
 * [LoadConfig.SilentLoadingAndError]) via the `+` operator. When attached, a reload stays
 * silent only while the current value is loaded; a current error is replaced by
 * [Container.Pending] on reload instead of being kept.
 *
 * Usage example:
 *
 * ```
 * LoadConfig.SilentLoading + ReplaceErrorsOnReload
 * ```
 */
public data object ReplaceErrorsOnReload
