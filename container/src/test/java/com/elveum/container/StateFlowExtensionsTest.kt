package com.elveum.container

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class StateFlowExtensionsTest {

    @Test
    fun public_castsTypeToStateFlow() {
        val mutableStateFlow = MutableStateFlow("test")
        val stateFlow = mutableStateFlow.public()
        assertSame(mutableStateFlow, stateFlow)
    }

    @Test
    fun tryUpdate_withMutableStateFlow_updatesValue() {
        val flow: StateFlow<Int> = MutableStateFlow(value = 1)
        flow.tryUpdate(2)
        assertEquals(2, flow.value)
    }

    @Test
    fun tryUpdate_withNonMutableStateFlow_doesNothing() {
        val flow: StateFlow<Int> = MutableStateFlow(value = 1).asStateFlow()
        flow.tryUpdate(2)
        assertEquals(1, flow.value)
    }

    @Test
    fun tryUpdate_withMutableStateFlow_callsUpdater() {
        val flow: StateFlow<Int> = MutableStateFlow(value = 1)
        val updater = mockk<(Int) -> Int>()
        every { updater(1) } returns 2

        flow.tryUpdate(updater)

        assertEquals(2, flow.value)
        verify(exactly = 1) {
            updater(1)
        }
    }

    @Test
    fun tryUpdate_withNonMutableStateFlow_doesNotCallUpdater() {
        val flow: StateFlow<Int> = MutableStateFlow(value = 1).asStateFlow()
        val updater = mockk<(Int) -> Int>()

        flow.tryUpdate(updater)

        assertEquals(1, flow.value)
        verify(exactly = 1) {
            updater wasNot called
        }
    }

}