@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.state

import com.elveum.container.Emitter
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ValueLoaderWithLocalCacheStateTest {

    @Test
    fun firstLoad_usesInitialValueAsState() = runTest {
        val loader1 = loaderWithLocalCacheState(useLocalCacheInitialValue = true) {
            emit(useLocalCache)
        }
        val emitter1 = mockk<Emitter<Boolean>>(relaxed = true)
        val loader2 = loaderWithLocalCacheState(useLocalCacheInitialValue = false) {
            emit(useLocalCache)
        }
        val emitter2 = mockk<Emitter<Boolean>>(relaxed = true)

        loader1.invoke(emitter1)
        coVerify(exactly = 1) {
            emitter1.emit(true)
        }

        loader2.invoke(emitter2)
        coVerify(exactly = 1) {
            emitter2.emit(false)
        }
    }

    @Test
    fun nextLoads_usesTrueValueAsState() = runTest {
        val loader1 = loaderWithLocalCacheState(useLocalCacheInitialValue = true) {
            emit(useLocalCache)
        }
        val emitter1 = mockk<Emitter<Boolean>>(relaxed = true)
        val loader2 = loaderWithLocalCacheState(useLocalCacheInitialValue = false) {
            emit(useLocalCache)
        }
        val emitter2 = mockk<Emitter<Boolean>>(relaxed = true)

        repeat(3) { loader1.invoke(emitter1) }
        coVerifySequence {
            emitter1.emit(true)
            emitter1.emit(true)
            emitter1.emit(true)
        }

        repeat(3) { loader2.invoke(emitter2) }
        coVerifySequence {
            emitter2.emit(false)
            emitter2.emit(true)
            emitter2.emit(true)
        }

    }

}