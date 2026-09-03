package com.elveum.container

/**
 * Return a new list with the item identified by [newItem]'s id replaced by
 * [newItem].
 *
 * The item is located by identity rather than by position: [itemId] is applied
 * to [newItem] and to each element, and the first element with a matching id is
 * replaced. If no element matches, the original list is returned unchanged -
 * the list may be filtered, sorted or paged differently from wherever
 * [newItem] came from, so its absence is normal and not an error.
 *
 * Typical use is patching a cached list after a mutation:
 *
 * ```
 * subject.updateIfSuccess { products ->
 *     products.updateItem(updatedProduct) { it.id }
 * }
 * ```
 *
 * @param T the element type of the list.
 * @param newItem the replacement item.
 * @param itemId extracts the identity of an item; the same selector shape
 *        accepted by `pageLoader`.
 */
public inline fun <T> List<T>.updateItem(
    newItem: T,
    itemId: (T) -> Any,
): List<T> {
    return updateItem(itemId(newItem), itemId) { newItem }
}

/**
 * Return a new list with the item whose id equals [id] replaced by the result
 * of [transform].
 *
 * Use this overload when the new value is derived from the old one. If no
 * element matches [id], the original list is returned unchanged and
 * [transform] is not called.
 *
 * ```
 * subject.updateIfSuccess { products ->
 *     products.updateItem(productId, { it.id }) { it.copy(isFavorite = true) }
 * }
 * ```
 *
 * @param T the element type of the list.
 * @param id identity of the item to replace.
 * @param itemId extracts the identity of an item.
 * @param transform produces the replacement from the current item.
 */
public inline fun <T> List<T>.updateItem(
    id: Any,
    itemId: (T) -> Any,
    transform: (T) -> T,
): List<T> {
    val index = indexOfFirst { itemId(it) == id }
    if (index < 0) return this
    val updated = toMutableList()
    updated[index] = transform(updated[index])
    return updated
}

/**
 * Return a new container whose list has the item identified by [newItem]'s id
 * replaced by [newItem].
 *
 * Non-`Success` containers are returned unchanged, as are lists that do not
 * contain a matching item.
 *
 * @see List.updateItem
 */
public inline fun <T> Container<List<T>>.updateItem(
    newItem: T,
    itemId: (T) -> Any,
): Container<List<T>> {
    return map { list -> list.updateItem(newItem, itemId) }
}

/**
 * Return a new container whose list has the item with the given [id] replaced
 * by the result of [transform].
 *
 * Non-`Success` containers are returned unchanged, as are lists that do not
 * contain a matching item.
 *
 * @see List.updateItem
 */
public inline fun <T> Container<List<T>>.updateItem(
    id: Any,
    itemId: (T) -> Any,
    transform: (T) -> T,
): Container<List<T>> {
    return map { list -> list.updateItem(id, itemId, transform) }
}
