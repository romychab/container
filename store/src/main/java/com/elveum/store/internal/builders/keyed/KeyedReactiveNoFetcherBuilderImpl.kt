package com.elveum.store.internal.builders.keyed

import com.elveum.store.builders.KeyedReactiveNoFetcherBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.contracts.KeyedReactiveNoFetcherContract
import com.elveum.store.internal.builders.BaseBuilderImpl
import com.elveum.store.internal.builders.SharedConfig
import com.elveum.store.internal.stores.KeyedStoreImpl
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal class KeyedReactiveNoFetcherBuilderImpl<Key : Any, T : Any>(
    val config: SharedConfig,
    sharedBuilder: BaseBuilderImpl<KeyedReactiveNoFetcherBuilder<Key, T>> = BaseBuilderImpl(config)
) : KeyedReactiveNoFetcherBuilder<Key, T>, BaseBuilder<KeyedReactiveNoFetcherBuilder<Key, T>> by sharedBuilder {

    init {
        sharedBuilder.setReference(this)
    }

    override fun build(contract: KeyedReactiveNoFetcherContract<Key, T>): KeyedStore<Key, T> {
        return build(onObserve = contract::observe)
    }

    override fun build(onObserve: (Key) -> Flow<T>): KeyedStore<Key, T> {
        return KeyedStoreImpl(
            fetcher = { key -> onObserve(key).first() },
            saver = { _, _ -> },
            observer = { key -> onObserve(key) },
            config = config,
        )
    }
}
