package com.elveum.store.load

import com.elveum.store.exceptions.NoCachedDataException

/**
 * The request source determines where data should be loaded from.
 */
public enum class LoadRequestSource {

    /**
     * Fetch data from in-memory if possible. Do not trigger the reloading if
     * the cached value is available.
     */
    Default,

    /**
     * Ignore any cached values and execute a fresh data load from the remote
     * source.
     */
    Fresh,

    /**
     * Do not fetch data from the remote source; use only cached values.
     * If data does not exist in the cache, [NoCachedDataException] is emitted.
     */
    Offline,

}
