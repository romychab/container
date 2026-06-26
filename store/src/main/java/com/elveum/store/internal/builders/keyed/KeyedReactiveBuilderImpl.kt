package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.KeyedReactiveBuilder
import com.elveum.store.builders.KeyedReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.KeyedReactiveContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedStoreImpl
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.flow.Flow

internal class KeyedReactiveBuilderImpl<Key : Any, T : Any>(
    val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<KeyedReactiveBuilder<Key, T>> = BaseBuilderImpl(config)
) : KeyedReactiveBuilder<Key, T>, BaseBuilder<KeyedReactiveBuilder<Key, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun disableFetcher(): KeyedReactiveNoFetcherBuilder<Key, T> {
        return KeyedReactiveNoFetcherBuilderImpl(config)
    }

    override fun build(contract: KeyedReactiveContract<Key, T>): KeyedStore<Key, T> {
        return build(
            onFetch = contract::fetch,
            onSaveToStorage = contract::saveToLocalStorage,
            onObserveStorage = contract::observeLocalStorage,
        )
    }

    override fun build(
        onFetch: suspend (Key) -> T,
        onSaveToStorage: suspend (Key, T) -> Unit,
        onObserveStorage: (Key) -> Flow<T?>
    ): KeyedStore<Key, T> {
        return KeyedStoreImpl(
            config = config,
            fetcher = onFetch,
            saver = onSaveToStorage,
            observer = onObserveStorage,
        )
    }
}
