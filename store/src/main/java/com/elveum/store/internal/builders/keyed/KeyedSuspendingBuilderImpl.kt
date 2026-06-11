package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.KeyedSuspendingBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.KeyedSuspendingContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedStoreImpl
import com.elveum.store.stores.keyed.KeyedStore

internal class KeyedSuspendingBuilderImpl<Key : Any, T : Any>(
    private val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<KeyedSuspendingBuilder<Key, T>> = BaseBuilderImpl(config),
) : KeyedSuspendingBuilder<Key, T>, BaseBuilder<KeyedSuspendingBuilder<Key, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun build(contract: KeyedSuspendingContract<Key, T>): KeyedStore<Key, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onLoadFromStorage = contract::loadFromLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Key) -> T,
        onSaveToStorage: suspend (Key, T) -> Unit,
        onLoadFromStorage: suspend (Key) -> T?
    ): KeyedStore<Key, T> {
        return KeyedStoreImpl(
            config = config,
            fetcher = onFetch,
            saver = onSaveToStorage,
            loader = onLoadFromStorage,
        )
    }
}
