@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.state

import com.elveum.container.Emitter
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ValueLoaderWithStateTest {

    @Test
    fun state_isAccessibleFromExecutionBlock() = runTest {
        val initialValue = 1
        val emitter = mockk<Emitter<Int>>(relaxed = true)
        val loader = loaderWithState(initialValue) {
            emit(state)
        }

        loader.invoke(emitter)

        coVerify(exactly = 1) {
            emitter.emit(initialValue)
        }
    }

    @Test
    fun state_canBeChangedInExecutionBlock() = runTest {
        val initialValue = 1
        val emitter = mockk<Emitter<Int>>(relaxed = true)
        val loader = loaderWithState(initialValue) {
            updateState(state + 1)
            emit(state)
            updateState(state + 1)
            emit(state)
        }

        loader.invoke(emitter)

        coVerifyOrder {
            emitter.emit(2)
            emitter.emit(3)
        }
    }

    @Test
    fun state_isRetainedBetweenExecutions() = runTest {
        val initialValue = 1
        val emitter1 = mockk<Emitter<Int>>(relaxed = true)
        val emitter2 = mockk<Emitter<Int>>(relaxed = true)
        val loader = loaderWithState(initialValue) {
            updateState(state + 1)
            emit(state)
        }

        loader.invoke(emitter1)
        loader.invoke(emitter2)

        coVerify(exactly = 1) {
            emitter1.emit(2)
            emitter2.emit(3)
        }
    }

}