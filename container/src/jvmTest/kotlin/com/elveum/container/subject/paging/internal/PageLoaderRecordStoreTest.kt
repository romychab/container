@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
import com.elveum.container.errorContainer
import com.elveum.container.getOrNull
import com.elveum.container.pendingContainer
import com.elveum.container.subject.paging.internal.PageLoaderRecordStore.NextKeyLoadResult
import com.elveum.container.successContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PageLoaderRecordStoreTest {

    private lateinit var store: PageLoaderRecordStore<Int, String>

    @Before
    fun setUp() {
        store = PageLoaderRecordStore(fetchDistance = 2) { it }
    }

    @Test
    fun getOrPut_addsNewRecord() {
        val record = store.getOrPut(1) { PageKeyRecord(1) }

        assertEquals(1, record!!.key)
        assertEquals(listOf(1), store.keys())
    }

    @Test
    fun getOrPut_returnsExistingRecordForDuplicateKey() {
        val first = store.getOrPut(1) { PageKeyRecord(1) }
        val second = store.getOrPut(1) { PageKeyRecord(1) }

        assertSame(first, second)
    }

    @Test
    fun getOrPut_doesNotIncrementCounterForDuplicateKey() = runTest {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(1) { PageKeyRecord(1) } // duplicate, counter stays 1

        store.onKeyCompleted(1) // counter 1 -> 0, unlocks mutex
        store.await() // should complete immediately
    }

    @Test
    fun updateContainer_updatesExistingRecord() {
        store.getOrPut(1) { PageKeyRecord(1) }
        val container = successContainer(listOf("a", "b"))

        store.updateContainer(1, container)

        assertEquals(listOf(container), store.getAllContainers())
    }

    @Test
    fun updateContainer_doesNothingForNonExistentKey() {
        store.updateContainer(999, successContainer(listOf("a")))

        assertTrue(store.getAllContainers().isEmpty())
    }

    @Test
    fun hasLaunchedKey_returnsFalseForNonExistentKey() {
        assertFalse(store.hasLaunchedKey(1))
    }

    @Test
    fun hasLaunchedKey_returnsFalseWhenJobIsNull() {
        store.getOrPut(1) { PageKeyRecord(1, job = null) }

        assertFalse(store.hasLaunchedKey(1))
    }

    @Test
    fun hasLaunchedKey_returnsTrueWhenJobExists() {
        store.getOrPut(1) { PageKeyRecord(1, job = Job()) }

        assertTrue(store.hasLaunchedKey(1))
    }

    @Test
    fun forEach_iteratesInInsertionOrder() {
        store.getOrPut(3) { PageKeyRecord(3) }
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }

        val keys = mutableListOf<Int>()
        store.forEach { keys.add(it.key) }

        assertEquals(listOf(3, 1, 2), keys)
    }

    @Test
    fun forEach_forEmptyStore_doesNothing() {
        var called = false

        store.forEach { called = true }

        assertFalse(called)
    }

    @Test
    fun keys_returnsAllKeysInInsertionOrder() {
        store.getOrPut(3) { PageKeyRecord(3) }
        store.getOrPut(1) { PageKeyRecord(1) }

        assertEquals(listOf(3, 1), store.keys())
    }

    @Test
    fun keys_emptyStore_returnsEmptyList() {
        assertEquals(emptyList<Int>(), store.keys())
    }

    @Test
    fun firstOrNull_returnsFirstMatchingRecord() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.updateContainer(2, successContainer(listOf("a")))

        val result = store.firstOrNull { it.container is Container.Success }

        assertNotNull(result)
        assertEquals(2, result!!.key)
    }

    @Test
    fun firstOrNull_returnsNullWhenNoMatch() {
        store.getOrPut(1) { PageKeyRecord(1) }

        val result = store.firstOrNull { it.container is Container.Success }

        assertNull(result)
    }

    @Test
    fun getAllContainers_returnsContainersInInsertionOrder() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.updateContainer(1, successContainer(listOf("a")))

        val containers = store.getAllContainers()

        assertEquals(2, containers.size)
        assertTrue(containers[0] is Container.Success)
        assertTrue(containers[1] is Container.Pending)
    }

    @Test
    fun getAllContainers_forEmptyStore_returnsEmptyList() {
        assertTrue(store.getAllContainers().isEmpty())
    }

    @Test
    fun buildOutputList_flattensSuccessContainers() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.updateContainer(1, successContainer(listOf("a", "b")))
        store.updateContainer(2, successContainer(listOf("c")))

        assertEquals(listOf("a", "b", "c"), store.buildOutputList())
    }

    @Test
    fun buildOutputList_skipsPendingAndErrorContainers() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.getOrPut(3) { PageKeyRecord(3) }
        store.updateContainer(1, successContainer(listOf("a")))
        // key 2 stays pending
        store.updateContainer(3, errorContainer(RuntimeException("fail")))

        assertEquals(listOf("a"), store.buildOutputList())
    }

    @Test
    fun buildOutputList_forEmptyStore_returnsEmptyList() {
        assertEquals(emptyList<String>(), store.buildOutputList())
    }

    @Test
    fun findNextKeyForIndex_forEmptyStore_returnsSkip() {
        assertEquals(NextKeyLoadResult.Skip, store.findNextKeyForIndex(0))
    }

    @Test
    fun findNextKeyForIndex_whenErrorExists_returnsSkip() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.updateContainer(1, successContainer(listOf("a", "b")))
        store.updateContainer(2, errorContainer(RuntimeException("fail")))

        assertEquals(NextKeyLoadResult.Skip, store.findNextKeyForIndex(0))
    }

    @Test
    fun findNextKeyForIndex_whenIndexInLoadedPageAtThreshold_returnsNextPageKey() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.getOrPut(3) { PageKeyRecord(3) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        store.updateContainer(2, successContainer(listOf("d", "e")))

        assertEquals(NextKeyLoadResult.Key(3), store.findNextKeyForIndex(4))
    }

    @Test
    fun findNextKeyForIndex_whenIndexInLoadedPageBelowThreshold_returnsSkip() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.getOrPut(3) { PageKeyRecord(3) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        store.updateContainer(2, successContainer(listOf("d", "e")))

        assertEquals(NextKeyLoadResult.Skip, store.findNextKeyForIndex(3))
    }

    @Test
    fun findNextKeyForIndex_whenIndexIsLastItemOfLastPage_returnsScheduleImmediateLoad() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.updateContainer(1, successContainer(listOf("a", "b")))
        store.updateContainer(2, successContainer(listOf("c", "d")))

        // items: 0=a, 1=b, 2=c, 3=d -> index 3 is last
        assertEquals(
            NextKeyLoadResult.ScheduleImmediateLoad,
            store.findNextKeyForIndex(3),
        )
    }

    @Test
    fun findNextKeyForIndex_whenIndexInMiddleOfLastPage_returnsSkip() {
        store = PageLoaderRecordStore(fetchDistance = 5) { it }
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(1) }
        // 10-item page: threshold=5 -> thresholdIndex = 5; index=4 is below threshold
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")))

        assertEquals(NextKeyLoadResult.Skip, store.findNextKeyForIndex(4))
    }

    @Test
    fun findNextKeyForIndex_whenNextPageIsPending_andIndexInPreviousPage_returnsNextKey() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.updateContainer(1, successContainer(listOf("a", "b")))
        // key 2 stays pending

        // index 1 is in page 1, next record is key 2
        assertEquals(NextKeyLoadResult.Key(2), store.findNextKeyForIndex(1))
    }

    @Test
    fun findNextKeyForIndex_whenIndexBeyondLoadedPages_andNextPagePending_returnsSkip() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.updateContainer(1, successContainer(listOf("a", "b")))
        // key 2 stays pending

        // index 2 would be in page 2's range, but page 2 is pending (getOrNull = null)
        assertEquals(NextKeyLoadResult.Skip, store.findNextKeyForIndex(2))
    }

    @Test
    fun findNextKeyForIndex_whenIndexIsOutOfBounds_returnsSkip() {
        store.getOrPut(1) { PageKeyRecord(1, container = successContainer(listOf("a", "b", "c"))) }
        store.getOrPut(2) { PageKeyRecord(2) }

        // out of low bound:
        assertEquals(NextKeyLoadResult.Skip, store.findNextKeyForIndex(-1))
        // in bounds:
        assertEquals(NextKeyLoadResult.Key(2), store.findNextKeyForIndex(2))
        // out of upper bound:
        assertEquals(NextKeyLoadResult.Skip, store.findNextKeyForIndex(3))
    }

    @Test
    fun findNextKeyForIndex_singlePageWithLastIndex_returnsScheduleImmediateLoad() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.updateContainer(1, successContainer(listOf("a", "b")))

        assertEquals(
            NextKeyLoadResult.ScheduleImmediateLoad,
            store.findNextKeyForIndex(1),
        )
    }

    @Test
    fun findNextKeyForIndex_singlePage_whenIndexBelowThreshold_returnsSkip() {
        // 4-item page: threshold=2 -> thresholdIndex=2; index=1 is below threshold
        store.getOrPut(1) { PageKeyRecord(1) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))

        assertEquals(NextKeyLoadResult.Skip, store.findNextKeyForIndex(1))
    }

    @Test
    fun findNextKeyForIndex_singlePage_whenIndexAtThreshold_returnsScheduleImmediateLoad() {
        // 4-item page: threshold=2 -> thresholdIndex=2; index=2 is at threshold
        store.getOrPut(1) { PageKeyRecord(1) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))

        assertEquals(NextKeyLoadResult.ScheduleImmediateLoad, store.findNextKeyForIndex(2))
    }

    @Test
    fun findNextKeyForIndex_singlePage_whenIndexAboveThreshold_returnsScheduleImmediateLoad() {
        // 4-item page: threshold=2 -> thresholdIndex=2; index=3 is above threshold
        store.getOrPut(1) { PageKeyRecord(1) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))

        assertEquals(NextKeyLoadResult.ScheduleImmediateLoad, store.findNextKeyForIndex(3))
    }

    @Test
    fun findNextKeyForIndex_pendingNextPage_whenIndexBelowThreshold_returnsSkip() {
        // 4-item page: threshold=2 -> thresholdIndex=2; index=1 is below threshold
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        // key 2 stays pending

        assertEquals(NextKeyLoadResult.Skip, store.findNextKeyForIndex(1))
    }

    @Test
    fun findNextKeyForIndex_pendingNextPage_whenIndexAtThreshold_returnsNextKey() {
        // 4-item page: threshold=2 -> thresholdIndex=2; index=2 is at threshold
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        // key 2 stays pending

        assertEquals(NextKeyLoadResult.Key(2), store.findNextKeyForIndex(2))
    }

    @Test
    fun findNextKeyForIndex_pendingNextPage_whenIndexAboveThreshold_returnsNextKey() {
        // 4-item page: threshold=2 -> thresholdIndex=2; index=3 is above threshold
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        // key 2 stays pending

        assertEquals(NextKeyLoadResult.Key(2), store.findNextKeyForIndex(3))
    }

    @Test
    fun findNextKeyForIndex_withMinDistance_whenIndexBeforeLastItem_returnsSkip() {
        val storeWithThreshold1 = PageLoaderRecordStore<Int, String>(fetchDistance = 0) { it }
        storeWithThreshold1.getOrPut(1) { PageKeyRecord(1) }
        storeWithThreshold1.getOrPut(2) { PageKeyRecord(2) }
        // 4-item page: threshold=min -> thresholdIndex=3 (last item only)
        storeWithThreshold1.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        // key 2 stays pending

        assertEquals(NextKeyLoadResult.Skip, storeWithThreshold1.findNextKeyForIndex(2))
    }

    @Test
    fun findNextKeyForIndex_withMinDistance_whenIndexAtLastItem_returnsNextKey() {
        val storeWithThreshold1 = PageLoaderRecordStore<Int, String>(fetchDistance = 0) { it }
        storeWithThreshold1.getOrPut(1) { PageKeyRecord(1) }
        storeWithThreshold1.getOrPut(2) { PageKeyRecord(2) }
        // 4-item page: threshold=min -> thresholdIndex=3 (last item only)
        storeWithThreshold1.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        // key 2 stays pending

        assertEquals(NextKeyLoadResult.Key(2), storeWithThreshold1.findNextKeyForIndex(3))
    }

    @Test
    fun findNextKeyForIndex_withMaxDistance_whenIndexAtFirstItem_returnsNextKey() {
        val storeWithThreshold0 = PageLoaderRecordStore<Int, String>(fetchDistance = Int.MAX_VALUE) { it }
        storeWithThreshold0.getOrPut(1) { PageKeyRecord(1) }
        storeWithThreshold0.getOrPut(2) { PageKeyRecord(2) }
        // 4-item page: threshold=max -> thresholdIndex=0 (fires on first rendered item)
        storeWithThreshold0.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        // key 2 stays pending

        assertEquals(NextKeyLoadResult.Key(2), storeWithThreshold0.findNextKeyForIndex(0))
    }

    @Test
    fun await_completesWhenAllKeysCompleted() = runTest {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }

        store.onKeyCompleted(1)
        store.onKeyCompleted(2)

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            store.await()
        }
        assertTrue(job.isCompleted)
    }

    @Test
    fun await_completesWhenSingleKeyCompleted() = runTest {
        store.getOrPut(1) { PageKeyRecord(1) }

        store.onKeyCompleted(1)

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            store.await()
        }
        assertTrue(job.isCompleted)
    }

    @Test
    fun onKeyContinued_setsContainerToPending() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.updateContainer(1, successContainer(listOf("a")))

        store.onKeyContinued(1)

        val containers = store.getAllContainers()
        assertTrue(containers[0] is Container.Pending)
    }

    @Test
    fun onKeyFailed_setsErrorContainerForFailedKey() {
        store.getOrPut(1) { PageKeyRecord(1) }
        val exception = RuntimeException("fail")

        store.onKeyFailed(1, exception)

        val container = store.getAllContainers()[0]
        assertTrue(container is Container.Error)
    }

    @Test
    fun onKeyFailed_doesNotCancelKeysBeforeFailedKey() {
        val job1 = Job()
        store.getOrPut(1) { PageKeyRecord(1, job = job1) }
        store.getOrPut(2) { PageKeyRecord(2, job = Job()) }
        store.getOrPut(3) { PageKeyRecord(3, job = Job()) }

        store.onKeyFailed(2, RuntimeException("fail"))

        assertFalse(job1.isCancelled)
        assertTrue(store.keys().contains(1))
    }

    @Test
    fun onKeyFailed_cancelsAllKeysAfterFailedKey() {
        val job2 = Job()
        val job3 = Job()
        val job4 = Job()
        store.getOrPut(1) { PageKeyRecord(1, job = Job()) }
        store.getOrPut(2) { PageKeyRecord(2, job = job2) }
        store.getOrPut(3) { PageKeyRecord(3, job = job3) }
        store.getOrPut(4) { PageKeyRecord(4, job = job4) }

        store.onKeyFailed(1, RuntimeException("fail"))

        assertTrue(job2.isCancelled)
        assertTrue(job3.isCancelled)
        assertTrue(job4.isCancelled)
        assertEquals(listOf(1), store.keys())
    }

    @Test
    fun onKeyFailed_whenFailedKeyIsLast_doesNotCancelAnyKeys() {
        val job1 = Job()
        store.getOrPut(1) { PageKeyRecord(1, job = job1) }
        store.getOrPut(2) { PageKeyRecord(2, job = Job()) }

        store.onKeyFailed(2, RuntimeException("fail"))

        assertFalse(job1.isCancelled)
        assertEquals(listOf(1, 2), store.keys())
    }

    @Test
    fun onKeyFailed_whenOnlyOneKey_doesNotCancelAnything() {
        store.getOrPut(1) { PageKeyRecord(1, job = Job()) }

        store.onKeyFailed(1, RuntimeException("fail"))

        assertEquals(listOf(1), store.keys())
    }

    @Test
    fun onKeyFailed_decrementsCounterForCancelledKeys() = runTest {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.getOrPut(3) { PageKeyRecord(3) }

        // key 1 fails, keys 2 and 3 are cancelled (counter 3 -> 1)
        store.onKeyFailed(1, RuntimeException("fail"))
        // remaining counter: only key 1 = need 1 completion
        store.onKeyCompleted(1) // counter -> 0, unlocks mutex

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            store.await()
        }
        assertTrue(job.isCompleted)
    }

    @Test
    fun shutdown_clearsAllRecords() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }

        store.shutdown()

        assertEquals(emptyList<Int>(), store.keys())
        assertTrue(store.getAllContainers().isEmpty())
    }

    @Test
    fun shutdown_unlocksAwaitMutex() = runTest {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }

        store.shutdown()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            store.await()
        }
        assertTrue(job.isCompleted)
    }

    @Test
    fun shutdown_preventsNewRecordsFromBeingAdded() {
        store.getOrPut(1) { PageKeyRecord(1) }

        store.shutdown()

        val result = store.getOrPut(2) { PageKeyRecord(2) }
        assertNull(result)
        assertTrue(store.keys().isEmpty())
    }

    @Test
    fun getOrPut_afterShutdown_returnsNull() {
        store.shutdown()

        val result = store.getOrPut(1) { PageKeyRecord(1) }

        assertNull(result)
    }

    @Test
    fun getOrPut_afterShutdown_doesNotAddRecord() {
        store.shutdown()

        store.getOrPut(1) { PageKeyRecord(1) }

        assertTrue(store.keys().isEmpty())
    }

    @Test
    fun getOrPut_beforeShutdown_returnsNonNull() {
        val result = store.getOrPut(1) { PageKeyRecord(1) }

        assertNotNull(result)
    }

    @Test
    fun findNextKeyForIndex_whenIndexIsLastOfFirstPage_andNextPageExists_returnsScheduleImmediateLoad() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.updateContainer(1, successContainer(listOf("a", "b")))
        store.updateContainer(2, successContainer(listOf("c")))

        assertEquals(NextKeyLoadResult.ScheduleImmediateLoad, store.findNextKeyForIndex(1))
    }

    @Test
    fun onKeyFailed_forNonExistentKey_doesNotCrash() {
        store.getOrPut(1) { PageKeyRecord(1) }

        // Should not throw
        store.onKeyFailed(999, RuntimeException("fail"))

        assertEquals(listOf(1), store.keys())
    }

    @Test
    fun onKeyContinued_forNonExistentKey_doesNotCrash() {
        // Should not throw
        store.onKeyContinued(999)

        assertTrue(store.keys().isEmpty())
    }

    @Test
    fun updateContainer_afterShutdown_doesNotCrash() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.shutdown()

        // Should not throw
        store.updateContainer(1, successContainer(listOf("a")))
    }

    @Test
    fun buildOutputList_withEmptySuccessContainers_returnsEmptyList() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.updateContainer(1, successContainer(emptyList()))

        assertEquals(emptyList<String>(), store.buildOutputList())
    }

    // 3 pages loaded (4 items each, indices 0-11), threshold=0.5:
    // getThresholdIndex(pageStartIndex=12, lastPageSize=4) = 12-4 + floor(0.5*4)=2 -> 10
    // So 4th page load triggers at index 10 (3rd item of page 3).

    @Test
    fun findNextKeyForIndex_multiplePages_pendingNextPage_whenIndexBelowThresholdOfLastPage_returnsSkip() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.getOrPut(3) { PageKeyRecord(3) }
        store.getOrPut(4) { PageKeyRecord(4) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        store.updateContainer(2, successContainer(listOf("e", "f", "g", "h")))
        store.updateContainer(3, successContainer(listOf("i", "j", "k", "l")))
        // key 4 stays pending

        // index 9 is the 2nd item of page 3, below thresholdIndex=10
        assertEquals(NextKeyLoadResult.Skip, store.findNextKeyForIndex(9))
    }

    @Test
    fun findNextKeyForIndex_multiplePages_pendingNextPage_whenIndexAtThresholdOfLastPage_returnsNextKey() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.getOrPut(3) { PageKeyRecord(3) }
        store.getOrPut(4) { PageKeyRecord(4) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        store.updateContainer(2, successContainer(listOf("e", "f", "g", "h")))
        store.updateContainer(3, successContainer(listOf("i", "j", "k", "l")))
        // key 4 stays pending

        // index 10 is the 3rd item of page 3, at thresholdIndex=10
        assertEquals(NextKeyLoadResult.Key(4), store.findNextKeyForIndex(10))
    }

    @Test
    fun findNextKeyForIndex_multiplePages_pendingNextPage_whenIndexInEarlierPage_returnsSkip() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.getOrPut(3) { PageKeyRecord(3) }
        store.getOrPut(4) { PageKeyRecord(4) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        store.updateContainer(2, successContainer(listOf("e", "f", "g", "h")))
        store.updateContainer(3, successContainer(listOf("i", "j", "k", "l")))
        // key 4 stays pending

        // index 5 is in page 2, which is before the last page — below thresholdIndex=10
        assertEquals(NextKeyLoadResult.Skip, store.findNextKeyForIndex(5))
    }

    @Test
    fun findNextKeyForIndex_multiplePages_noPendingPage_whenIndexBelowThresholdOfLastPage_returnsSkip() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.getOrPut(3) { PageKeyRecord(3) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        store.updateContainer(2, successContainer(listOf("e", "f", "g", "h")))
        store.updateContainer(3, successContainer(listOf("i", "j", "k", "l")))

        // index 9 is the 2nd item of page 3, below thresholdIndex=10
        assertEquals(NextKeyLoadResult.Skip, store.findNextKeyForIndex(9))
    }

    @Test
    fun findNextKeyForIndex_multiplePages_noPendingPage_whenIndexAtThresholdOfLastPage_returnsScheduleImmediateLoad() {
        store.getOrPut(1) { PageKeyRecord(1) }
        store.getOrPut(2) { PageKeyRecord(2) }
        store.getOrPut(3) { PageKeyRecord(3) }
        store.updateContainer(1, successContainer(listOf("a", "b", "c", "d")))
        store.updateContainer(2, successContainer(listOf("e", "f", "g", "h")))
        store.updateContainer(3, successContainer(listOf("i", "j", "k", "l")))

        // index 10 is the 3rd item of page 3, at thresholdIndex=10
        assertEquals(NextKeyLoadResult.ScheduleImmediateLoad, store.findNextKeyForIndex(10))
    }

    @Test
    fun intercept_withSuccessContainer_returnsDifferentInstance() {
        val input = successContainer(listOf("a", "b"))

        val output = store.intercept(input)

        assertNotSame(input, output)
    }

    @Test
    fun intercept_withSuccessContainer_replacesOutputList() {
        val input = successContainer(listOf("a", "b"))

        store.intercept(input)

        assertEquals(input.value, store.buildOutputList())
    }

    @Test
    fun intercept_withSuccessContainer_containsInputValue() {
        val input = successContainer(listOf("a", "b"))

        val output = store.intercept(input)

        assertEquals(listOf("a", "b"), output.getOrNull())
    }

    @Test
    fun intercept_withPendingContainer_returnsSameInstance() {
        val input = pendingContainer()

        val output = store.intercept(input)

        assertSame(input, output)
    }

    @Test
    fun intercept_withErrorContainer_returnsSameInstance() {
        val input = errorContainer(RuntimeException("error"))

        val output = store.intercept(input)

        assertSame(input, output)
    }

}
