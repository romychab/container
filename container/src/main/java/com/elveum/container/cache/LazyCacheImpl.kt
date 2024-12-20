package com.elveum.container.cache

import com.elveum.container.Container
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.LazyFlowSubjectImpl
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.factories.CoroutineScopeFactory
import com.elveum.container.subject.newAsyncLoad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

internal class LazyCacheImpl<Arg, T>(
    private val cacheTimeoutMillis: Long,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val loader: CacheValueLoader<Arg, T>,
    private val subjectFactory: LazyFlowSubjectFactory = LazyFlowSubjectFactory.Default(
        cacheTimeoutMillis, coroutineScopeFactory
    )
) : LazyCache<Arg, T> {

    private val cacheSlots = mutableMapOf<Arg, CacheRecord<T>>()
    private val totalCount: Int get() = cacheSlots.values.sumOf { it.count }
    private var scope: CoroutineScope? = null

    override fun listen(arg: Arg): Flow<Container<T>> {
        return flow {
            useCacheSlot(arg) { subject ->
                subject.listen().collect(this)
            }
        }
    }

    override fun isValueLoading(arg: Arg): StateFlow<Boolean> {
        return ValueLoadingStateFlowImpl(arg)
    }

    override fun get(arg: Arg): Container<T> {
        return getSubject(arg)?.currentValue ?: Container.Pending
    }

    override fun getActiveCollectorsCount(arg: Arg): Int {
        return getSubject(arg)?.activeCollectorsCount ?: 0
    }

    override fun reload(arg: Arg, silently: Boolean): Flow<T> {
        return getSubject(arg)?.reload(silently) ?: emptyFlow()
    }

    override fun updateWith(arg: Arg, container: Container<T>) {
        getSubject(arg)?.updateWith(container)
    }

    private fun registerRecord(arg: Arg): CacheRecord<T> = synchronized(this) {
        val record = cacheSlots.computeIfAbsent(arg) {
            val subject = subjectFactory.create {
                loader.invoke(this, arg)
            }
            CacheRecord(subject)
        }
        record.count++
        if (totalCount == 1 && scope == null) {
            scope = coroutineScopeFactory.createScope()
        }
        return record
    }

    private fun unregisterRecord(arg: Arg) = synchronized(this) {
        val record = cacheSlots[arg] ?: return
        record.count--
        if (record.count == 0) {
            scheduleRemoval(arg)
        }
    }

    private fun scheduleRemoval(arg: Arg) {
        scope?.launch {
            delay(cacheTimeoutMillis)
            synchronized(this) {
                val record = cacheSlots[arg]
                if (record?.count == 0) {
                    cacheSlots.remove(arg)
                }
                if (totalCount == 0) {
                    scope?.cancel()
                    scope = null
                }
            }
        }
    }

    private inline fun <R> useCacheSlot(arg: Arg, block: (LazyFlowSubject<T>) -> R): R {
        try {
            val record = registerRecord(arg)
            return block(record.subject)
        } finally {
            unregisterRecord(arg)
        }
    }

    private fun getSubject(arg: Arg): LazyFlowSubject<T>? {
        return cacheSlots[arg]?.subject
    }

    private class CacheRecord<T>(
        val subject: LazyFlowSubject<T>,
        var count: Int = 0,
    )

    private inner class ValueLoadingStateFlowImpl(
        private val arg: Arg,
    ) : StateFlow<Boolean> {

        private val origin: StateFlow<Boolean>? get() = getSubject(arg)?.isValueLoading()

        override val replayCache: List<Boolean> get() = origin?.replayCache ?: listOf(false)
        override val value: Boolean get() = origin?.value ?: false

        override suspend fun collect(collector: FlowCollector<Boolean>): Nothing {
            useCacheSlot(arg) { subject ->
                subject.isValueLoading().collect(collector)
            }
        }
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
