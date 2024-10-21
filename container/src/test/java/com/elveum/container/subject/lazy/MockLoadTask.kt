@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.LoadTrigger
import com.elveum.container.subject.ValueLoader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent

internal class MockLoadTask(scope: TestScope) : LoadTask<String> {

    private val _controller = LoadTaskControllerImpl(scope)
    val controller: LoadTaskController = _controller

    override var lastRealLoader: ValueLoader<String>? = null

    override fun execute(): Flow<Container<String>> {
        return channelFlow {
            _controller.awaitStart()
            val job = launch {
                _controller.flow().collect(::send)
            }
            _controller.awaitComplete()
            job.cancel()
        }
    }

    override fun setLoadTrigger(loadTrigger: LoadTrigger) = Unit
    override fun cancel() = Unit
}

internal interface LoadTaskController {
    fun start()
    suspend fun emit(container: Container<String>)
    fun complete()
    fun reset()
}

private class LoadTaskControllerImpl(
    private val scope: TestScope
) : LoadTaskController {

    private var startContinuation = CompletableDeferred<Unit>()
    private var endContinuation = CompletableDeferred<Unit>()
    private val flow = MutableSharedFlow<Container<String>>()

    override fun start() {
        startContinuation.complete(Unit)
        scope.runCurrent()
    }

    override suspend fun emit(container: Container<String>) {
        flow.emit(container)
        scope.runCurrent()
    }

    fun flow() = flow

    override fun complete() {
        endContinuation.complete(Unit)
        scope.runCurrent()
    }

    suspend fun awaitStart() {
        startContinuation.await()
    }

    suspend fun awaitComplete() {
        endContinuation.await()
    }

    override fun reset() {
        startContinuation = CompletableDeferred()
        endContinuation = CompletableDeferred()
    }

}