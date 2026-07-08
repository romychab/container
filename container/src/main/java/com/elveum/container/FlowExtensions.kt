package com.elveum.container

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Throttles emissions so that consecutive downstream values are never closer
 * than [millis] apart, while keeping isolated updates responsive.
 *
 * Behavior:
 * - The first value (and any value that arrives after at least [millis] of
 *   silence since the previous emission) is emitted **immediately**.
 * - While values arrive more frequently than [millis], they are coalesced:
 *   only the **latest** value is emitted, once per [millis] window.
 *
 * This gives two benefits at once:
 * 1. bursts of frequent updates are optimized down to one emission per window;
 * 2. rare, standalone updates are delivered without delay.
 *
 * Note: for a finite source, completion may be postponed by up to [millis]
 * after the last value; this does not affect which values are emitted.
 *
 * @param T the type of values emitted by the flow.
 * @param millis minimum period, in milliseconds, between two downstream emissions.
 */
public fun <T> Flow<T>.throttleLatest(
    millis: Long,
): Flow<T> {
    val originFlow = this
    return channelFlow {
        val channel = Channel<T>(capacity = Channel.CONFLATED)
        launch {
            originFlow.collect { channel.send(it) }
            channel.close()
        }
        while (true) {
            channel.receiveCatching()
                .onClosed { break }
                .onSuccess {
                    send(it)
                    delay(millis)
                }
        }
    }
}
