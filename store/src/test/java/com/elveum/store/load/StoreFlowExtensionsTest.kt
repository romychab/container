package com.elveum.store.load

import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreFlowExtensionsTest {

    private val exception = IllegalStateException("boom")

    @Test
    fun `GIVEN loaded result WHEN storeMap THEN value is transformed`() = runFlowTest {
        val flow = flowOf<StoreResult<String>>(StoreResult.Loaded("value"))

        val collector = flow.storeMap { "$it!" }.startCollecting()
        runCurrent()

        assertEquals(StoreResult.Loaded("value!"), collector.lastItem)
    }

    @Test
    fun `GIVEN failed result WHEN storeMap THEN failure is preserved`() = runFlowTest {
        val flow = flowOf<StoreResult<String>>(StoreResult.Failed(exception))

        val collector = flow.storeMap { "$it!" }.startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isFailed())
    }

    @Test
    fun `GIVEN loading result WHEN storeMap THEN loading is preserved`() = runFlowTest {
        val flow = flowOf<StoreResult<String>>(StoreResult.Loading)

        val collector = flow.storeMap { "$it!" }.startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isForegroundLoading())
    }

    @Test
    fun `GIVEN loaded result WHEN storeFlatMapResultLatest THEN inner result is observed`() = runFlowTest {
        val flow = flowOf<StoreResult<String>>(StoreResult.Loaded("value"))

        val collector = flow
            .storeFlatMapResultLatest { flowOf<StoreResult<String>>(StoreResult.Loaded("inner-$it")) }
            .startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isLoaded())
        assertEquals("inner-value", collector.lastItem.getOrNull())
    }

    @Test
    fun `GIVEN failed result WHEN storeFlatMapResultLatest THEN mapper is not invoked and failure is kept`() = runFlowTest {
        val flow = flowOf<StoreResult<String>>(StoreResult.Failed(exception))
        var mapperCalled = false

        val collector = flow
            .storeFlatMapResultLatest {
                mapperCalled = true
                flowOf<StoreResult<String>>(StoreResult.Loaded("inner"))
            }
            .startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isFailed())
        assertTrue(!mapperCalled)
    }

    @Test
    fun `GIVEN loading result WHEN storeFlatMapResultLatest THEN loading is kept`() = runFlowTest {
        val flow = flowOf<StoreResult<String>>(StoreResult.Loading)

        val collector = flow
            .storeFlatMapResultLatest { flowOf<StoreResult<String>>(StoreResult.Loaded("inner")) }
            .startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isForegroundLoading())
    }

    @Test
    fun `GIVEN loaded result WHEN inner flow emits failure THEN failure is observed`() = runFlowTest {
        val flow = flowOf<StoreResult<String>>(StoreResult.Loaded("value"))

        val collector = flow
            .storeFlatMapResultLatest { flowOf<StoreResult<String>>(StoreResult.Failed(exception)) }
            .startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isFailed())
    }

    @Test
    fun `GIVEN loaded result WHEN mapper throws THEN failure is observed`() = runFlowTest {
        val flow = flowOf<StoreResult<String>>(StoreResult.Loaded("value"))

        val collector = flow
            .storeFlatMapResultLatest<String, String> { throw exception }
            .startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isFailed())
    }

    @Test
    fun `GIVEN loaded result WHEN storeFlatMapLatest THEN plain values are wrapped into loaded results`() = runFlowTest {
        val flow = flowOf<StoreResult<String>>(StoreResult.Loaded("value"))

        val collector = flow
            .storeFlatMapLatest { flowOf("plain-$it") }
            .startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isLoaded())
        assertEquals("plain-value", collector.lastItem.getOrNull())
    }

    @Test
    fun `GIVEN list of items WHEN storeListFlatMapLatest THEN each item is merged with its details`() = runFlowTest {
        val flow = flowOf<StoreResult<List<String>>>(StoreResult.Loaded(listOf("a", "b")))

        val collector = flow
            .storeListFlatMapLatest(
                observer = { item -> flowOf<StoreResult<String>>(StoreResult.Loaded(item.uppercase())) },
                mapper = { item, result -> "$item:${result.getOrNull()}" },
            )
            .startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isLoaded())
        assertEquals(listOf("a:A", "b:B"), collector.lastItem.getOrNull())
    }

    @Test
    fun `GIVEN empty list WHEN storeListFlatMapLatest THEN empty loaded list is observed`() = runFlowTest {
        val flow = flowOf<StoreResult<List<String>>>(StoreResult.Loaded(emptyList()))

        val collector = flow
            .storeListFlatMapLatest(
                observer = { item -> flowOf<StoreResult<String>>(StoreResult.Loaded(item)) },
                mapper = { _, result -> result.getOrNull() },
            )
            .startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isLoaded())
        assertEquals(emptyList<String>(), collector.lastItem.getOrNull())
    }

    @Test
    fun `GIVEN loading then loaded WHEN firstGetOrThrow THEN loading is skipped and value returned`() = runFlowTest {
        val flow = flowOf<StoreResult<String>>(StoreResult.Loading, StoreResult.Loaded("value"))

        val result = flow.firstGetOrThrow()

        assertEquals("value", result)
    }

    @Test
    fun `GIVEN failed result WHEN firstGetOrThrow THEN the exception is thrown`() = runFlowTest {
        val flow = flowOf<StoreResult<String>>(StoreResult.Loading, StoreResult.Failed(exception))

        val thrown = runCatching { flow.firstGetOrThrow() }.exceptionOrNull()

        assertSame(exception, thrown)
    }

    @Test
    fun `GIVEN mixed results WHEN filterLoaded THEN only loaded values are emitted`() = runFlowTest {
        val flow = flowOf(
            StoreResult.Loading,
            StoreResult.Loaded("a"),
            StoreResult.Failed(exception),
            StoreResult.Loaded("b"),
        )

        val collector = flow.filterLoaded().startCollecting()
        runCurrent()

        assertEquals(listOf("a", "b"), collector.collectedItems)
    }

    @Test
    fun `GIVEN no loaded results WHEN filterLoaded THEN nothing is emitted`() = runFlowTest {
        val flow = flowOf<StoreResult<String>>(StoreResult.Loading, StoreResult.Failed(exception))

        val collector = flow.filterLoaded().startCollecting()
        runCurrent()

        assertEquals(0, collector.count)
    }

}
