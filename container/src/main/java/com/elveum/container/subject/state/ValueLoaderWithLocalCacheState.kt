package com.elveum.container.subject.state

import com.elveum.container.subject.ValueLoader

public interface EmitterWithLocalCacheState<T> : EmitterWithState<T, Boolean> {
    /**
     * Whether the loader should use a local cache for loading values or not.
     * You can read this value from a loader created by [loaderWithLocalCacheState]
     * function.
     */
    public val useLocalCache: Boolean
}

private class EmitterWithLocalCacheStateImpl<T>(
    originEmitter: EmitterWithState<T, Boolean>
) : EmitterWithState<T, Boolean> by originEmitter, EmitterWithLocalCacheState<T> {
    override val useLocalCache: Boolean get() = state
}

/**
 * Create a value loader which has an additional state containing a flag
 * [EmitterWithLocalCacheState.useLocalCache].
 * This flag is equal to [useLocalCacheInitialValue] argument during the first execution. And for all
 * next executions, the flag value is set to `true`.
 *
 * May be useful for implementing "refresh" or "try-again" behavior, when data should
 * be loaded directly from the remote source skipping the local one during the
 * first execution.
 */
public fun <T> loaderWithLocalCacheState(
    useLocalCacheInitialValue: Boolean = true,
    loader: suspend EmitterWithLocalCacheState<T>.() -> Unit
): ValueLoader<T> {
    return loaderWithState(useLocalCacheInitialValue) {
        EmitterWithLocalCacheStateImpl(originEmitter = this).loader()
        updateState(state = true)
    }
}
