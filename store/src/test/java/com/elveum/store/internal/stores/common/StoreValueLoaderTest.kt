package com.elveum.store.internal.stores.common

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.Emitter
import com.elveum.container.EmptyMetadata
import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.container.SourceType
import com.elveum.container.sourceType
import com.elveum.store.exceptions.NoCachedDataException
import com.elveum.store.load.LoadRequestSource
import kotlinx.coroutines.flow.Flow
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

    private class FakeEmitter : Emitter<String> {
        val emissions = mutableListOf<Emission>()
        override val metadata: ContainerMetadata = EmptyMetadata

        override suspend fun emit(value: String, metadata: ContainerMetadata, isLastValue: Boolean) {
            emissions += Emission(value, metadata.sourceType, isLastValue)
        }

        override suspend fun <R> dependsOnFlow(key: Any, vararg keys: Any, flow: () -> Flow<R>): R =
            error("not used in this test")

        override suspend fun <R> dependsOnContainerFlow(key: Any, vararg keys: Any, flow: () -> Flow<Container<R>>): R =
            error("not used in this test")
    }

    @Test
    fun `GIVEN default source WHEN cache hit THEN local value is emitted then remote value`() = runTest {
        val emitter = FakeEmitter()

        emitter.processDataLoad(
            query = "q",
            fetcher = { "remote" },
            loader = { "cached" },
            saver = { _, _ -> },
            observer = { flowOf(null) },
            loadRequestSource = LoadRequestSource.Default,
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
        val emitter = FakeEmitter()
        var saved = false

        emitter.processDataLoad(
            query = "q",
            fetcher = { "same" },
            loader = { "same" },
            saver = { _, _ -> saved = true },
            observer = { flowOf(null) },
            loadRequestSource = LoadRequestSource.Default,
        )

        assertFalse(saved)
    }

    @Test
    fun `GIVEN default source WHEN remote differs from cache THEN saver is called with remote value`() = runTest {
        val emitter = FakeEmitter()
        val savedValues = mutableListOf<Pair<String, String>>()

        emitter.processDataLoad(
            query = "q",
            fetcher = { "remote" },
            loader = { "cached" },
            saver = { query, value -> savedValues += query to value },
            observer = { flowOf(null) },
            loadRequestSource = LoadRequestSource.Default,
        )

        assertEquals(listOf("q" to "remote"), savedValues)
    }

    @Test
    fun `GIVEN default source WHEN loader misses but observer has value THEN observed value is emitted as local`() = runTest {
        val emitter = FakeEmitter()

        emitter.processDataLoad(
            query = "q",
            fetcher = { "remote" },
            loader = { null },
            saver = { _, _ -> },
            observer = { flowOf("observed") },
            loadRequestSource = LoadRequestSource.Default,
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
        val emitter = FakeEmitter()
        var saved = false

        emitter.processDataLoad(
            query = "q",
            fetcher = { "remote" },
            loader = { null },
            saver = { _, _ -> saved = true },
            observer = { flowOf(null) },
            loadRequestSource = LoadRequestSource.Default,
        )

        assertEquals(
            listOf(Emission("remote", RemoteSourceType, isLastValue = true)),
            emitter.emissions,
        )
        assertTrue(saved)
    }

    @Test
    fun `GIVEN fresh source WHEN load THEN cache is ignored and only remote value is emitted`() = runTest {
        val emitter = FakeEmitter()
        var loaderCalled = false
        var observerCalled = false

        emitter.processDataLoad(
            query = "q",
            fetcher = { "remote" },
            loader = { loaderCalled = true; "cached" },
            saver = { _, _ -> },
            observer = { observerCalled = true; flowOf("observed") },
            loadRequestSource = LoadRequestSource.Fresh,
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
        val emitter = FakeEmitter()
        var fetcherCalled = false
        var saved = false

        emitter.processDataLoad(
            query = "q",
            fetcher = { fetcherCalled = true; "remote" },
            loader = { "cached" },
            saver = { _, _ -> saved = true },
            observer = { flowOf(null) },
            loadRequestSource = LoadRequestSource.Offline,
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
        val emitter = FakeEmitter()
        var thrown: Exception? = null

        try {
            emitter.processDataLoad(
                query = "q",
                fetcher = { "remote" },
                loader = { null },
                saver = { _, _ -> },
                observer = { flowOf(null) },
                loadRequestSource = LoadRequestSource.Offline,
            )
        } catch (e: Exception) {
            thrown = e
        }

        assertTrue(thrown is NoCachedDataException)
        assertTrue(emitter.emissions.isEmpty())
    }
}
