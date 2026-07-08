package com.elveum.container.subject.paging.internal

import com.elveum.container.Container
import com.elveum.container.EmptyMetadata
import com.elveum.container.LoadConfig
import com.elveum.container.errorContainer
import com.elveum.container.pendingContainer
import com.elveum.container.subject.paging.TotalPagedItemsCountMetadata
import com.elveum.container.successContainer
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PageContextTest {

    @RelaxedMockK
    private lateinit var state: PageRecordsState<Int, String>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun pageIndex_reflectsInitialRecord() = runTest {
        val context = createContext(pageIndex = 3, pageKey = 0)
        assertEquals(3, context.pageIndex)
    }

    @Test
    fun pageKey_reflectsInitialRecord() = runTest {
        val context = createContext(pageIndex = 0, pageKey = 99)
        assertEquals(99, context.pageKey)
    }

    @Test
    fun isRetry_reflectsConstructorParameter() = runTest {
        val context = createContext(isRetry = true)
        assertTrue(context.isRetry)
    }

    @Test
    fun isRetry_isFalse_whenConstructedWithFalse() = runTest {
        val context = createContext(isRetry = false)
        assertFalse(context.isRetry)
    }

    @Test
    fun onLoadStarted_withIsRetryFalse_doesNotResetExistingContainer() = runTest {
        val context = createContext(
            pageIndex = 0,
            pageKey = 1,
        )
        val inputContainer = successContainer(listOf("a"))

        context.onLoadStarted(isRetry = false)

        val slot = containerSlot()
        coVerify(exactly = 1) {
            state.updateRecord(
                pageIndex = 0,
                pageKey = 1,
                container = capture(slot),
                isRetry = false,
            )
        }
        val outputContainer = slot.captured(inputContainer)
        assertSame(inputContainer, outputContainer)
    }

    @Test
    fun onLoadStarted_withIsRetryTrue_resetsContainerToPending() = runTest {
        val context = createContext(
            pageIndex = 0,
            pageKey = 1,
        )
        val inputContainer = successContainer(listOf("a"))

        context.onLoadStarted(isRetry = true)

        val slot = containerSlot()
        coVerify(exactly = 1) {
            state.updateRecord(
                pageIndex = 0,
                pageKey = 1,
                container = capture(slot),
                isRetry = true,
            )
        }
        val outputContainer = slot.captured(inputContainer)
        assertSame(pendingContainer(), outputContainer)
    }

    @Test
    fun onPageDataLoaded_updatesStateWithSuccessContainer() = runTest {
        val context = createContext(
            pageIndex = 0,
            pageKey = 1,
        )

        context.onPageDataLoaded(listOf("x", "y"), EmptyMetadata)

        val slot = containerSlot()
        coVerify(exactly = 1) {
            state.updateRecord(
                pageIndex = 0,
                pageKey = 1,
                container = capture(slot)
            )
        }
        val outputContainer = slot.captured.invoke(pendingContainer())
        assertEquals(successContainer(listOf("x", "y")), outputContainer)
    }

    @Test
    fun onPageDataLoaded_withEmptyList_updatesState() = runTest {
        val context = createContext(
            pageIndex = 0,
            pageKey = 1,
        )

        context.onPageDataLoaded(emptyList(), EmptyMetadata)

        val slot = containerSlot()
        coVerify(exactly = 1) {
            state.updateRecord(
                pageIndex = 0,
                pageKey = 1,
                container = capture(slot)
            )
        }
        val outputContainer = slot.captured.invoke(pendingContainer())
        assertEquals(successContainer(emptyList<String>()), outputContainer)
    }

    @Test
    fun onPageDataLoaded_attachesProvidedMetadataToSuccessContainer() = runTest {
        val context = createContext(
            pageIndex = 0,
            pageKey = 1,
        )
        val metadata = TotalPagedItemsCountMetadata(totalPagedItemsCount = 42)

        context.onPageDataLoaded(listOf("x", "y"), metadata)

        val slot = containerSlot()
        coVerify(exactly = 1) {
            state.updateRecord(
                pageIndex = 0,
                pageKey = 1,
                container = capture(slot)
            )
        }
        val outputContainer = slot.captured.invoke(pendingContainer())
        assertEquals(successContainer(listOf("x", "y"), metadata), outputContainer)
    }

    @Test
    fun onLoadFailed_updatesStateWithErrorContainer() = runTest {
        val exception = RuntimeException("test error")
        val context = createContext(
            pageIndex = 0,
            pageKey = 1,
        )

        context.onLoadFailed(exception)

        val slot = containerSlot()
        coVerify(exactly = 1) {
            state.updateRecord(
                pageIndex = 0,
                pageKey = 1,
                container = capture(slot)
            )
        }
        val outputContainer = slot.captured.invoke(pendingContainer())
        assertEquals(errorContainer(exception), outputContainer)
    }

    @Test
    fun onLoadCompleted_marksPageAsCompleted() = runTest {
        val context = createContext(pageIndex = 0, pageKey = 1)

        context.onLoadCompleted()

        coVerify(exactly = 1) {
            state.markAsCompleted(pageIndex = 0, pageKey = 1)
        }
    }

    @Test
    fun isAllPagesCompleted_delegatesToState() = runTest {
        val context = createContext()
        coEvery { state.isAllPagesLoaded() } returns false andThen true

        val result1 = context.isAllPagesCompleted()
        val result2 = context.isAllPagesCompleted()

        assertFalse(result1)
        assertTrue(result2)
    }

    @Test
    fun scheduleNextKey_callsLambda() = runTest {
        val lambda = mockk<suspend (Int, Int) -> Unit>(relaxed = true)
        val context = createContext(
            pageIndex = 3,
            onScheduleNextKey = lambda,
        )

        context.scheduleNextKey(key = 10)

        coVerify(exactly = 1) {
            lambda(
                /* pageIndex = */ 4, // 3 + 1 (incremented)
                /* key = */ 10,
            )
        }
    }

    @Test
    fun observePageSnapshot_delegatesToState() = runTest {
        val expectedFlow = MutableSharedFlow<List<String>>()
        every { state.observePageSnapshots(pageIndex = 2) } returns expectedFlow
        val context = createContext()

        val flow = context.observePageSnapshot(pageIndex = 2)

        assertSame(expectedFlow, flow)
    }

    @Test
    fun observeListSnapshots_delegatesToState() = runTest {
        val expectedFlow = MutableStateFlow<List<String>>(emptyList())
        every { state.observeListSnapshots() } returns expectedFlow
        val context = createContext()

        val flow = context.observeListSnapshots()

        assertSame(expectedFlow, flow)
    }

    private fun containerSlot() = slot<(Container<List<String>>) -> Container<List<String>>>()

    private fun createContext(
        pageIndex: Int = 0,
        pageKey: Int = 0,
        priority: Long = 0,
        onScheduleNextKey: suspend (Int, Int) -> Unit = { _, _ -> },
        container: Container<List<String>> = pendingContainer(),
        isRetry: Boolean = false,
        isCompleted: Boolean = false,
    ): PageContext<Int, String> {
        val record = ImmutablePageRecord(pageIndex, pageKey, priority, container, isCompleted)
        return PageContext(state, LoadConfig.Normal, isRetry, onScheduleNextKey, record)
    }

}
