package com.elveum.store.internal.stores.common

import com.elveum.container.subject.ValueLoader
import com.elveum.store.load.LoadRequestSource

internal interface CoreValueLoaderProvider<Key : Any, Q : Any, T : Any, R : Any> {
    fun provideValueLoader(
        key: Key,
        querySource: () -> Q,
        requestSource: LoadRequestSource,
        delegate: CoreLoaderDelegate<R>,
    ): ValueLoader<T>
}
