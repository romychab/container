@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.lazy

import com.elveum.container.Container
import com.elveum.container.LoadTrigger
import com.elveum.container.LocalSourceType
import com.elveum.container.subject.FlowSubject
import com.elveum.container.successContainer
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
        assertFalse(flowEmitter.hasEmittedValues)
    }

    @Test
    fun hasEmittedItems_afterEmittingItem_returnsTrue() = runTest {
        val flowEmitter = makeFlowEmitter()
        flowEmitter.emit("item")
        assertTrue(flowEmitter.hasEmittedValues)
    }

    @Test
    fun emit_sendsItem_toCollectorAndSubject() = runTest {
        val flowEmitter = makeFlowEmitter()

        flowEmitter.emit("item", LocalSourceType)

        coVerify(exactly = 1) {
            flowSubject.onNext("item")
            flowCollector.emit(successContainer("item", LocalSourceType, true))
        }
    }

    @Test
    fun emitLastItem_sendsLastItem() = runTest {
        val flowEmitter = makeFlowEmitter()

        flowEmitter.emit("item", LocalSourceType)
        flowEmitter.emitLastItem()

        coVerify(exactly = 1) {
            flowSubject.onNext("item")
            flowCollector.emit(successContainer("item", LocalSourceType, true))
            flowCollector.emit(successContainer("item", LocalSourceType))
        }
    }

    private fun makeFlowEmitter() = FlowEmitter(
        loadTrigger = LoadTrigger.NewLoad,
        flowCollector = flowCollector,
        flowSubject = flowSubject,
    )
}
