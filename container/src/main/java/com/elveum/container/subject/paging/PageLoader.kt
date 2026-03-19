package com.elveum.container.subject.paging

import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.StatefulValueLoader
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.paging.internal.PageLoaderImpl
import kotlinx.coroutines.flow.StateFlow

/**
 * Create a new [PageLoader] that can be assigned as a [ValueLoader] when instantiating
 * a new [LazyFlowSubject].
 *
 * Page loader can load multiple pages on demand:
 *
 * ```
 * val itemsPageLoader = pageLoader(initialKey = 0) { index ->
 *     val items = source.fetchItems(pageIndex = index, pageSize = 20)
 *     emitPage(items) // emit loaded items of the current page
 *     emitNextKey(index + 1) // emit a key for the next page to be loaded
 * }
 *
 * val subject = LazyFlowSubject.create(
 *     valueLoader = itemsPageLoader
 * )
 * ```
 *
 * The `emitPage` can be called multiple times. For example, you can load
 * data from a local storage at first, and after that - from a remote store.
 *
 * The `emitNextKey` can be called either 1 time (when there is a next page
 * available), or 0 time (if the current page is the last one).
 *
 * If [emitMetadata] is set to `true`, then emitted containers include additional
 * metadata values:
 * - `Container.metadata.nextPageState` - see [PageLoader.nextPageState]
 * - `Container.metadata.onItemRendered(index)` - see [PageLoader.onItemRendered]
 *
 * @param threshold fraction of the last loaded page that must be rendered before the
 * next page load is triggered. For example, if a page has 20 items and [threshold] is
 * `0.5`, the next page starts loading when the 10th item is rendered. Must be in
 * the range `[0.0, 1.0]`. Defaults to `0.5`.
 */
public fun <Key, T> pageLoader(
    initialKey: Key,
    threshold: Float = 0.5f,
    emitMetadata: Boolean = true,
    block: suspend PageEmitter<Key, T>.(Key) -> Unit
): PageLoader<Key, T> {
    return PageLoaderImpl(
        initialKey = initialKey,
        threshold = threshold,
        emitMetadata = emitMetadata,
        block = block,
    )
}

/**
 * A loader for fetching multiple pages of data. It can be created by using [pageLoader]
 * function.
 */
public interface PageLoader<Key, T> : StatefulValueLoader<List<T>> {

    /**
     * Listen the next page loading state.
     *
     * @see PageState
     */
    public val nextPageState: StateFlow<PageState>

    /**
     * Call this method when the item with the specified [index] has been rendered.
     *
     * This call notifies the loader to start loading a next page if needed. For example,
     * you can call it as a side effect within `LazyColumn`:
     *
     * ```
     * LazyColumn {
     *     itemsIndexed(list) { index, item ->
     *         SideEffect {
     *             pageLoader.onItemRendered(index)
     *         }
     *         Item(item)
     *     }
     * }
     * ```
     */
    public fun onItemRendered(index: Int)

}
