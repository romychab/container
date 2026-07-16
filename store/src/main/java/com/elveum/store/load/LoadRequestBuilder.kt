package com.elveum.store.load

public interface BaseLoadRequestBuilder {

    /**
     * Create a [LoadRequest] request instance based on configuration provided
     * by this builder.
     */
    public fun build(): LoadRequest

}

public interface LoadRequestBuilder :
    LoadRequestSourceModeBuilder,
    LoadRequestKeepOnLoadBuilder,
    LoadRequestKeepOnQueryBuilder,
    LoadRequestKeepOnBuilder,
    LoadRequestModeAndKeepQueryBuilder,
    LoadRequestModeAndKeepLoadBuilder {

    /**
     * Keep existing content in all observed flows while new content is being loaded by changed query
     * even if the new content loading has been failed.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnQuery when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    override fun keepContentOnQueryAndError(replaceErrorsOnQuery: Boolean): LoadRequestModeAndKeepLoadBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded by changed query.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnQuery when `true` (the default), a current *error* state is
     *   replaced by a loading state on query change instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    override fun keepContentOnQuery(replaceErrorsOnQuery: Boolean): LoadRequestModeAndKeepLoadBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded
     * even if the new content loading has been failed.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnReload when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    override fun keepContentOnLoadAndError(replaceErrorsOnReload: Boolean): LoadRequestModeAndKeepQueryBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnReload when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    override fun keepContentOnLoad(replaceErrorsOnReload: Boolean): LoadRequestModeAndKeepQueryBuilder

    /**
     * Ignore cached values and force re-fetching from the remote source.
     */
    override fun freshMode(): LoadRequestKeepOnBuilder

    /**
     * Use only cached data. If there is no cached values, emit an error.
     */
    override fun offlineMode(): LoadRequestKeepOnBuilder

    /**
     * Create a [LoadRequest] request instance based on configuration provided
     * by this builder.
     */
    override fun build(): LoadRequest
}


public interface LoadRequestKeepOnBuilder : LoadRequestKeepOnLoadBuilder, LoadRequestKeepOnQueryBuilder {

    /**
     * Keep existing content in all observed flows while new content is being loaded.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnReload when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    override fun keepContentOnLoad(replaceErrorsOnReload: Boolean): LoadRequestKeepOnQueryBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded
     * even if the new content loading has been failed.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnReload when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    override fun keepContentOnLoadAndError(replaceErrorsOnReload: Boolean): LoadRequestKeepOnQueryBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded by changed query.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnQuery when `true` (the default), a current *error* state is
     *   replaced by a loading state on query change instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    override fun keepContentOnQuery(replaceErrorsOnQuery: Boolean): LoadRequestKeepOnLoadBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded by changed query
     * even if the new content loading has been failed.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnQuery when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    override fun keepContentOnQueryAndError(replaceErrorsOnQuery: Boolean): LoadRequestKeepOnLoadBuilder

    /**
     * Create a [LoadRequest] request instance based on configuration provided
     * by this builder.
     */
    override fun build(): LoadRequest
}

public interface LoadRequestModeAndKeepQueryBuilder : LoadRequestSourceModeBuilder, LoadRequestKeepOnQueryBuilder {

    /**
     * Use only cached data. If there is no cached values, emit an error.
     */
    override fun offlineMode(): LoadRequestKeepOnQueryBuilder

    /**
     * Ignore cached values and force re-fetching from the remote source.
     */
    override fun freshMode(): LoadRequestKeepOnQueryBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded by changed query.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnQuery when `true` (the default), a current *error* state is
     *   replaced by a loading state on query change instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    override fun keepContentOnQuery(replaceErrorsOnQuery: Boolean): LoadRequestSourceModeBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded by changed query
     * even if the new content loading has been failed.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnQuery when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    override fun keepContentOnQueryAndError(replaceErrorsOnQuery: Boolean): LoadRequestSourceModeBuilder

    /**
     * Create a [LoadRequest] request instance based on configuration provided
     * by this builder.
     */
    override fun build(): LoadRequest
}

public interface LoadRequestModeAndKeepLoadBuilder : LoadRequestSourceModeBuilder, LoadRequestKeepOnLoadBuilder {

    /**
     * Use only cached data. If there is no cached values, emit an error.
     */
    override fun offlineMode(): LoadRequestKeepOnLoadBuilder

    /**
     * Ignore cached values and force re-fetching from the remote source.
     */
    override fun freshMode(): LoadRequestKeepOnLoadBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded
     * even if the new content loading has been failed.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnReload when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    override fun keepContentOnLoadAndError(replaceErrorsOnReload: Boolean): LoadRequestSourceModeBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnReload when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    override fun keepContentOnLoad(replaceErrorsOnReload: Boolean): LoadRequestSourceModeBuilder

    /**
     * Create a [LoadRequest] request instance based on configuration provided
     * by this builder.
     */
    override fun build(): LoadRequest
}

public interface LoadRequestKeepOnQueryBuilder : BaseLoadRequestBuilder {

    /**
     * Keep existing content in all observed flows while new content is being loaded by changed query.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnQuery when `true` (the default), a current *error* state is
     *   replaced by a loading state on query change instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    public fun keepContentOnQuery(replaceErrorsOnQuery: Boolean = true): BaseLoadRequestBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded by changed query
     * even if the new content loading has been failed.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnQuery when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    public fun keepContentOnQueryAndError(replaceErrorsOnQuery: Boolean = true): BaseLoadRequestBuilder

    /**
     * Create a [LoadRequest] request instance based on configuration provided
     * by this builder.
     */
    override fun build(): LoadRequest
}

public interface LoadRequestKeepOnLoadBuilder : BaseLoadRequestBuilder {

    /**
     * Keep existing content in all observed flows while new content is being loaded.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnReload when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    public fun keepContentOnLoad(replaceErrorsOnReload: Boolean = true): BaseLoadRequestBuilder

    /**
     * Keep existing content in all observed flows while new content is being loaded
     * even if the new content loading has been failed.
     *
     * You can still observe the load state using background metadata.
     *
     * @param replaceErrorsOnReload when `true` (the default), a current *error* state is
     *   replaced by a loading state on reload instead of being kept silently; i.e. content is
     *   kept only while it is actually loaded.
     */
    public fun keepContentOnLoadAndError(replaceErrorsOnReload: Boolean = true): BaseLoadRequestBuilder

    /**
     * Create a [LoadRequest] request instance based on configuration provided
     * by this builder.
     */
    override fun build(): LoadRequest
}

public interface LoadRequestSourceModeBuilder : BaseLoadRequestBuilder {

    /**
     * Use only cached data. If there is no cached values, emit an error.
     */
    public fun offlineMode(): BaseLoadRequestBuilder

    /**
     * Ignore cached values and force re-fetching from the remote source.
     */
    public fun freshMode(): BaseLoadRequestBuilder

    /**
     * Create a [LoadRequest] request instance based on configuration provided
     * by this builder.
     */
    override fun build(): LoadRequest
}
