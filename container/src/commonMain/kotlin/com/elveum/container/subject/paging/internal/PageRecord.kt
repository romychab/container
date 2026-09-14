package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
import com.elveum.container.pendingContainer

internal interface PageRecord<Key, T> {
    val pageIndex: Int
    val pageKey: Key
    val priority: Long
    val container: Container<List<T>>?
    val isCompleted: Boolean

    val safeContainer: Container<List<T>> get() = container ?: pendingContainer()

    fun immutable(): ImmutablePageRecord<Key, T>
}

internal class MutablePageRecord<Key, T>(
    override val pageIndex: Int,
    override val pageKey: Key,
    override val priority: Long,
    override var container: Container<List<T>>? = null,
    override var isCompleted: Boolean = false,
) : PageRecord<Key, T> {
    override fun immutable() = ImmutablePageRecord(pageIndex, pageKey, priority, container, isCompleted)
}

internal data class ImmutablePageRecord<Key, T>(
    override val pageIndex: Int,
    override val pageKey: Key,
    override val priority: Long,
    override val container: Container<List<T>>?,
    override val isCompleted: Boolean,
) : PageRecord<Key, T> {
    override fun immutable(): ImmutablePageRecord<Key, T> = this
}
