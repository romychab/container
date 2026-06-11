package com.elveum.container.subject.paging

import com.elveum.container.ContainerMetadata
import com.elveum.container.FlowComposer

/**
 * An instance of this emitter is available within the page loader function
 * when calling [pageLoader] for creating a new [PageLoader] instance.
 */
public interface PageEmitter<Key, T> : FlowComposer {

    /**
     * Input metadata of the current load. It may contain the load trigger,
     * or custom values, etc.
     */
    public val metadata: ContainerMetadata

    /**
     * Emit data loaded for the current page key.
     *
     * - Must be called 1, or more times.
     */
    public suspend fun emitPage(list: List<T>)

    /**
     * Emit a key of the next page to be loaded (if any).
     *
     * - If there is no next page, just do not call this method.
     * - Can be called 0, 1, or multiple times (e.g. with keys from a local storage, and then with keys
     *   from a remote source).
     */
    public suspend fun emitNextKey(key: Key)

}
