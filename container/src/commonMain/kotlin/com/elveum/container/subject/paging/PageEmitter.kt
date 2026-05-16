package com.elveum.container.subject.paging

import com.elveum.container.FlowComposer

/**
 * An instance of this emitter is available within the page loader function
 * when calling [pageLoader] for creating a new [PageLoader] instance.
 */
public interface PageEmitter<Key, T> : FlowComposer {

    /**
     * Emit data loaded for the current page key.
     *
     * Can be called multiple times.
     */
    public suspend fun emitPage(list: List<T>)

    /**
     * Emit a key of the next page to be loaded.
     *
     * Must be called either 1, or 0 times (if there is no next pages).
     */
    public fun emitNextKey(key: Key)

}
