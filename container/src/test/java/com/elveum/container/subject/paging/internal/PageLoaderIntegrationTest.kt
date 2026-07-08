package com.elveum.container.subject.paging.internal

import com.elveum.container.ContainerMetadata
import com.elveum.container.StatefulEmitter
import com.elveum.container.subject.paging.PageEmitter
import com.elveum.container.subject.paging.PageLoader
import com.elveum.container.subject.paging.PageState
import com.elveum.container.subject.paging.nextPageState
import com.elveum.container.subject.paging.onItemRendered
import com.elveum.container.subject.paging.pageLoader
import com.uandcode.flowtest.JobStatus
import com.uandcode.flowtest.runFlowTest
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PageLoaderIntegrationTest {

    @RelaxedMockK
    private lateinit var emitter: StatefulEmitter<List<String>>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun pageLoader_whenLoadStarted_emitsPendingStateAndNothingElse() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) {
            awaitCancellation()
        }

        executeInBackground { pageLoader.statefulInvoke() }

        coVerifySequence {
            emitter.loadConfig
            emitter.emitPendingState()
        }
    }

    @Test
    fun pageLoader_whenLoadStarted_setsNextPageStateToIdle() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) {
            awaitCancellation()
        }

        executeInBackground { pageLoader.statefulInvoke() }

        assertEquals(PageState.Idle, pageLoader.nextPageState.value)
    }

    @Test
    fun pageLoader_whenLoadStarted_executesInitialKeyLoad() = runFlowTest {
        val expectedInitialKey = 0
        var execCount = 0
        var actualInitialKey = -1
        val pageLoader = createPageLoader(initialKey = expectedInitialKey) {
            execCount++
            actualInitialKey = it
            awaitCancellation()
        }

        executeInBackground { pageLoader.statefulInvoke() }

        assertEquals(1, execCount)
        assertEquals(expectedInitialKey, actualInitialKey)
    }

    @Test
    fun pageLoader_whenOnlyOnePageExists_completes() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) {
            emitPage(listOf("a", "b"))
        }

        val state = executeInBackground { pageLoader.statefulInvoke() }

        assertEquals(JobStatus.Completed(Unit), state.status)
        coVerify(exactly = 1) {
            emitter.emitCompletedState()
        }
    }

    @Test
    fun pageLoader_whenPageEmitted_emitsList() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) {
            delay(10)
            emitPage(listOf("a1", "b1"))
            delay(20)
            emitPage(listOf("a2", "b2", "c2"))
            awaitCancellation()
        }

        executeInBackground { pageLoader.statefulInvoke() }
        advanceTimeBy(1)

        advanceTimeBy(10)
        coVerify(exactly = 1) {
            emitter.emit(listOf("a1", "b1"), any<ContainerMetadata>())
        }
        coVerify(exactly = 0) {
            emitter.emit(listOf("a2", "b2", "c2"), any<ContainerMetadata>())
        }

        advanceTimeBy(20)
        coVerify(exactly = 1) {
            emitter.emit(listOf("a2", "b2", "c2"), any<ContainerMetadata>())
        }
    }

    @Test
    fun pageLoader_whenPageLoadFails_emitsError() = runFlowTest {
        val expectedException = IllegalArgumentException("Oops")
        val pageLoader = createPageLoader(initialKey = 0) {
            throw expectedException
        }

        executeInBackground { pageLoader.statefulInvoke() }

        coVerify(exactly = 1) {
            emitter.emitFailureState(refEq(expectedException))
        }
    }

    @Test
    fun pageLoader_whenPageLoadFails_completes() = runFlowTest {
        val expectedException = IllegalArgumentException("Oops")
        val pageLoader = createPageLoader(initialKey = 0) {
            throw expectedException
        }

        val state = executeInBackground { pageLoader.statefulInvoke() }

        assertEquals(JobStatus.Completed(Unit), state.status)
    }

    @Test
    fun pageLoader_whenNoPagesEmitted_emitsFailureAndCompletes() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) {}

        val state = executeInBackground { pageLoader.statefulInvoke() }

        assertEquals(JobStatus.Completed(Unit), state.status)
        coVerify(exactly = 1) {
            emitter.emitFailureState(match { exception ->
                exception is IllegalStateException &&
                        exception.message?.contains("emitPage() must be called at least once") == true
            })
        }
    }

    @Test
    fun pageLoader_whenNextKeyEmitted_doesNotLoadsItImmediately() = runFlowTest {
        val triggeredIndexes = mutableSetOf<Int>()
        val pageLoader = createPageLoader(initialKey = 0, fetchDistance = 1) { index ->
            triggeredIndexes.add(index)
            emitPage(listOf("a", "b"))
            if (index == 0) emitNextKey(1)
        }

        executeInBackground { pageLoader.statefulInvoke() }

        assertEquals(setOf(0), triggeredIndexes)
    }

    @Test
    fun pageLoader_whenNextKeyEmitted_doesNotUpdateNextPageState() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            emitPage(listOf("a", "b"))
            if (index == 0) emitNextKey(1)
        }

        executeInBackground { pageLoader.statefulInvoke() }

        assertEquals(PageState.Idle, pageLoader.nextPageState.value)
    }

    @Test
    fun pageLoader_whenItemFromFirstPageRendered_showsNextPageLoadingIndicator() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            if (index == 0) {
                emitPage(listOf("a", "b"))
                emitNextKey(1)
            } else {
                awaitCancellation()
            }
        }

        executeInBackground { pageLoader.statefulInvoke() }
        pageLoader.onItemRendered(1)

        assertEquals(PageState.Pending, pageLoader.nextPageState.value)
    }

    @Test
    fun pageLoader_whenItemFromFirstPageRendered_showsNextPageLoadingIndicatorInMetadata() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            if (index == 0) {
                emitPage(listOf("a", "b"))
                emitNextKey(1)
            } else {
                awaitCancellation()
            }
        }
        val slot = slot<ContainerMetadata>()
        coEvery { emitter.emit(any(), capture(slot)) } just runs

        executeInBackground { pageLoader.statefulInvoke() }
        pageLoader.onItemRendered(1)

        assertEquals(PageState.Pending, slot.captured.nextPageState)
    }

    @Test
    fun pageLoader_whenNextPageLoadFails_updatesNextPageStateAndAwaitsForRetry() = runFlowTest {
        val expectedException = IllegalArgumentException("oops")
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            if (index == 0) {
                emitPage(listOf("a", "b"))
                emitNextKey(1)
            } else {
                throw expectedException
            }
        }

        val jobState = executeInBackground { pageLoader.statefulInvoke() }
        pageLoader.onItemRendered(1)

        val errorState = pageLoader.nextPageState.value as? PageState.Error
        assertEquals(expectedException, errorState?.exception)
        assertEquals(JobStatus.Executing, jobState.status)
        coVerify(exactly = 0) {
            emitter.emitFailureState(any())
            emitter.emitCompletedState()
        }
    }

    @Test
    fun pageLoader_whenRetryNextPageLoadAfterError_reloadsPageAndCompletes() = runFlowTest {
        val expectedException = IllegalArgumentException("oops")
        var callIndex = 0
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            if (index == 0) {
                emitPage(listOf("a", "b"))
                emitNextKey(1)
            } else {
                callIndex++
                if (callIndex == 1) {
                    throw expectedException
                } else {
                    emitPage(listOf("c", "d"))
                }
            }
        }

        val jobState = executeInBackground { pageLoader.statefulInvoke() }
        pageLoader.onItemRendered(1)
        val errorState = pageLoader.nextPageState.value as? PageState.Error
        errorState?.retry()
        runCurrent()

        assertEquals(JobStatus.Completed(Unit), jobState.status)
        coVerify(exactly = 1) {
            emitter.emit(listOf("a", "b", "c", "d"), any<ContainerMetadata>())
            emitter.emitCompletedState()
        }
    }

    @Test
    fun pageLoader_whenRetryLoadNextPageFromMetadata_reloadsPageAndCompletes() = runFlowTest {
        val expectedException = IllegalArgumentException("oops")
        var callIndex = 0
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            if (index == 0) {
                emitPage(listOf("a", "b"))
                emitNextKey(1)
            } else {
                callIndex++
                if (callIndex == 1) {
                    throw expectedException
                } else {
                    emitPage(listOf("c", "d"))
                }
            }
        }
        val slot = slot<ContainerMetadata>()
        coEvery { emitter.emit(any(), capture(slot)) } just runs

        val jobState = executeInBackground { pageLoader.statefulInvoke() }
        pageLoader.onItemRendered(1)
        val errorState = slot.captured.nextPageState as? PageState.Error
        errorState?.retry()
        runCurrent()

        assertEquals(JobStatus.Completed(Unit), jobState.status)
        coVerify(exactly = 1) {
            emitter.emit(listOf("a", "b", "c", "d"), any<ContainerMetadata>())
            emitter.emitCompletedState()
        }
    }

    @Test
    fun pageLoader_whenMultiplePagesExists_loadsEachPageAfterItemRenderingFromPreviousPage() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0, fetchDistance = 1) { index ->
            emitPage(listOf("item-$index-0", "item-$index-1"))
            if (index < 3) emitNextKey(index + 1)
        }

        val jobState = executeInBackground { pageLoader.statefulInvoke() }

        // page 0
        coVerify(exactly = 1) {
            emitter.emitPendingState()
            emitter.emit(listOf("item-0-0", "item-0-1"), any<ContainerMetadata>())
        }
        coVerify(exactly = 0) {
            emitter.emit(
                listOf("item-0-0", "item-0-1", "item-1-0", "item-1-1"),
                any<ContainerMetadata>(),
            )
        }

        // page1
        pageLoader.onItemRendered(1)
        coVerify(exactly = 1) {
            emitter.emit(
                listOf("item-0-0", "item-0-1", "item-1-0", "item-1-1"),
                any<ContainerMetadata>(),
            )
        }
        coVerify(exactly = 0) {
            emitter.emit(
                listOf("item-0-0", "item-0-1", "item-1-0", "item-1-1", "item-2-0", "item-2-1"),
                any<ContainerMetadata>(),
            )
        }

        // page 2
        pageLoader.onItemRendered(3)
        coVerify(exactly = 1) {
            emitter.emit(
                listOf("item-0-0", "item-0-1", "item-1-0", "item-1-1", "item-2-0", "item-2-1"),
                any<ContainerMetadata>(),
            )
        }
        coVerify(exactly = 0) {
            emitter.emit(
                listOf("item-0-0", "item-0-1", "item-1-0", "item-1-1", "item-2-0", "item-2-1",
                    "item-3-0", "item-3-1"),
                any<ContainerMetadata>(),
            )
        }

        // page 3
        pageLoader.onItemRendered(5)
        coVerify(exactly = 1) {
            emitter.emit(
                listOf("item-0-0", "item-0-1", "item-1-0", "item-1-1", "item-2-0", "item-2-1",
                    "item-3-0", "item-3-1"),
                any<ContainerMetadata>(),
            )
        }
        assertEquals(JobStatus.Completed(Unit), jobState.status)
    }

    @Test
    fun pageLoader_whenEmitPageCalledMultipleTimes_updatesPageData() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) {
            emitPage(listOf("local-a", "local-b"))
            delay(10)
            emitPage(listOf("remote-a", "remote-b", "remote-c"))
            awaitCancellation()
        }

        executeInBackground { pageLoader.statefulInvoke() }
        advanceTimeBy(1)

        coVerify(exactly = 1) {
            emitter.emit(listOf("local-a", "local-b"), any<ContainerMetadata>())
        }

        advanceTimeBy(10)
        coVerify(exactly = 1) {
            emitter.emit(listOf("remote-a", "remote-b", "remote-c"), any<ContainerMetadata>())
        }
    }

    @Test
    fun pageLoader_whenEmitNextKeyCalledTwice_executesBothKeys() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) { key ->
            when (key) {
                0 -> {
                    emitPage(listOf("a"))
                    emitNextKey(1)
                    emitNextKey(2)
                }
                1 -> emitPage(listOf("b"))
                2 -> emitPage(listOf("b-updated"))
            }
        }

        executeInBackground { pageLoader.statefulInvoke() }
        runCurrent()

        coVerifyOrder {
            emitter.emit(listOf("a"), any<ContainerMetadata>())
            emitter.emit(listOf("a", "b"), any<ContainerMetadata>())
            emitter.emit(listOf("a", "b-updated"), any<ContainerMetadata>())
        }
    }

    @Test
    fun pageLoader_whenTheSameKeyEmittedTwice_executesKeyOnce() = runFlowTest {
        val executedKeys = mutableListOf<Int>()
        val pageLoader = createPageLoader(initialKey = 0) { key ->
            executedKeys.add(key)
            when (key) {
                0 -> {
                    emitPage(listOf("a"))
                    emitNextKey(1)
                    emitNextKey(1)
                }
                1 -> emitPage(listOf("b"))
            }
        }

        executeInBackground { pageLoader.statefulInvoke() }
        runCurrent()

        coVerifyOrder {
            emitter.emit(listOf("a"), any<ContainerMetadata>())
            emitter.emit(listOf("a", "b"), any<ContainerMetadata>())
        }
        assertEquals(
            listOf(0, 1),
            executedKeys,
        )
    }

    @Test
    fun pageLoader_whenOnItemRenderedCalledBeforeLoad_doesNotCrash() {
        val pageLoader = createPageLoader(initialKey = 0) {
            emitPage(listOf("a"))
        }

        // Should not throw - launcher is null before statefulInvoke
        pageLoader.onItemRendered(0)
    }

    @Test
    fun pageLoader_whenOnItemRenderedForMiddleOfLastPage_doesNotTriggerLoad() = runFlowTest {
        val loadedKeys = mutableListOf<Int>()
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            loadedKeys.add(index)
            emitPage(listOf("a", "b", "c"))
            if (index == 0) emitNextKey(1)
        }

        executeInBackground { pageLoader.statefulInvoke() }
        // index 0 is in the middle of a 3-item page with next key registered
        // This should trigger loading of key 1
        pageLoader.onItemRendered(0)

        // index 1 is also in page 0's range, next key is 1 which is already launched
        pageLoader.onItemRendered(1)

        // key 1 should only be loaded once
        assertEquals(listOf(0, 1), loadedKeys)
    }

    @Test
    fun pageLoader_afterCompletion_nextPageStateResetsToIdle() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            if (index == 0) {
                emitPage(listOf("a"))
                emitNextKey(1)
            } else {
                emitPage(listOf("b"))
            }
        }

        val jobState = executeInBackground { pageLoader.statefulInvoke() }
        pageLoader.onItemRendered(0)

        assertEquals(JobStatus.Completed(Unit), jobState.status)
        assertEquals(PageState.Idle, pageLoader.nextPageState.value)
    }

    @Test
    fun pageLoader_afterInitialPageFailure_nextPageStateResetsToIdle() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) {
            throw RuntimeException("fail")
        }

        val jobState = executeInBackground { pageLoader.statefulInvoke() }

        assertEquals(JobStatus.Completed(Unit), jobState.status)
        assertEquals(PageState.Idle, pageLoader.nextPageState.value)
    }

    @Test
    fun pageLoader_whenLastItemOfLastPageRendered_andNextKeyEmittedLater_loadsNextPageImmediately() = runFlowTest {
        // When the user renders the last item of the last loaded page,
        // and there's no next key yet, ScheduleImmediateLoad is triggered.
        // When the block then emits a next key, it should launch immediately.
        val loadedKeys = mutableListOf<Int>()
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            loadedKeys.add(index)
            if (index == 0) {
                emitPage(listOf("a", "b"))
                delay(10)
                emitNextKey(1)
            } else {
                emitPage(listOf("c"))
            }
        }

        executeInBackground { pageLoader.statefulInvoke() }

        // Render last item of the only page -> triggers ScheduleImmediateLoad
        pageLoader.onItemRendered(1) // 'b' has been rendered
        advanceTimeBy(11) // trigger emitNextKey() after delay(10)
        // As a result, emitNextKey is called after rendering the last item -> need to load next page:
        assertEquals(listOf(0, 1), loadedKeys)
        coVerify(exactly = 1) {
            emitter.emit(listOf("a", "b", "c"), any<ContainerMetadata>())
        }
    }

    @Test
    fun pageLoader_whenNextPageLoadedSuccessfully_emitsConcatenatedList() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            if (index == 0) {
                emitPage(listOf("a", "b"))
                emitNextKey(1)
            } else {
                emitPage(listOf("c", "d"))
            }
        }

        val jobState = executeInBackground { pageLoader.statefulInvoke() }
        pageLoader.onItemRendered(1)

        assertEquals(JobStatus.Completed(Unit), jobState.status)
        coVerify(exactly = 1) {
            emitter.emit(listOf("a", "b", "c", "d"), any<ContainerMetadata>())
        }
    }

    @Test
    fun pageLoader_whenSameItemRenderedMultipleTimes_loadsNextPageOnlyOnce() = runFlowTest {
        var page1LoadCount = 0
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            if (index == 0) {
                emitPage(listOf("a", "b"))
                emitNextKey(1)
            } else {
                page1LoadCount++
                awaitCancellation()
            }
        }

        executeInBackground { pageLoader.statefulInvoke() }
        pageLoader.onItemRendered(1)
        runCurrent()
        pageLoader.onItemRendered(1)
        runCurrent()
        pageLoader.onItemRendered(1)
        runCurrent()

        assertEquals(1, page1LoadCount)
    }

    @Test
    fun pageLoader_withEmptyPageList_emitsEmptyList() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) {
            emitPage(emptyList())
        }

        val jobState = executeInBackground { pageLoader.statefulInvoke() }

        assertEquals(JobStatus.Completed(Unit), jobState.status)
        coVerify(exactly = 1) {
            emitter.emit(emptyList(), any<ContainerMetadata>())
        }
    }

    @Test
    fun pageLoader_whenNextPageCompletes_nextPageStateBecomesIdle() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            if (index == 0) {
                emitPage(listOf("a"))
                emitNextKey(1)
            } else {
                emitPage(listOf("b"))
            }
        }

        executeInBackground { pageLoader.statefulInvoke() }

        // Before triggering next page
        assertEquals(PageState.Idle, pageLoader.nextPageState.value)

        // Trigger next page load
        pageLoader.onItemRendered(0)

        // After next page completes successfully
        assertEquals(PageState.Idle, pageLoader.nextPageState.value)
    }

    @Test
    fun pageLoader_whenMultipleNextPagesFail_emitsFailureAndCompletes() = runFlowTest {
        // When page 0 fails initially (all containers are error) -> shutdown -> complete with failure
        val exception = RuntimeException("initial fail")
        val pageLoader = createPageLoader(initialKey = 0) {
            throw exception
        }

        val jobState = executeInBackground { pageLoader.statefulInvoke() }

        assertEquals(JobStatus.Completed(Unit), jobState.status)
        coVerify(exactly = 1) {
            emitter.emitFailureState(refEq(exception))
        }
    }

    @Test
    fun pageLoader_whenRetryAfterNextPageError_resetsNextPageStateToPending() = runFlowTest {
        var callIndex = 0
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            if (index == 0) {
                emitPage(listOf("a"))
                emitNextKey(1)
            } else {
                callIndex++
                if (callIndex == 1) {
                    throw RuntimeException("fail")
                } else {
                    awaitCancellation()
                }
            }
        }

        executeInBackground { pageLoader.statefulInvoke() }
        pageLoader.onItemRendered(0)

        val errorState = pageLoader.nextPageState.value as? PageState.Error
        errorState?.retry()
        runCurrent()

        assertEquals(PageState.Pending, pageLoader.nextPageState.value)
    }

    @Test
    fun pageLoader_withStringKeys_worksCorrectly() = runFlowTest {
        val pageLoader = pageLoader<String, String>(
            initialKey = "first",
            itemId = { it },
        ) { key ->
            when (key) {
                "first" -> {
                    emitPage(listOf("a"))
                    emitNextKey("second")
                }
                "second" -> {
                    emitPage(listOf("b"))
                }
            }
        }

        val jobState = executeInBackground { with(pageLoader) { emitter.statefulInvoke() } }
        pageLoader.onItemRendered(0)

        assertEquals(JobStatus.Completed(Unit), jobState.status)
        coVerify(exactly = 1) {
            emitter.emit(listOf("a", "b"), any<ContainerMetadata>())
        }
    }

    @Test
    fun pageLoader_whenNextPageFailsAfterFirstPageSucceeded_firstPageDataIsPreserved() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            if (index == 0) {
                emitPage(listOf("a", "b"))
                emitNextKey(1)
            } else {
                throw RuntimeException("next page fail")
            }
        }

        executeInBackground { pageLoader.statefulInvoke() }
        pageLoader.onItemRendered(1)

        // Should NOT emit failure state (only next page failed, first page is ok)
        coVerify(exactly = 0) {
            emitter.emitFailureState(any())
        }
        assertTrue(pageLoader.nextPageState.value is PageState.Error)
    }

    // Fetch Distance Tests:
    // 4-item page, fetchDistance=2 -> thresholdIndex = 2
    // Indices 0..1 are below threshold; indices 2..3 are at/above threshold.

    @Test
    fun pageLoader_whenItemBelowThresholdFromFirstPageRenderedByMetadata_doesNotStartLoadingNextPage() = runFlowTest {
        var nextPageLoadCalls = 0
        val pageLoader = createPageLoader(initialKey = 0, fetchDistance = 2) { index ->
            if (index == 0) {
                emitPage(listOf("a", "b", "c", "d"))
                emitNextKey(1)
            } else {
                nextPageLoadCalls++
                awaitCancellation()
            }
        }
        val slot = slot<ContainerMetadata>()
        coEvery { emitter.emit(any(), capture(slot)) } just runs

        executeInBackground { pageLoader.statefulInvoke() }
        slot.captured.onItemRendered(1)

        assertEquals(0, nextPageLoadCalls)
    }

    @Test
    fun pageLoader_whenItemAtThresholdFromFirstPageRenderedByMetadata_startsLoadingNextPage() = runFlowTest {
        var nextPageLoadCalls = 0
        val pageLoader = createPageLoader(initialKey = 0, fetchDistance = 2) { index ->
            if (index == 0) {
                emitPage(listOf("a", "b", "c", "d"))
                emitNextKey(1)
            } else {
                nextPageLoadCalls++
                awaitCancellation()
            }
        }
        val slot = slot<ContainerMetadata>()
        coEvery { emitter.emit(any(), capture(slot)) } just runs

        executeInBackground { pageLoader.statefulInvoke() }
        slot.captured.onItemRendered(2)

        assertEquals(1, nextPageLoadCalls)
    }

    @Test
    fun pageLoader_whenItemBelowFetchDistanceRendered_doesNotLoadNextPage() = runFlowTest {
        val loadedKeys = mutableListOf<Int>()
        val pageLoader = createPageLoader(initialKey = 0, fetchDistance = 2) { index ->
            loadedKeys.add(index)
            emitPage(listOf("a", "b", "c", "d"))
            if (index == 0) emitNextKey(1)
        }

        executeInBackground { pageLoader.statefulInvoke() }

        pageLoader.onItemRendered(1) // index 1 is below threshold (1)

        assertEquals(listOf(0), loadedKeys)
    }

    @Test
    fun pageLoader_whenItemAtFetchDistanceRendered_loadsNextPage() = runFlowTest {
        val loadedKeys = mutableListOf<Int>()
        val pageLoader = createPageLoader(initialKey = 0, fetchDistance = 2) { index ->
            loadedKeys.add(index)
            emitPage(listOf("a", "b", "c", "d"))
            if (index == 0) emitNextKey(1)
        }

        executeInBackground { pageLoader.statefulInvoke() }

        pageLoader.onItemRendered(2) // index 2 is exactly at threshold

        assertEquals(listOf(0, 1), loadedKeys)
    }

    @Test
    fun pageLoader_whenItemAboveFetchDistanceRendered_loadsNextPage() = runFlowTest {
        val loadedKeys = mutableListOf<Int>()
        val pageLoader = createPageLoader(initialKey = 0, fetchDistance = 2) { index ->
            loadedKeys.add(index)
            emitPage(listOf("a", "b", "c", "d"))
            if (index == 0) emitNextKey(1)
        }

        executeInBackground { pageLoader.statefulInvoke() }

        pageLoader.onItemRendered(3) // index 3 is above threshold

        assertEquals(listOf(0, 1), loadedKeys)
    }

    @Test
    fun pageLoader_withMinFetchDistance_whenItemBeforeLastRendered_doesNotLoadNextPage() = runFlowTest {
        val loadedKeys = mutableListOf<Int>()
        val pageLoader = createPageLoader(initialKey = 0, fetchDistance = 0) { index ->
            loadedKeys.add(index)
            emitPage(listOf("a", "b", "c", "d"))
            if (index == 0) emitNextKey(1)
        }

        executeInBackground { pageLoader.statefulInvoke() }

        pageLoader.onItemRendered(0)
        pageLoader.onItemRendered(1)
        pageLoader.onItemRendered(2) // all below threshold (3 = last index)

        assertEquals(listOf(0), loadedKeys)
    }

    @Test
    fun pageLoader_withMinFetchDistance_whenLastItemRendered_loadsNextPage() = runFlowTest {
        val loadedKeys = mutableListOf<Int>()
        val pageLoader = createPageLoader(initialKey = 0, fetchDistance = 0) { index ->
            loadedKeys.add(index)
            emitPage(listOf("a", "b", "c", "d"))
            if (index == 0) emitNextKey(1)
        }

        executeInBackground { pageLoader.statefulInvoke() }

        pageLoader.onItemRendered(3) // index 3 is the last item

        assertEquals(listOf(0, 1), loadedKeys)
    }

    @Test
    fun pageLoader_withDelayedNextKeyWhenItemBelowThreshold_nextKeyNotLoadedImmediately() = runFlowTest {
        // ScheduleImmediateLoad path: emitNextKey is called after the user renders an item.
        // When the item is below threshold, ScheduleImmediateLoad is NOT set,
        // so emitNextKey registers the key as pending rather than launching immediately.
        val loadedKeys = mutableListOf<Int>()
        val pageLoader = createPageLoader(initialKey = 0, fetchDistance = 2) { index ->
            loadedKeys.add(index)
            if (index == 0) {
                emitPage(listOf("a", "b", "c", "d"))
                delay(10)
                emitNextKey(1)
            } else {
                emitPage(listOf("e"))
            }
        }

        executeInBackground { pageLoader.statefulInvoke() }

        pageLoader.onItemRendered(1)     // index 1 is below threshold (2): no ScheduleImmediateLoad
        advanceTimeBy(11)                // emitNextKey fires; key registered as pending, not launched

        // Page 1 should NOT be loaded yet
        assertEquals(listOf(0), loadedKeys)

        // Rendering an item below threshold via the Key path also skips loading
        pageLoader.onItemRendered(0)
        assertEquals(listOf(0), loadedKeys)

        // Rendering at/above threshold via the Key path triggers the launch
        pageLoader.onItemRendered(2)
        assertEquals(listOf(0, 1), loadedKeys)
    }

    @Test
    fun pageLoader_withDelayedNextKey_whenItemAtThreshold_nextKeyLoadedImmediately() = runFlowTest {
        // ScheduleImmediateLoad path: when the item at/above threshold is rendered before
        // emitNextKey, ScheduleImmediateLoad IS set, so emitNextKey launches immediately.
        val loadedKeys = mutableListOf<Int>()
        val pageLoader = createPageLoader(initialKey = 0, fetchDistance = 2) { index ->
            loadedKeys.add(index)
            if (index == 0) {
                emitPage(listOf("a", "b", "c", "d"))
                delay(10)
                emitNextKey(1)
            } else {
                emitPage(listOf("e"))
            }
        }

        executeInBackground { pageLoader.statefulInvoke() }

        pageLoader.onItemRendered(2)    // index 2 is at threshold: ScheduleImmediateLoad is set
        advanceTimeBy(11) // emitNextKey fires and launches immediately

        assertEquals(listOf(0, 1), loadedKeys)
    }

    @Test
    fun pageLoader_withDifferentKeys_completesOnlyWhenAllKeysCompletes() = runFlowTest {
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            when (index) {
                0 -> {
                    emitPage(listOf("item1"))
                    emitNextKey(1)
                    emitNextKey(2)
                }
                1 -> {
                    delay(10)
                    emitPage(listOf("item2"))
                }
                2 -> {
                    delay(20)
                    emitPage(listOf("item2-updated"))
                }
            }
        }

        val state = executeInBackground { pageLoader.statefulInvoke() }

        advanceTimeBy(19) // only one next key has been processed:
        assertEquals(JobStatus.Executing, state.status)

        advanceTimeBy(2) // both keys are processed:
        assertEquals(JobStatus.Completed(Unit), state.status)

        coVerifyOrder {
            emitter.emit(listOf("item1"), any<ContainerMetadata>())
            emitter.emit(listOf("item1", "item2"), any<ContainerMetadata>())
            emitter.emit(listOf("item1", "item2-updated"), any<ContainerMetadata>())
        }
    }


    @Test
    fun pageLoader_withDifferentKeyPaths_completesOnlyWhenAllKeysCompletes() = runFlowTest {
        var emitError = true
        val pageLoader = createPageLoader(initialKey = 0) { index ->
            when (index) {
                0 -> { // root: 0
                    emitPage(listOf("item1"))
                    emitNextKey(1)
                    emitNextKey(2)
                }
                1 -> { // branch A: 0 -> 1
                    delay(10)
                    emitPage(listOf("item2"))
                    emitNextKey(3)
                }
                2 -> { // branch B: 0 -> 2
                    delay(20)
                    emitPage(listOf("item2-updated"))
                    emitNextKey(4)
                }
                3 -> { // branch A: 0 -> 1 -> 3
                    delay(5)
                    emitPage(listOf("item3"))
                }
                4 -> { // branch B: 0 -> 2 -> 5
                    delay(5)
                    if (emitError) {
                        throw IllegalStateException("oops")
                    } else {
                        emitPage(listOf("item3-updated"))
                    }
                }
            }
        }

        val state = executeInBackground { pageLoader.statefulInvoke() }

        // load session should be still active, since error happened on the last key branch
        advanceTimeBy(30)
        assertEquals(JobStatus.Executing, state.status)

        // retrying:
        emitError = false
        (pageLoader.nextPageState.value as PageState.Error).retry()
        advanceTimeBy(6)
        // session must be finished:
        assertEquals(JobStatus.Completed(Unit), state.status)
        // and verify final items:
        coVerify {
            emitter.emit(listOf("item1", "item2-updated", "item3-updated"), any<ContainerMetadata>())
        }
    }



    private suspend fun PageLoader<Int, String>.statefulInvoke() {
        emitter.statefulInvoke()
    }

    private fun createPageLoader(
        initialKey: Int,
        fetchDistance: Int = 10,
        block: suspend PageEmitter<Int, String>.(Int) -> Unit,
    ): PageLoader<Int, String> {
        return pageLoader(
            initialKey = initialKey,
            itemId = { it },
            block = block,
            fetchDistance = fetchDistance,
        )
    }

}
