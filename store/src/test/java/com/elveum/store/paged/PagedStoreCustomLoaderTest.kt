package com.elveum.store.paged

import com.elveum.store.load.StoreResult
import org.junit.Test

class PagedStoreCustomLoaderTest : AbstractPagedStoreTest() {

    @Test
    fun `GIVEN paged custom loader WHEN observe THEN the emitted page is exposed`() = runFlowTest {
        val store = storeBuilder().buildCustom { pageKey ->
            emitPage(listOf("item-$pageKey-a", "item-$pageKey-b"))
        }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("item-0-a", "item-0-b")), collector.lastItem)
    }

    @Test
    fun `GIVEN paged custom loader with next key WHEN onItemRendered THEN the next page is loaded`() = runFlowTest {
        val store = storeBuilder().buildCustom { pageKey ->
            emitPage(listOf("item${pageKey * 2}", "item${pageKey * 2 + 1}"))
            if (pageKey < 1) emitNextKey(pageKey + 1)
        }
        val collector = store.observe().startCollecting()
        runCurrent()

        store.onItemRendered(1) // render the last item of the first page
        runCurrent()

        assertResult(
            StoreResult.Loaded(listOf("item0", "item1", "item2", "item3")),
            collector.lastItem,
        )
    }

    @Test
    fun `GIVEN paged query custom loader WHEN observe THEN the query reaches the loader`() = runFlowTest {
        val store = storeBuilder()
            .withQuery(initialQuery = "q1")
            .buildCustom { query, pageKey -> emitPage(listOf("$query-item$pageKey")) }

        val collector = store.observe().startCollecting()
        runCurrent()

        assertResult(StoreResult.Loaded(listOf("q1-item0")), collector.lastItem)
    }
}
