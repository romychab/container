package com.elveum.store.internal

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Like `flatMapLatest`, but make-before-break: the flow for a new upstream value is
 * subscribed to before the flow for the previous value is cancelled.
 *
 * `flatMapLatest` cancels and joins the previous inner flow before collecting the next
 * one, so for a moment nothing is subscribed. Resources tied to an active subscription
 * (e.g. a cache record with a zero timeout) can be released in that gap.
 *
 * Values of a replaced inner flow are never emitted once a newer inner flow has started,
 * including values already buffered: each value is checked when it is consumed.
 */
internal fun <T, R> Flow<T>.flatMapLatestMakeBeforeBreak(
    transform: (T) -> Flow<R>,
): Flow<R> = flow {
    val latestGeneration = AtomicLong(0)
    val taggedFlow = channelFlow {
        var previousJob: Job? = null
        this@flatMapLatestMakeBeforeBreak.collect { value ->
            val generation = latestGeneration.incrementAndGet()
            // UNDISPATCHED: the new inner flow subscribes right here, before the old one is cancelled
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                transform(value).collect { item -> send(Tagged(generation, item)) }
            }
            previousJob?.cancel()
            previousJob = job
        }
    }
    taggedFlow.collect { tagged ->
        if (tagged.generation == latestGeneration.get()) emit(tagged.item)
    }
}

private class Tagged<R>(
    val generation: Long,
    val item: R,
)
