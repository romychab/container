package com.elveum.store.internal.builders

import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.store.builders.base.BaseBuilder
import com.elveum.store.load.LoadRequest
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

internal open class BaseBuilderImpl<OutBuilder : Any>(
    val config: SharedConfig = SharedConfig(),
) : BaseBuilder<OutBuilder> {

    protected lateinit var ref: OutBuilder

    override fun setInMemoryCacheTimeout(duration: Duration): OutBuilder {
        config.inMemoryCacheTimeout = duration
        return ref
    }

    override fun setCoroutineContext(context: CoroutineContext): OutBuilder {
        config.coroutineContext = context
        return ref
    }

    override fun setCoroutineScopeFactory(factory: CoroutineScopeFactory): OutBuilder {
        config.coroutineScopeFactory = factory
        return ref
    }

    override fun setLoadRequest(flow: Flow<LoadRequest>): OutBuilder {
        config.loadRequestFlow = flow
        return ref
    }

    fun setReference(reference: OutBuilder) {
        this.ref = reference
    }

}
