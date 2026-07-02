package com.elveum.store.load

public interface LoadRequestSourceBuilder : BaseLoadRequestBuilder {

    /**
     * Use only cached data. If there is no cached values, emit an error.
     */
    public fun offlineMode(): LoadRequestKeepContentBuilder

    /**
     * Ignore cached values and force re-fetching from the remote source.
     */
    public fun freshMode(): LoadRequestKeepContentBuilder

}

public interface LoadRequestKeepContentBuilder : BaseLoadRequestBuilder {

    /**
     * Keep existing content in all observed flows while new content is being loaded.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnReload when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    public fun keepContentOnLoad(replaceErrorsOnReload: Boolean = true): LoadRequestSourceBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded and
     * if the new content loading has been failed.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnReload when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    public fun keepContentOnLoadAndError(replaceErrorsOnReload: Boolean = true): LoadRequestSourceBuilder
}

public interface BaseLoadRequestBuilder {

    /**
     * Create a [LoadRequest] request instance based on configuration provided
     * by this builder.
     */
    public fun build(): LoadRequest

}

public interface LoadRequestBuilder :
    LoadRequestSourceBuilder,
    LoadRequestKeepContentBuilder,
    BaseLoadRequestBuilder
