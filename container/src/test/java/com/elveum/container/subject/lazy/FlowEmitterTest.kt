@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.LoadTrigger
import com.elveum.container.LocalSourceIndicator
import com.elveum.container.subject.FlowSubject
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FlowEmitterTest {

    @MockK
    private lateinit var flowCollector: FlowCollector<Container<String>>

    @MockK
    private lateinit var flowSubject: FlowSubject<String>

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @Test
    fun hasEmittedItems_beforeEmittingItems_returnsFalse() {
        val flowEmitter = makeFlowEmitter()
        assertFalse(flowEmitter.hasEmittedItems)
    }

    @Test
    fun hasEmittedItems_afterEmittingItem_returnsTrue() = runTest {
        val flowEmitter = makeFlowEmitter()
        flowEmitter.emit("item")
        assertTrue(flowEmitter.hasEmittedItems)
    }

    @Test
    fun emit_sendsItem_toCollectorAndSubject() = runTest {
        val flowEmitter = makeFlowEmitter()

        flowEmitter.emit("item", LocalSourceIndicator)

        coVerify(exactly = 1) {
            flowSubject.onNext("item")
            flowCollector.emit(Container.Success("item", LocalSourceIndicator))
        }
    }

    private fun makeFlowEmitter() = FlowEmitter(
        loadTrigger = LoadTrigger.NewLoad,
        flowCollector = flowCollector,
        flowSubject = flowSubject,
    )
}