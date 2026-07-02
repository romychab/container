package com.elveum.store.internal.stores.common

import com.elveum.container.Emitter

internal sealed class CoreFetcher<Key, Q, T> {

    class Custom<Key, Q, T>(
        val fetcher: suspend Emitter<T>.(Key, Q) -> Unit,
    ) : CoreFetcher<Key, Q, T>()

    class Default<Key, Q, T>(
        val fetcher: suspend (Key, Q) -> T,
    ) : CoreFetcher<Key, Q, T>()

}
