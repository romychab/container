package com.elveum.store.exceptions

/**
 * Any store can throw this exception in the Offline mode (if the request is configured to
 * fetch only from the local storage).
 */
public class NoCachedDataException : Exception(
    "No cached data. The fresh load is disabled by LoadRequestSource.Offline configuration."
)
