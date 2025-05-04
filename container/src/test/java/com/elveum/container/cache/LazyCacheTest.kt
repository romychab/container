package com.elveum.container.cache

import com.elveum.container.Container
import com.elveum.container.Emitter
import com.elveum.container.cache.LazyCacheImpl.LazyFlowSubjectFactory
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.ValueLoader
import com.elveum.container.subject.factories.CoroutineScopeFactory
import com.elveum.container.utils.JobStatus
import com.elveum.container.utils.runFlowTest
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LazyCacheTest {

    private val timeoutMillis: Long = 1000L
    private val key = "key"
    private val key1 = "key1"
    private val key2 = "key2"

    @MockK
    private lateinit var subjectFactory: LazyFlowSubjectFactory

    private lateinit var loader: CacheValueLoader<String, String>

    @MockK
    private lateinit var coroutineScopeFactory: CoroutineScopeFactory

    private lateinit var scope: TestScope

    private lateinit var lazyCache: LazyCache<String, String>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        loader = mockk(relaxed = true)
        scope = TestScope()
        every { coroutineScopeFactory.createScope() } answers {
            TestScope(scope.testScheduler)
        }
        lazyCache = LazyCacheImpl(
            cacheTimeoutMillis = timeoutMillis,
            coroutineScopeFactory = coroutineScopeFactory,
            loader = loader,
            subjectFactory = subjectFactory,
        )
    }

    @Test
    fun listen_withoutCollecting_doesNothing() {
        lazyCache.listen(key)

        verify(exactly = 0) {
            subjectFactory.create<String>(any())
        }
    }

    @Test
    fun listen_afterStartOfCollecting_opensCacheSlot() = scope.runFlowTest {
        val expectedKey = key
        val expectedEmitter = mockk<Emitter<String>>()
        val subject = mockSubject()
        every { subject.listen() } returns MutableStateFlow(Container.Pending)

        lazyCache.listen(key).startCollecting()

        val valueLoader = slot<ValueLoader<String>>()
        verify(exactly = 1) {
            subjectFactory.create(capture(valueLoader))
        }
        valueLoader.captured.invoke(expectedEmitter)
        coVerify(exactly = 1) {
            loader(refEq(expectedEmitter), expectedKey)
        }
    }

    @Test
    fun listen_afterStartOfCollecting_listensFlowFromSubject() = scope.runFlowTest {
        val subject = mockSubject()
        val flow = MutableStateFlow<Container<String>>(Container.Pending)
        every { subject.listen() } returns flow

        val collectedItems = lazyCache.listen(key).startCollectingToList()

        val firstExpectedValue = Container.Pending
        flow.emit(firstExpectedValue)
        assertEquals(firstExpectedValue, collectedItems.last())
        val secondExpectedValue = Container.Error(IllegalStateException())
        flow.emit(secondExpectedValue)
        assertEquals(secondExpectedValue, collectedItems.last())
        val thirdExpectedValue = Container.Success("123")
        flow.emit(thirdExpectedValue)
        assertEquals(thirdExpectedValue, collectedItems.last())
    }

    @Test
    fun listen_afterStopOfCollecting_closesCacheSlot() = scope.runFlowTest {
        val subject = mockSubject()
        val subjectValue = Container.Success("111")
        every { subject.listen() } returns MutableStateFlow(Container.Pending)
        every { subject.currentValue } returns subjectValue

        lazyCache.listen(key).startCollecting().cancel()

        // 1 ms before timeout -> not cancelled yet
        advanceTimeBy(timeoutMillis)
        assertEquals(subjectValue, lazyCache.get(key))
        // timeout! -> should be cancelled
        advanceTimeBy(1)
        assertEquals(Container.Pending, lazyCache.get(key))
    }

    @Test
    fun listen_testMultipleArgs() = scope.runFlowTest {
        val flow1 = MutableStateFlow<Container<String>>(Container.Pending)
        val flow2 = MutableStateFlow<Container<String>>(Container.Pending)
        val (subject1, subject2) = mockTwoSubjects()
        every { subject1.listen() } returns flow1
        every { subject1.currentValue } returns Container.Success("1")
        every { subject2.listen() } returns flow2
        every { subject2.currentValue } returns Container.Success("2")

        val state1 = lazyCache.listen(key1).startCollecting()
        val state2 = lazyCache.listen(key2).startCollecting()
        val state1v2 = lazyCache.listen(key1).startCollecting()

        // emit item by subject1
        val subject1value1 = Container.Success("1_111")
        flow1.emit(subject1value1)
        assertEquals(subject1value1, state1.collectedItems.last()) // item is emitted to 1st collector of subject 1
        assertEquals(subject1value1, state1v2.collectedItems.last()) // item is emitted to 2nd collector of subject 1
        assertEquals(Container.Pending, state2.collectedItems.last()) // item should not be emitted to collector of subject 2
        // emit item by subject2
        val subject2value1 = Container.Success("2_111")
        flow2.emit(subject2value1)
        assertEquals(subject1value1, state1.collectedItems.last()) // item from subject 2 should not be emitted to 1st collector of subject 1
        assertEquals(subject1value1, state1v2.collectedItems.last()) // item from subject 2 should not be emitted to 2nd collector of subject 1
        assertEquals(subject2value1, state2.collectedItems.last()) // item is emitted to collector of subject 2
        // cancel second collector from subject1 & emit item
        state1v2.cancel()
        advanceTimeBy(timeoutMillis + 1)
        val subject1value2 = Container.Success("1_222")
        flow1.emit(subject1value2)
        assertEquals(subject1value2, state1.collectedItems.last()) // item is still emitted to 1st collector of subject 1
        assertNotEquals(Container.Pending, lazyCache.get(key1)) // cache slot should not be released yet
        assertEquals(subject2value1, state2.collectedItems.last()) // item should not be emitted to collector of other subject 2
        assertNotEquals(Container.Pending, lazyCache.get(key2)) // cache slot of subject 2 is active
        // cancel first collector from subject1
        state1.cancel()
        advanceTimeBy(timeoutMillis + 1)
        flow1.emit(Container.Success("1_333"))
        assertEquals(subject2value1, state2.collectedItems.last()) // item should not be emitted to collector of other subject 2
        assertEquals(Container.Pending, lazyCache.get(key1)) // cache slot of subject 1 should be released
        assertNotEquals(Container.Pending, lazyCache.get(key2)) // cache slot of subject 2 is active
    }

    @Test
    fun listen_afterResubscribing_doesNotRecreateCacheSlot() = scope.runFlowTest {
        val flow = MutableStateFlow<Container<String>>(Container.Pending)
        val subject = mockSubject()
        every { subject.listen() } returns flow

        val state = lazyCache.listen(key).startCollecting()
        state.cancel()
        advanceTimeBy(timeoutMillis) // timeout almost expired
        lazyCache.listen(key).startCollecting() // start collecting before expiration
        advanceTimeBy(timeoutMillis * 2)
        lazyCache.listen(key).startCollecting() // next collector should not lead to creating a new cache slot

        verify(exactly = 1) { // as a result: coroutineScope and subject should not be created twice
            coroutineScopeFactory.createScope()
            subjectFactory.create<String>(any())
        }
    }

    @Test
    fun isValueLoading_withoutActiveCacheSlot_returnsFalse() = scope.runFlowTest {
        assertFalse(lazyCache.isValueLoading(key).value)
        assertEquals(listOf(false), lazyCache.isValueLoading(key).replayCache)
        verify(exactly = 0) {
            subjectFactory.create<String>(any())
        }
    }

    @Test
    fun isValueLoading_opensCacheSlotAndListensFlowFromSubject() = scope.runFlowTest {
        val subject = mockSubject()
        val flow = MutableStateFlow(false)
        every { subject.isValueLoading() } returns flow

        val state = lazyCache.isValueLoading(key).startCollecting()

        // initial state
        assertFalse(state.collectedItems.last())
        // next state
        flow.value = true
        assertTrue(state.collectedItems.last())
    }

    @Test
    fun isValueLoading_afterStoppingCollector_releasesCacheSlot() = scope.runFlowTest {
        val subject = mockSubject()
        val flow = MutableStateFlow(true)
        every { subject.isValueLoading() } returns flow
        val state = lazyCache.isValueLoading(key).startCollecting()

        state.cancel()
        advanceTimeBy(timeoutMillis + 1)

        assertEquals(Container.Pending, lazyCache.get(key))
    }

    @Test
    fun isValueLoading_withDifferentArgs_returnsResultsFromDifferentSubjects() = scope.runFlowTest {
        val (subject1, subject2) = mockTwoSubjects()
        val flow1 = MutableStateFlow(false)
        val flow2 = MutableStateFlow(true)
        every { subject1.isValueLoading() } returns flow1
        every { subject2.isValueLoading() } returns flow2

        val collectedItems1 = lazyCache.isValueLoading(key1).startCollectingToList()
        val collectedItems2 = lazyCache.isValueLoading(key2).startCollectingToList()

        // initial state
        assertEquals(listOf(false), collectedItems1)
        assertEquals(listOf(true), collectedItems2)
        // emit items by first subject
        flow1.apply {
            emit(true)
            emit(false)
        }
        assertEquals(listOf(false, true, false), collectedItems1)
        assertEquals(listOf(true), collectedItems2)
        // emit items by second subject
        flow2.apply {
            emit(false)
            emit(true)
        }
        assertEquals(listOf(false, true, false), collectedItems1)
        assertEquals(listOf(true, false, true), collectedItems2)
    }

    @Test
    fun get_withoutActiveCacheSlot_returnsPendingContainer() {
        assertEquals(Container.Pending, lazyCache.get(key))
        verify(exactly = 0) {
            subjectFactory.create<String>(any())
        }
    }

    @Test
    fun get_withActiveCacheSlot_returnsValueFromSubject() = scope.runFlowTest {
        val expectedValue = Container.Success("123")
        val subject = mockSubject()
        every { subject.currentValue } returns expectedValue
        lazyCache.listen(key).startCollecting()

        val value = lazyCache.get(key)

        assertEquals(expectedValue, value)
    }

    @Test
    fun get_withDifferentArgs_returnsValuesFromDifferentSubjects() = runFlowTest {
        val expectedValue1 = Container.Success("1")
        val expectedValue2 = Container.Success("2")
        val (subject1, subject2) = mockTwoSubjects()
        every { subject1.currentValue } returns expectedValue1
        every { subject2.currentValue } returns expectedValue2

        lazyCache.listen(key1).startCollecting()
        lazyCache.listen(key2).startCollecting()
        val value1 = lazyCache.get(key1)
        val value2 = lazyCache.get(key2)

        assertEquals(expectedValue1, value1)
        assertEquals(expectedValue2, value2)
    }

    @Test
    fun getActiveCollectorsCount_withoutActiveCacheSlot_returnsZero() {
        assertEquals(0, lazyCache.getActiveCollectorsCount(key))
        verify(exactly = 0) {
            subjectFactory.create<String>(any())
        }
    }

    @Test
    fun getActiveCollectorsCount_withActiveCacheSlot_returnsCountFromSubject() = scope.runFlowTest {
        val expectedCount = 3
        val subject = mockSubject()
        every { subject.activeCollectorsCount } returns expectedCount
        lazyCache.listen(key).startCollecting()

        val count = lazyCache.getActiveCollectorsCount(key)

        assertEquals(expectedCount, count)
    }

    @Test
    fun getActiveCollectors_withDifferentArgs_returnCollectorsFromDifferentSubjects() = scope.runFlowTest {
        val expectedActiveCollectors1 = 21
        val expectedActiveCollectors2 = 20
        val (subject1, subject2) = mockTwoSubjects()
        every { subject1.activeCollectorsCount } returns expectedActiveCollectors1
        every { subject2.activeCollectorsCount } returns expectedActiveCollectors2

        lazyCache.listen(key1).startCollecting()
        lazyCache.listen(key2).startCollecting()
        val activeCollectors1 = lazyCache.getActiveCollectorsCount(key1)
        val activeCollectors2 = lazyCache.getActiveCollectorsCount(key2)

        assertEquals(expectedActiveCollectors1, activeCollectors1)
        assertEquals(expectedActiveCollectors2, activeCollectors2)
    }

    @Test
    fun hasActiveCollectors_withZeroActiveCollectors_returnsFalse() = scope.runFlowTest {
        val subject = mockSubject()
        every { subject.activeCollectorsCount } returns 0

        lazyCache.listen(key).startCollecting()

        assertFalse(lazyCache.hasActiveCollectors(key))
    }

    @Test
    fun hasActiveCollectors_withAtLeastOneCollector_returnsTrue() = scope.runFlowTest {
        val subject = mockSubject()
        every { subject.activeCollectorsCount } returns 1

        lazyCache.listen(key).startCollecting()

        assertTrue(lazyCache.hasActiveCollectors(key))
    }

    @Test
    fun reload_withoutActiveCacheSlot_doesNothing() = scope.runFlowTest {
        val flow = lazyCache.reload(key)
        val state = flow.startCollecting()

        verify(exactly = 0) {
            subjectFactory.create<String>(any())
        }
        assertEquals(JobStatus.Completed, state.jobStatus)
        assertTrue(state.collectedItems.isEmpty())
    }

    @Test
    fun reload_withActiveCacheSlot_reloadsValue() = scope.runFlowTest {
        val subject = mockSubject()
        val expectedFlow = flowOf("")
        every { subject.reload(silently = true) } returns expectedFlow
        lazyCache.listen(key).startCollecting()

        val flow = lazyCache.reload(key, silently = true)

        assertSame(expectedFlow, flow)
        verify(exactly = 1) {
            subject.reload(silently = true)
        }
    }

    @Test
    fun updateWith_withoutActiveCacheSlot_doesNothing() {
        lazyCache.updateWith(key, Container.Success("value"))

        verify(exactly = 0) {
            subjectFactory.create<String>(any())
        }
        assertEquals(Container.Pending, lazyCache.get(key))
    }

    @Test
    fun updateWith_withActiveCacheSlot_updatesValue() = scope.runFlowTest {
        val expectedValue = Container.Success("111")
        val subject = mockSubject()
        lazyCache.listen(key).startCollecting()

        lazyCache.updateWith(key, expectedValue)

        verify(exactly = 1) {
            subject.updateWith(expectedValue)
        }
    }

    private fun mockSubject(): LazyFlowSubject<String> {
        val subject = mockk<LazyFlowSubject<String>>(relaxUnitFun = true)
        every { subjectFactory.create<String>(any()) } returns subject
        return subject
    }

    private fun mockTwoSubjects(): Pair<LazyFlowSubject<String>, LazyFlowSubject<String>> {
        val subject1 = mockk<LazyFlowSubject<String>>(relaxUnitFun = true)
        val subject2 = mockk<LazyFlowSubject<String>>(relaxUnitFun = true)
        every { subjectFactory.create<String>(any()) } returns subject1 andThen subject2
        return subject1 to subject2
    }

}