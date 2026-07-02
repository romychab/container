package com.elveum.store.internal.stores.common

import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.container.SourceType
import com.elveum.store.exceptions.NoCachedDataException
import com.elveum.store.load.LoadRequestSource
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreValueLoaderTest {

    private data class Emission(
        val value: String,
        val sourceType: SourceType,
        val isLastValue: Boolean,
    )

    private class FakeCoreEmitter : CoreEmitter<String> {
        val emissions = mutableListOf<Emission>()

        override suspend fun emit(value: String, sourceType: SourceType, isLastValue: Boolean) {
            emissions += Emission(value, sourceType, isLastValue)
        }
    }

    private val delegate = DefaultCoreLoaderDelegate<String>()

    @Test
    fun `GIVEN default source WHEN cache hit THEN local value is emitted then remote value`() = runTest {
        val emitter = FakeCoreEmitter()

        delegate.processDataLoad(
            emitter = emitter,
            requestSource = LoadRequestSource.Default,
            fetcher = { "remote" },
            loader = { "cached" },
            observer = { flowOf(null) },
            saver = { },
        )

        assertEquals(
            listOf(
                Emission("cached", LocalSourceType, isLastValue = false),
                Emission("remote", RemoteSourceType, isLastValue = true),
            ),
            emitter.emissions,
        )
    }

    @Test
    fun `GIVEN default source WHEN cache hit equals remote THEN saver is not called`() = runTest {
        val emitter = FakeCoreEmitter()
        var saved = false

        delegate.processDataLoad(
            emitter = emitter,
            requestSource = LoadRequestSource.Default,
            fetcher = { "same" },
            loader = { "same" },
            observer = { flowOf(null) },
            saver = { saved = true },
        )

        assertFalse(saved)
    }

    @Test
    fun `GIVEN default source WHEN remote differs from cache THEN saver is called with remote value`() = runTest {
        val emitter = FakeCoreEmitter()
        val savedValues = mutableListOf<String>()

        delegate.processDataLoad(
            emitter = emitter,
            requestSource = LoadRequestSource.Default,
            fetcher = { "remote" },
            loader = { "cached" },
            observer = { flowOf(null) },
            saver = { value -> savedValues += value },
        )

        assertEquals(listOf("remote"), savedValues)
    }

    @Test
    fun `GIVEN default source WHEN loader misses but observer has value THEN observed value is emitted as local`() = runTest {
        val emitter = FakeCoreEmitter()

        delegate.processDataLoad(
            emitter = emitter,
            requestSource = LoadRequestSource.Default,
            fetcher = { "remote" },
            loader = { null },
            observer = { flowOf("observed") },
            saver = { },
        )

        assertEquals(
            listOf(
                Emission("observed", LocalSourceType, isLastValue = false),
                Emission("remote", RemoteSourceType, isLastValue = true),
            ),
            emitter.emissions,
        )
    }

    @Test
    fun `GIVEN default source WHEN no cached value THEN only remote value is emitted and saved`() = runTest {
        val emitter = FakeCoreEmitter()
        var saved = false

        delegate.processDataLoad(
            emitter = emitter,
            requestSource = LoadRequestSource.Default,
            fetcher = { "remote" },
            loader = { null },
            observer = { flowOf(null) },
            saver = { saved = true },
        )

        assertEquals(
            listOf(Emission("remote", RemoteSourceType, isLastValue = true)),
            emitter.emissions,
        )
        assertTrue(saved)
    }

    @Test
    fun `GIVEN fresh source WHEN load THEN cache is ignored and only remote value is emitted`() = runTest {
        val emitter = FakeCoreEmitter()
        var loaderCalled = false
        var observerCalled = false

        delegate.processDataLoad(
            emitter = emitter,
            requestSource = LoadRequestSource.Fresh,
            fetcher = { "remote" },
            loader = { loaderCalled = true; "cached" },
            observer = { observerCalled = true; flowOf("observed") },
            saver = { },
        )

        assertEquals(
            listOf(Emission("remote", RemoteSourceType, isLastValue = true)),
            emitter.emissions,
        )
        assertFalse(loaderCalled)
        assertFalse(observerCalled)
    }

    @Test
    fun `GIVEN offline source WHEN cache hit THEN only cached value is emitted as last value and remote is not fetched`() = runTest {
        val emitter = FakeCoreEmitter()
        var fetcherCalled = false
        var saved = false

        delegate.processDataLoad(
            emitter = emitter,
            requestSource = LoadRequestSource.Offline,
            fetcher = { fetcherCalled = true; "remote" },
            loader = { "cached" },
            observer = { flowOf(null) },
            saver = { saved = true },
        )

        assertEquals(
            listOf(Emission("cached", LocalSourceType, isLastValue = true)),
            emitter.emissions,
        )
        assertFalse(fetcherCalled)
        assertFalse(saved)
    }

    @Test
    fun `GIVEN offline source WHEN no cached value THEN NoCachedDataException is thrown`() = runTest {
        val emitter = FakeCoreEmitter()
        var thrown: Exception? = null

        try {
            delegate.processDataLoad(
                emitter = emitter,
                requestSource = LoadRequestSource.Offline,
                fetcher = { "remote" },
                loader = { null },
                observer = { flowOf(null) },
                saver = { },
            )
        } catch (e: Exception) {
            thrown = e
        }

        assertTrue(thrown is NoCachedDataException)
        assertTrue(emitter.emissions.isEmpty())
    }
}
