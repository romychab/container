package com.elveum.store.internal.stores.common

import com.elveum.container.Emitter
import com.elveum.container.SourceType
import com.elveum.container.subject.paging.PageEmitter
import com.elveum.store.stores.paged.PagedList

internal interface CoreEmitter<T> {
    suspend fun emit(value: T, sourceType: SourceType, isLastValue: Boolean = false)

    companion object {

        fun <T> fromEmitter(emitter: Emitter<T>): CoreEmitter<T> {
            return object : CoreEmitter<T> {
                override suspend fun emit(
                    value: T,
                    sourceType: SourceType,
                    isLastValue: Boolean
                ) {
                    emitter.emit(value, sourceType, isLastValue)
                }
            }
        }

        fun <Key : Any, T : Any> fromPageEmitter(emitter: PageEmitter<Key, T>): CoreEmitter<PagedList<Key, T>> {
            return object : CoreEmitter<PagedList<Key, T>> {
                override suspend fun emit(
                    value: PagedList<Key, T>,
                    sourceType: SourceType,
                    isLastValue: Boolean
                ) {
                    emitter.emitPage(value.items, value.metadata)
                    if (value.nextKey != null) {
                        emitter.emitNextKey(value.nextKey)
                    }
                }
            }
        }
    }
}
