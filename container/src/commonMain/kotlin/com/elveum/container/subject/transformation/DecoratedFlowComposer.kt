package com.elveum.container.subject.transformation

import com.elveum.container.FlowComposer

/**
 * The receiver of [LoaderDecorator.decorate].
 *
 * In addition to the [FlowComposer] capabilities (flow dependencies), it lets
 * a decorator finish the load on its own, without delegating to the origin
 * loader. Both terminating functions never return: they unwind the decorator
 * body, so the origin loader is not executed if it has not been called yet.
 */
public interface DecoratedFlowComposer : FlowComposer {

    /**
     * Terminate the load with the [exception]. The exception
     * is propagated to collectors regardless of the current silent policy.
     *
     * Use it when a failure must always be visible, even for loads configured
     * with [com.elveum.container.LoadConfig.SilentLoadingAndError], which would
     * otherwise keep the previously cached value.
     */
    public fun completeWithFailure(exception: Exception): Nothing

    /**
     * Terminate the load and cleanup any cached values. Collectors
     * receive in-progress loading state.
     *
     * Use it to invalidate data that must not be shown anymore, e.g. after
     * signing out, so no stale value is left in the cache.
     */
    public fun completeWithCacheCleanUp(): Nothing

}
