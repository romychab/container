package com.elveum.container.cache

import com.elveum.container.Container
import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.container.subject.ContainerConfiguration
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.transformation.ContainerTransformation
import com.elveum.container.subject.transformation.EmptyContainerTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

internal class LazyCacheImpl<Arg, T>(
    private val cacheTimeoutMillis: Long,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    transformation: ContainerTransformation<T> = EmptyContainerTransformation(),
    private val valueLoader: CacheValueLoader<Arg, T>,
    private val subjectFactory: LazyFlowSubjectFactory<T> = LazyFlowSubjectFactory.Default(
        cacheTimeoutMillis, coroutineScopeFactory, transformation,
    )
) : LazyCache<Arg, T> {

    private val cacheSlots = mutableMapOf<Arg, CacheRecord<T>>()
    private val totalCount: Int get() = cacheSlots.values.sumOf { it.count }
    private var scope: CoroutineScope? = null

    override fun listen(
        arg: Arg,
        configuration: ContainerConfiguration,
    ): StateFlow<Container<T>> {
        return ListenStateFlowImpl(arg, configuration)
    }

    override fun get(
        arg: Arg,
        configuration: ContainerConfiguration,
    ): Container<T> {
        return getSubject(arg)?.currentValue(configuration) ?: Container.Pending
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

    override fun reset() = synchronized(this) {
        val iterator = cacheSlots.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.count == 0) {
                iterator.remove()
            }
        }
        if (totalCount == 0) {
            scope?.cancel()
            scope = null
        }
    }

    private fun registerRecord(arg: Arg): CacheRecord<T> = synchronized(this) {
        val record = cacheSlots.getOrPut(arg) {
            val subject = subjectFactory.create {
                valueLoader.invoke(this, arg)
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

    private fun getSubject(arg: Arg): LazyFlowSubject<T>? = synchronized(this) {
        return cacheSlots[arg]?.subject
    }

    private class CacheRecord<T>(
        val subject: LazyFlowSubject<T>,
        var count: Int = 0,
    )

    private inner class ListenStateFlowImpl(
        private val arg: Arg,
        private val configuration: ContainerConfiguration,
    ): StateFlow<Container<T>> {

        override val replayCache: List<Container<T>> get() = listOf(value)
        override val value: Container<T>
            get() = getSubject(arg)?.currentValue(configuration) ?: Container.Pending

        override suspend fun collect(collector: FlowCollector<Container<T>>): Nothing {
            useCacheSlot(arg) { subject ->
                subject.listen(configuration).collect(collector)
            }
        }

    }

    interface LazyFlowSubjectFactory<T> {

        fun create(
            valueLoader: ValueLoader<T>,
        ): LazyFlowSubject<T>

        class Default<T>(
            private val cacheTimeoutMillis: Long,
            private val coroutineScopeFactory: CoroutineScopeFactory,
            private val transformation: ContainerTransformation<T>,
        ) : LazyFlowSubjectFactory<T> {

            override fun create(
                valueLoader: ValueLoader<T>,
            ): LazyFlowSubject<T> {
                return LazyFlowSubject.create(
                    cacheTimeoutMillis = cacheTimeoutMillis,
                    coroutineScopeFactory = coroutineScopeFactory,
                    transformation = transformation,
                    valueLoader = valueLoader,
                )
            }

        }
    }

}
