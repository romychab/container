package com.elveum.container.cache

import com.elveum.container.Container
import com.elveum.container.Emitter
import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.container.successContainer
import com.uandcode.flowtest.CollectStatus
import com.uandcode.flowtest.runFlowTest
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LazyCacheIntegrationTest {

    private val timeoutMillis: Long = 1000L
    private val loadDelay = 4000L

    private val key = "key"
    private val key1 = "key1"
    private val key2 = "key2"

    private lateinit var loader: CacheValueLoader<String, String>

    private lateinit var lazyCache: LazyCache<String, String>

    private lateinit var scope: TestScope

    private lateinit var coroutineScopeFactory: CoroutineScopeFactory

    @Before
    fun setUp() {
        loader = mockk()
        scope = TestScope()
        coroutineScopeFactory = CoroutineScopeFactory {
            TestScope(scope.testScheduler)
        }
        coEvery { loader.invoke(any(), any()) } coAnswers {
            delay(loadDelay)
            firstArg<Emitter<String>>().emit(expectedLoadedValue(secondArg()))
        }
        lazyCache = LazyCacheImpl(
            cacheTimeoutMillis = timeoutMillis,
            coroutineScopeFactory = coroutineScopeFactory,
            valueLoader = this.loader,
        )
    }

    @Test
    fun listen_withoutCollecting_doesNothing() {
        lazyCache.listen(key)

        coVerify(exactly = 0) {
            loader(any(), any())
        }
        assertFalse(lazyCache.hasActiveCollectors(key))
        assertEquals(0, lazyCache.getActiveCollectorsCount(key))
    }

    @Test
    fun listen_afterStartOfCollecting_startsLoadingValue() = scope.runFlowTest {
        val state = lazyCache.listen(key).startCollecting()
        runCurrent()

        coVerify(exactly = 1) {
            loader(any(), key)
        }
        assertEquals(CollectStatus.Collecting, state.collectStatus)
        assertEquals(1, state.count)
        assertEquals(Container.Pending, state.lastItem)
    }

    @Test
    fun listen_collectsLoadedValues() = scope.runFlowTest {
        val expectedContainer = successContainer(expectedLoadedValue(key))

        val state = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        assertEquals(CollectStatus.Collecting, state.collectStatus)
        assertEquals(2, state.count)
        assertEquals(expectedContainer, state.lastItem)
    }

    @Test
    fun listen_withSameArg_doesNotTriggerLoaderTwice() = scope.runFlowTest {
        val expectedContainer = successContainer(expectedLoadedValue(key))

        val state1 = lazyCache.listen(key).startCollecting()
        advanceTimeBy(1)
        val state2 = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay)

        coVerify(exactly = 1) {
            loader(any(), key)
        }
        assertEquals(CollectStatus.Collecting, state1.collectStatus)
        assertEquals(CollectStatus.Collecting, state2.collectStatus)
        assertEquals(expectedContainer, state1.lastItem)
        assertEquals(expectedContainer, state2.lastItem)
    }

    @Test
    fun listen_withMultipleArgs_startsMultipleLoads() = scope.runFlowTest {
        val expectedContainer1 = successContainer(expectedLoadedValue(key1))
        val expectedContainer2 = successContainer(expectedLoadedValue(key2))

        val state1 = lazyCache.listen(key1).startCollecting()
        val state2 = lazyCache.listen(key2).startCollecting()
        advanceTimeBy(loadDelay + 1)

        coVerify(exactly = 1) {
            loader(any(), key1)
        }
        coVerify(exactly = 1) {
            loader(any(), key2)
        }
        assertEquals(CollectStatus.Collecting, state1.collectStatus)
        assertEquals(CollectStatus.Collecting, state2.collectStatus)
        assertEquals(expectedContainer1, state1.lastItem)
        assertEquals(expectedContainer2, state2.lastItem)
    }

    @Test
    fun listen_afterStopOfCollecting_cancelsCurrentLoad() = scope.runFlowTest {
        val state = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay) // almost loaded

        state.cancel() // unsubscribing should cancel the load
        advanceTimeBy(1) // try to finish the load, but is should be already cancelled

        // results should not be delivered as the load has been cancelled
        assertEquals(Container.Pending, state.lastItem)
    }

    @Test
    fun listen_withinTimeout_holdsCachedValue() = scope.runFlowTest {
        val expectedContainer = successContainer(expectedLoadedValue(key))
        val state1 = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        state1.cancel()

        advanceTimeBy(timeoutMillis) // almost expired
        val state2 = lazyCache.listen(key).startCollecting()
        advanceTimeBy(1)
        assertEquals(expectedContainer, state2.lastItem)
        coVerify(exactly = 1) {
            loader(any(), key)
        }
    }

    @Test
    fun listen_afterTimeout_startsNewLoad() = scope.runFlowTest {
        val expectedContainer = successContainer(expectedLoadedValue(key))
        val state1 = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        state1.cancel()

        advanceTimeBy(timeoutMillis + 1) // expired
        val state2 = lazyCache.listen(key).startCollecting()
        advanceTimeBy(1)
        assertEquals(Container.Pending, state2.lastItem)
        advanceTimeBy(loadDelay)
        assertEquals(expectedContainer, state2.lastItem)
        coVerify(exactly = 2) {
            loader(any(), key)
        }
    }

    @Test
    fun listen_countsTimeoutSeparatelyForEachArg() = scope.runFlowTest {
        val expectedContainer2 = successContainer(expectedLoadedValue(key2))
        val state1 = lazyCache.listen(key1).startCollecting()
        val state2 = lazyCache.listen(key2).startCollecting()
        advanceTimeBy(loadDelay + 1) // force load all values

        state1.cancel()
        advanceTimeBy(timeoutMillis) // almost expired cache of the first key
        state2.cancel()

        advanceTimeBy(1) // now cache is expired for the first key, but the second isn't
        val collectedItems1 = lazyCache.listen(key1).startCollecting().collectedItems
        val collectedItems2 = lazyCache.listen(key2).startCollecting().collectedItems
        assertEquals(Container.Pending, collectedItems1.last())
        assertEquals(expectedContainer2, collectedItems2.last())
    }

    @Test
    fun listen_withFailedLoad_emitsErrorStatusAndContinuesListening() = scope.runFlowTest {
        val expectedException = IllegalStateException("Oops")
        coEvery { loader.invoke(any(), any()) } coAnswers {
            delay(loadDelay)
            throw expectedException
        }

        val state = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        val errorContainer = state.lastItem as Container.Error
        assertSame(expectedException, errorContainer.exception)
        assertEquals(CollectStatus.Collecting, state.collectStatus)
    }

    @Test
    fun get_withoutActiveCollectors_returnsPending() {
        assertEquals(Container.Pending, lazyCache.get(key))
    }

    @Test
    fun get_withActiveCollector_returnsLatestValue() = scope.runFlowTest {
        val expectedContainer = successContainer(expectedLoadedValue(key))
        lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        val result = lazyCache.get(key)

        assertEquals(expectedContainer, result)
    }

    @Test
    fun get_afterCacheExpiration_returnsPendingStatus() = scope.runFlowTest {
        val expectedContainer = successContainer(expectedLoadedValue(key))
        val state = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        state.cancel()
        advanceTimeBy(timeoutMillis) // almost expired
        val result1 = lazyCache.get(key)
        advanceTimeBy(1) // now expired
        val result2 = lazyCache.get(key)

        assertEquals(expectedContainer, result1)
        assertEquals(Container.Pending, result2)
    }

    @Test
    fun get_withMultipleArgs_returnsDifferentResults() = scope.runFlowTest {
        val expectedContainer1 = successContainer(expectedLoadedValue(key1))
        val expectedContainer2 = successContainer(expectedLoadedValue(key2))
        lazyCache.listen(key1).startCollecting()
        lazyCache.listen(key2).startCollecting()
        advanceTimeBy(loadDelay + 1)

        val result1 = lazyCache.get(key1)
        val result2 = lazyCache.get(key2)

        assertEquals(expectedContainer1, result1)
        assertEquals(expectedContainer2, result2)
    }

    @Test
    fun getActiveCollectorsCount_returnsOnlyActualCollectorsCount() = scope.runFlowTest {
        val flow = lazyCache.listen(key)
        assertEquals(0, lazyCache.getActiveCollectorsCount(key))

        val state1 = flow.startCollecting()
        assertEquals(1, lazyCache.getActiveCollectorsCount(key))

        val state2 = flow.startCollecting()
        assertEquals(2, lazyCache.getActiveCollectorsCount(key))

        state1.cancel()
        assertEquals(1, lazyCache.getActiveCollectorsCount(key))

        state2.cancel()
        assertEquals(0, lazyCache.getActiveCollectorsCount(key))
    }

    @Test
    fun getActiveCollectorsCount_countsSeparatelyForEachArg() = scope.runFlowTest {
        val flow1 = lazyCache.listen(key1)
        val flow2 = lazyCache.listen(key2)
        assertEquals(0, lazyCache.getActiveCollectorsCount(key1))
        assertEquals(0, lazyCache.getActiveCollectorsCount(key2))

        val state1 = flow1.startCollecting()
        assertEquals(1, lazyCache.getActiveCollectorsCount(key1))
        assertEquals(0, lazyCache.getActiveCollectorsCount(key2))

        val state2 = flow2.startCollecting()
        assertEquals(1, lazyCache.getActiveCollectorsCount(key1))
        assertEquals(1, lazyCache.getActiveCollectorsCount(key2))

        state2.cancel()
        assertEquals(1, lazyCache.getActiveCollectorsCount(key1))
        assertEquals(0, lazyCache.getActiveCollectorsCount(key2))

        state1.cancel()
        assertEquals(0, lazyCache.getActiveCollectorsCount(key1))
        assertEquals(0, lazyCache.getActiveCollectorsCount(key2))
    }

    @Test
    fun hasActiveCollectors_withZeroCollectors_returnsFalse() {
        assertFalse(lazyCache.hasActiveCollectors(key))
    }

    @Test
    fun hasActiveCollectors_withAtLeastOneCollector_returnsTrue() = scope.runFlowTest {
        val state = lazyCache.listen(key).startCollecting()
        assertTrue(lazyCache.hasActiveCollectors(key))

        state.cancel()
        assertFalse(lazyCache.hasActiveCollectors(key))
    }

    @Test
    fun reload_withoutSilentFlag_emitsPendingAndSuccessStatuses() = scope.runFlowTest {
        mockLoaderReturningDifferentResults()
        val state = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        lazyCache.reload(key, silently = false)

        advanceTimeBy(1)
        assertEquals(Container.Pending, state.lastItem)
        advanceTimeBy(loadDelay)
        assertEquals(successContainer(expectedLoadedValue(key, 2)), state.lastItem)
    }

    @Test
    fun reload_withSilentFlag_emitsOnlySuccessStatus() = scope.runFlowTest {
        mockLoaderReturningDifferentResults()
        val state = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        lazyCache.reload(key, silently = true)

        advanceTimeBy(1)
        assertEquals(successContainer(expectedLoadedValue(key, 1)), state.lastItem)
        advanceTimeBy(loadDelay)
        assertEquals(successContainer(expectedLoadedValue(key, 2)), state.lastItem)
    }

    @Test
    fun reload_withoutActiveCollectors_doesNothing() = scope.runFlowTest {
        lazyCache.reload(key, silently = false)
        advanceTimeBy(loadDelay + 1)

        verify {
            loader wasNot called
        }
    }

    @Test
    fun reload_withExpiredCollector_doesNothing() = scope.runFlowTest {
        mockLoaderReturningDifferentResults()
        val state = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        state.cancel()
        advanceTimeBy(timeoutMillis + 1)
        lazyCache.reload(key, silently = false)
        advanceTimeBy(loadDelay + 1)

        coVerify(exactly = 1) { // loader should not be executed twice
            loader(any(), any())
        }
    }

    @Test
    fun reload_returnsFlowIndicatingCurrentStatus() = scope.runFlowTest {
        mockLoaderReturningDifferentResults()
        lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        val state = lazyCache.reload(key, silently = false).startCollecting()

        advanceTimeBy(1)
        assertFalse(state.hasItems)

        advanceTimeBy(loadDelay)
        assertEquals(expectedLoadedValue(key, 2), state.lastItem)
        assertEquals(CollectStatus.Completed, state.collectStatus)
    }

    @Test
    fun reload_withFailedLoad_returnsFlowEmittingError() = scope.runFlowTest {
        val expectedException = IllegalStateException("Oops")
        var isError = false
        coEvery { loader.invoke(any(), any()) } coAnswers {
            delay(loadDelay)
            if (isError) {
                isError = true
                firstArg<Emitter<String>>().emit(expectedLoadedValue(secondArg()))
            } else {
                throw expectedException
            }
        }
        lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        val flow = lazyCache.reload(key)
        val state1 = flow.startCollecting()

        // initial state
        assertFalse(state1.hasItems)
        assertEquals(CollectStatus.Collecting, state1.collectStatus)
        // assert error
        advanceTimeBy(loadDelay + 1)
        val jobStatus1 = state1.collectStatus as CollectStatus.Failed
        val exception1 = jobStatus1.exception as IllegalStateException
        assertEquals(expectedException.message, exception1.message)
        // assert error if the same flow is collected again
        val state2 = flow.startCollecting()
        val jobStatus2 = state2.collectStatus as CollectStatus.Failed
        val exception2 = jobStatus2.exception as IllegalStateException
        assertEquals(expectedException.message, exception2.message)
    }

    @Test
    fun updateWith_withoutActiveCollector_doesNothing() = scope.runFlowTest {
        lazyCache.listen(key)

        lazyCache.updateWith(key, successContainer("123"))
        advanceTimeBy(loadDelay + 1)

        assertEquals(Container.Pending, lazyCache.get(key))
    }

    @Test
    fun updateWith_withActiveCollector_replacesLastValue() = scope.runFlowTest {
        val expectedContainer = successContainer("hello")
        val state = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        lazyCache.updateWith(key, expectedContainer)

        assertEquals(
            listOf(Container.Pending, successContainer(expectedLoadedValue(key)), expectedContainer),
            state.collectedItems,
        )
        assertEquals(expectedContainer, lazyCache.get(key))
    }

    @Test
    fun updateWith_withActiveCollectorAndLoader_cancelsLoaderAndReplacesValue() = scope.runFlowTest {
        val expectedContainer = successContainer("hello")
        val state = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay) // almost loaded

        lazyCache.updateWith(key, expectedContainer)
        advanceTimeBy(loadDelay * 2)

        assertEquals(
            listOf(Container.Pending, expectedContainer),
            state.collectedItems,
        )
        assertEquals(expectedContainer, lazyCache.get(key))
    }

    @Test
    fun updateWith_afterExpiredCache_doesNothing() = scope.runFlowTest {
        val state = lazyCache.listen(key).startCollecting()
        advanceTimeBy(loadDelay + 1)

        state.cancel()
        advanceTimeBy(timeoutMillis + 1)
        lazyCache.updateWith(key, successContainer("123"))

        assertEquals(Container.Pending, lazyCache.get(key))
    }

    private fun mockLoaderReturningDifferentResults() {
        var count = 0
        coEvery { loader.invoke(any(), any()) } coAnswers {
            delay(loadDelay)
            firstArg<Emitter<String>>().emit(expectedLoadedValue(secondArg(), ++count))
        }
    }

    private fun expectedLoadedValue(key: String, executionNumber: Int) = "loaded-$key-$executionNumber"

    private fun expectedLoadedValue(key: String) = "loaded-$key"
}