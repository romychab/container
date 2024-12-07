package com.elveum.container.cache

import com.elveum.container.Container
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.LazyFlowSubjectImpl
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.factories.CoroutineScopeFactory
import com.elveum.container.subject.newAsyncLoad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class LazyCacheImpl<Arg, T>(
    private val cacheTimeoutMillis: Long,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val loader: CacheValueLoader<Arg, T>,
    private val subjectFactory: LazyFlowSubjectFactory = LazyFlowSubjectFactory.Default(
        cacheTimeoutMillis, coroutineScopeFactory
    )
) : LazyCache<Arg, T> {

    override fun listen(arg: Arg): Flow<Container<T>> {
        TODO("Not yet implemented")
    }

    override fun isValueLoading(arg: Arg): StateFlow<Boolean> {
        TODO("Not yet implemented")
    }

    override fun get(arg: Arg): Container<T> {
        TODO("Not yet implemented")
    }

    override fun getActiveCollectorsCount(arg: Arg): Int {
        TODO("Not yet implemented")
    }

    override fun reload(arg: Arg, silently: Boolean): Flow<T> {
        TODO("Not yet implemented")
    }

    override fun updateWith(arg: Arg, container: Container<T>) {
        TODO("Not yet implemented")
    }

    interface LazyFlowSubjectFactory {

        fun <T> create(loader: ValueLoader<T>): LazyFlowSubject<T>

        class Default(
            private val cacheTimeoutMillis: Long,
            private val coroutineScopeFactory: CoroutineScopeFactory,
        ) : LazyFlowSubjectFactory {
            override fun <T> create(loader: ValueLoader<T>): LazyFlowSubject<T> {
                return LazyFlowSubjectImpl<T>(
                    coroutineScopeFactory = coroutineScopeFactory,
                    cacheTimeoutMillis = cacheTimeoutMillis,
                ).apply {
                    newAsyncLoad(valueLoader = loader)
                }
            }
        }
    }

}