package com.elveum.store

import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.container.subject.transformation.LoaderDecorator
import com.elveum.store.builders.BasePagedBuilder
import com.elveum.store.builders.PagedBuilder
import com.elveum.store.builders.SimpleBuilder
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.internal.StoreFactoryImpl
import com.elveum.store.load.LoadRequest
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/**
 * An implementation of [StoreFactory] holding predefined default
 * values reused across all store instances created by the factory.
 */
public class SimpleStoreFactory(
    private val loaderDecorator: LoaderDecorator? = null,
    private val cacheTimeout: Duration? = null,
    private val coroutineContext: CoroutineContext? = null,
    private val coroutineScopeFactory: CoroutineScopeFactory? = null,
    private val loadRequestFlow: Flow<LoadRequest>? = null,
    private val fetchDistance: Int? = null,
) : StoreFactory {

    override fun <T : Any> simpleStoreBuilder(): SimpleBuilder<T> {
        return StoreFactoryImpl.simpleStoreBuilder<T>().configure()
    }

    override fun <PageKey : Any, T : Any> pagedStoreBuilder(
        initialKey: PageKey,
        itemId: (T) -> Any
    ): PagedBuilder<PageKey, T> {
        return StoreFactoryImpl.pagedStoreBuilder(initialKey, itemId).configure()
    }

    private fun <T : BaseBuilder<T>> T.configure(): T {
        var builder = this
        loaderDecorator?.let { builder = builder.setLoaderDecorator(it) }
        cacheTimeout?.let { builder = builder.setInMemoryCacheTimeout(it) }
        coroutineContext?.let { builder = builder.setCoroutineContext(it) }
        coroutineScopeFactory?.let { builder = builder.setCoroutineScopeFactory(it) }
        loadRequestFlow?.let { builder = builder.setLoadRequest(it) }
        fetchDistance?.let { builder = builder.configureFetchDistance(it) }
        return builder
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : BaseBuilder<T>> T.configureFetchDistance(fetchDistance: Int): T {
        return if (this is BasePagedBuilder<*>) {
            setFetchDistance(fetchDistance) as T
        } else {
            this
        }
    }
}
