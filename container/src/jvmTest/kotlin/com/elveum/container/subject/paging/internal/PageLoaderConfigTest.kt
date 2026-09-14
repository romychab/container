package com.elveum.container.subject.paging.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class PageLoaderConfigTest {

    @Test
    fun finalFetchDistance_isOne_whenFetchDistanceIsZero() {
        val config = createConfig(fetchDistance = 0)

        assertEquals(1, config.finalFetchDistance)
    }

    @Test
    fun finalFetchDistance_isOne_whenFetchDistanceIsNegative() {
        val config = createConfig(fetchDistance = -5)

        assertEquals(1, config.finalFetchDistance)
    }

    @Test
    fun finalFetchDistance_returnsOriginalValue_whenFetchDistanceIsOne() {
        val config = createConfig(fetchDistance = 1)

        assertEquals(1, config.finalFetchDistance)
    }

    @Test
    fun finalFetchDistance_returnsOriginalValue_whenFetchDistanceIsGreaterThanOne() {
        val config = createConfig(fetchDistance = 7)

        assertEquals(7, config.finalFetchDistance)
    }

    private fun createConfig(fetchDistance: Int) = PageLoaderConfig<Int, String>(
        initialKey = 0,
        fetchDistance = fetchDistance,
        emitMetadata = false,
        itemId = { it },
        block = { },
    )

}
