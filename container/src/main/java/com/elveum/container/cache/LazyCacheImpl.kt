@file:OptIn(ExperimentalForInheritanceCoroutinesApi::class)

package com.elveum.container.cache

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.container.stateMap
import com.elveum.container.subject.ContainerConfiguration
import com.elveum.container.subject.LazyFlowSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class LazyCacheImpl<Arg, T>(
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val cacheTimeoutMillis: Long,
    private val factory: LazyFlowSubjectFactory<Arg, T>,
) : LazyCache<Arg, T> {

    private val cacheSlotsFlow = MutableStateFlow<Map<Arg, CacheRecord<T>>>(emptyMap())
    private val cacheSlots get() = cacheSlotsFlow.value

    private val totalCount: Int get() = cacheSlots.values.sumOf { it.count }
    private var scope: CoroutineScope? = null

    private val whenActiveBlocks = mutableListOf<suspend ScopedLazyCache<Arg, T>.() -> Unit>()

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

    override fun spyOnArgs(): StateFlow<Set<Arg>> {
        return cacheSlotsFlow.stateMap { it.keys }
    }

    override fun reload(
        arg: Arg,
        config: LoadConfig?,
        metadata: ContainerMetadata,
    ): Flow<T> {
        return getSubject(arg)?.reload(config, metadata) ?: emptyFlow()
    }

    override fun updateWith(arg: Arg, container: Container<T>) {
        getSubject(arg)?.updateWith(container)
    }

    override fun reset() = synchronized(this) {
        val iterator = cacheSlots.iterator()
        val argsToRemove = mutableSetOf<Arg>()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.count == 0) {
                argsToRemove += entry.key
            }
        }
        if (argsToRemove.isNotEmpty()) {
            cacheSlotsFlow.update { oldMap -> oldMap - argsToRemove }
        }
        if (totalCount == 0) {
            scope?.cancel()
            scope = null
        }
    }

    override fun whenActive(
        block: suspend ScopedLazyCache<Arg, T>.() -> Unit,
    ): LazyCache<Arg, T> = synchronized(this) {
        whenActiveBlocks.add(block)
        this
    }

    private fun registerRecord(arg: Arg): CacheRecord<T> = synchronized(this) {
        val existingRecord = cacheSlots[arg]
        val record = if (existingRecord == null) {
            val subject = factory.create(arg, coroutineScopeFactory, cacheTimeoutMillis)
            CacheRecord(subject).also {
                cacheSlotsFlow.update { oldMap ->
                    oldMap + (arg to it)
                }
            }
        } else {
            existingRecord
        }
        record.count++
        if (totalCount == 1 && scope == null) {
            scope = coroutineScopeFactory
                .createScope()
                .also { scope ->
                    whenActiveBlocks.forEach { block ->
                        scope.launch {
                            supervisorScope {
                                val scopedCache = ScopedLazyCacheImpl(
                                    lazyCache = this@LazyCacheImpl,
                                    coroutineScope = this,
                                )
                                block.invoke(scopedCache)
                            }
                        }
                    }
                }
        }
        return record
    }

    private fun unregisterRecord(arg: Arg) = synchronized(this) {
        val record = cacheSlots[arg] ?: return@synchronized
        record.count--
        if (record.count == 0) {
            scheduleRemoval(arg)
        }
    }

    private fun scheduleRemoval(arg: Arg) {
        scope?.launch {
            delay(cacheTimeoutMillis)
            synchronized(this@LazyCacheImpl) {
                val record = cacheSlots[arg]
                if (record?.count == 0) {
                    cacheSlotsFlow.update { oldMap -> oldMap - arg }
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

}
