package com.elveum.store.load

import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreResultCombinesTest {

    private val exception = IllegalStateException("boom")

    @Test
    fun `GIVEN two loaded flows WHEN combineStores THEN success values are combined`() = runFlowTest {
        val flow1 = flowOf<StoreResult<String>>(StoreResult.Loaded("a"))
        val flow2 = flowOf<StoreResult<Int>>(StoreResult.Loaded(1))

        val collector = combineStores(flow1, flow2) { a, b -> "$a-$b" }.startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isLoaded())
        assertEquals("a-1", collector.lastItem.getOrNull())
    }

    @Test
    fun `GIVEN one failed flow WHEN combineStores THEN result is failed`() = runFlowTest {
        val flow1 = flowOf<StoreResult<String>>(StoreResult.Loaded("a"))
        val flow2 = flowOf<StoreResult<Int>>(StoreResult.Failed(exception))

        val collector = combineStores(flow1, flow2) { a, b -> "$a-$b" }.startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isFailed())
    }

    @Test
    fun `GIVEN one loading flow WHEN combineStores THEN result is loading`() = runFlowTest {
        val flow1 = flowOf<StoreResult<String>>(StoreResult.Loaded("a"))
        val flow2 = flowOf<StoreResult<Int>>(StoreResult.Loading)

        val collector = combineStores(flow1, flow2) { a, b -> "$a-$b" }.startCollecting()
        runCurrent()

        assertTrue(collector.lastItem.isForegroundLoading())
    }

    @Test
    fun `GIVEN three loaded flows WHEN combineStores THEN all values are combined`() = runFlowTest {
        val flow1 = flowOf<StoreResult<String>>(StoreResult.Loaded("a"))
        val flow2 = flowOf<StoreResult<String>>(StoreResult.Loaded("b"))
        val flow3 = flowOf<StoreResult<String>>(StoreResult.Loaded("c"))

        val collector = combineStores(flow1, flow2, flow3) { a, b, c -> "$a$b$c" }.startCollecting()
        runCurrent()

        assertEquals("abc", collector.lastItem.getOrNull())
    }

    @Test
    fun `GIVEN list of loaded flows WHEN combineStores THEN values are combined in order`() = runFlowTest {
        val flows = listOf<Flow<StoreResult<*>>>(
            flowOf(StoreResult.Loaded("a")),
            flowOf(StoreResult.Loaded("b")),
        )

        val collector = combineStores(flows) { values -> values.joinToString("") }.startCollecting()
        runCurrent()

        assertEquals("ab", collector.lastItem.getOrNull())
    }
}
