package com.elveum.container

import com.elveum.container.subject.LazyFlowSubject

/**
 * Reason why a loader function has been executed.
 * A [LoadTrigger] value can be accessed via [Emitter.loadTrigger].
 * For example:
 *
 * ```kotlin
 * LazyFlowSubject.create {
 *     if (loadTrigger != LoadTrigger.Reload) {
 *         emit(dataSource.loadFromLocalDatabase())
 *     }
 *     emit(dataSource.loadFromRemoteService())
 * }
 * ```
 */
public enum class LoadTrigger {

    /**
     * LazyFlowSubject has executed a new loader function assigned
     * via [LazyFlowSubject.newLoad]
     */
    NewLoad,

    /**
     * LazyFlowSubject has re-executed an old loader function due to
     * calling of [LazyFlowSubject.reload]
     */
    Reload,

    /**
     * An old loader function has been executed due to the expiration of
     * in-memory cache (this happens when all listeners have unsubscribed
     * and then subscribed again after the expiration period)
     */
    CacheExpired,

}
