package com.elveum.container.subject.paging

import com.elveum.container.Container
import com.elveum.container.successContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagingAccessorsTest {

    private fun pagedContainer(
        state: PageState = PageState.Idle,
        total: Int? = null,
        onRendered: ((Int) -> Unit)? = null,
    ): Container<List<String>> {
        var metadata = NextPageStateMetadata(state) as com.elveum.container.ContainerMetadata
        if (total != null) metadata += TotalPagedItemsCountMetadata(total)
        if (onRendered != null) metadata += OnItemRenderedCallbackMetadata(onRendered)
        return successContainer(listOf("a", "b"), metadata = metadata)
    }

    @Test
    fun nextPageState_readableFromContainerDirectly() {
        val container = pagedContainer(state = PageState.Pending)
        assertEquals(PageState.Pending, container.nextPageState)
        assertEquals(container.metadata.nextPageState, container.nextPageState)
    }

    @Test
    fun nextPageState_defaultsToIdleWithoutMetadata() {
        assertEquals(PageState.Idle, successContainer(listOf("a")).nextPageState)
    }

    @Test
    fun totalPagedItemsCount_readableFromContainerDirectly() {
        assertEquals(42, pagedContainer(total = 42).totalPagedItemsCount)
        assertEquals(-1, successContainer(listOf("a")).totalPagedItemsCount)
    }

    @Test
    fun onItemRendered_forwardsIndexFromContainerDirectly() {
        val seen = mutableListOf<Int>()
        pagedContainer(onRendered = { seen += it }).onItemRendered(7)
        assertEquals(listOf(7), seen)
    }

    @Test
    fun accessorsAreAvailableInsideFoldBranch() {
        val seen = mutableListOf<Int>()
        val container = pagedContainer(
            state = PageState.Pending,
            total = 9,
            onRendered = { seen += it },
        )

        // inside fold the receiver is ContainerMapperScope: no `metadata.` prefix
        val rendered: String = container.fold(
            onPending = { "pending" },
            onError = { "error" },
            onSuccess = { items ->
                onItemRendered(1)
                "${items.size}/$totalPagedItemsCount/$nextPageState"
            },
        )

        assertEquals("2/9/Pending", rendered)
        assertEquals(listOf(1), seen)
    }

    @Test
    fun accessorsResolveUnambiguouslyOnCompletedContainers() {
        // Container.Success is BOTH a Container and a ContainerMapperScope.
        // The scope members must win over the Container extensions rather than
        // producing an overload-resolution ambiguity.
        val success: Container.Success<List<String>> =
            pagedContainer(state = PageState.Pending, total = 4) as Container.Success

        assertEquals(PageState.Pending, success.nextPageState)
        assertEquals(4, success.totalPagedItemsCount)
        success.onItemRendered(0)

        // and the same values are reachable through the Container extensions
        val asContainer: Container<List<String>> = success
        assertEquals(PageState.Pending, asContainer.nextPageState)
        assertEquals(4, asContainer.totalPagedItemsCount)
    }

    @Test
    fun onItemRendered_isNoOpWhenCallbackAbsent() {
        successContainer(listOf("a")).onItemRendered(3)   // must not throw
        assertTrue(true)
    }
}
