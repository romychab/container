package com.elveum.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

private data class Product(val id: Long, val name: String, val favorite: Boolean = false)

class ListItemExtensionsTest {

    private val list = listOf(
        Product(1, "one"),
        Product(2, "two"),
        Product(3, "three"),
    )

    @Test
    fun updateItem_replacesMatchingItemByIdentity() {
        val result = list.updateItem(Product(2, "TWO")) { it.id }

        assertEquals(listOf(Product(1, "one"), Product(2, "TWO"), Product(3, "three")), result)
    }

    @Test
    fun updateItem_ignoresPositionAndUsesId() {
        val reordered = list.reversed()
        val result = reordered.updateItem(Product(1, "ONE")) { it.id }

        assertEquals("ONE", result.first { it.id == 1L }.name)
        assertEquals(3, result.size)
    }

    @Test
    fun updateItem_returnsSameListWhenIdAbsent() {
        val result = list.updateItem(Product(99, "nope")) { it.id }

        assertSame(list, result)
    }

    @Test
    fun updateItem_doesNotMutateTheOriginal() {
        val result = list.updateItem(Product(2, "TWO")) { it.id }

        assertEquals("two", list[1].name)
        assertTrue(result !== list)
    }

    @Test
    fun updateItem_transformOverloadDerivesFromCurrentValue() {
        val result = list.updateItem(3L, { it.id }) { it.copy(favorite = true) }

        assertEquals(Product(3, "three", favorite = true), result[2])
    }

    @Test
    fun updateItem_transformIsNotCalledWhenIdAbsent() {
        var called = false
        val result = list.updateItem(99L, { it.id }) { called = true; it }

        assertSame(list, result)
        assertTrue(!called)
    }

    @Test
    fun containerUpdateItem_patchesSuccessValue() {
        val container: Container<List<Product>> = successContainer(list)

        val result = container.updateItem(Product(2, "TWO")) { it.id }

        assertEquals("TWO", result.unwrap()[1].name)
    }

    @Test
    fun containerUpdateItem_transformOverload() {
        val container: Container<List<Product>> = successContainer(list)

        val result = container.updateItem(1L, { it.id }) { it.copy(favorite = true) }

        assertTrue(result.unwrap().first().favorite)
    }

    @Test
    fun containerUpdateItem_leavesNonSuccessContainersUnchanged() {
        val pending: Container<List<Product>> = pendingContainer()
        val error: Container<List<Product>> = errorContainer(IllegalStateException("x"))

        assertTrue(pending.updateItem(Product(2, "TWO")) { it.id } is Container.Pending)
        assertTrue(error.updateItem(Product(2, "TWO")) { it.id } is Container.Error)
    }

    @Test
    fun updateItem_acceptsAPageLoaderStyleSelector() {
        // pageLoader takes `itemId: (T) -> Any`; the same lambda must work here
        val selector: (Product) -> Any = { it.id }

        val result = list.updateItem(Product(2, "TWO"), selector)

        assertEquals("TWO", result[1].name)
    }
}
