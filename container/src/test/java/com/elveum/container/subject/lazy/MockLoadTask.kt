@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.EmptyMetadata
import com.elveum.container.LoadTrigger
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.lazy.LoadTask.ExecuteParams
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent

internal class MockLoadTask private constructor(
    private val scope: TestScope,
    override val metadata: ContainerMetadata,
    override val initialContainer: Container<String>?,
    controller: LoadTaskControllerImpl,
) : LoadTask<String> {

    private val _controller = controller
    val controller: LoadTaskController = _controller

    override var lastRealLoader: ValueLoader<String>? = null
    override val lastRealMetadata: ContainerMetadata = EmptyMetadata

    constructor(
        scope: TestScope,
        metadata: ContainerMetadata = EmptyMetadata,
        initialContainer: Container<String>? = null,
    ) : this(scope, metadata, initialContainer, LoadTaskControllerImpl(scope))

    override fun execute(executeParams: ExecuteParams<String>): Flow<Container<String>> {
        return channelFlow {
            _controller.executeParams = executeParams
            _controller.awaitStart()
            val job = launch {
                _controller.flow().collect(::send)
            }
            _controller.awaitComplete()
            job.cancel()
        }
    }

    override fun restoreLoadTask(metadata: ContainerMetadata): LoadTask<String> {
        return MockLoadTask(scope, metadata, initialContainer, _controller)
    }

    override fun cancel() = Unit
}

internal interface LoadTaskController {
    val executeParams: ExecuteParams<String>?
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

    override var executeParams: ExecuteParams<String>? = null

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