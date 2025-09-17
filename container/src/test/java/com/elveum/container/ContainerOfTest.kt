package com.elveum.container

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ContainerOfTest {

    @Test
    fun containerOf_forCompletedBlock_returnsSuccessContainer() {
        val reloadFunction = mockk<ReloadFunction>()
        val container = containerOf(
            source = LocalSourceType,
            isLoadingInBackground = true,
            reloadFunction = reloadFunction,
        ) { "hello" }

        assertEquals(
            successContainer("hello", LocalSourceType, true, reloadFunction),
            container,
        )
    }

    @Test
    fun containerOf_forFailedBlock_returnsErrorContainer() {
        val exception = IllegalStateException()
        val reloadFunction = mockk<ReloadFunction>()
        val container = containerOf(
            source = RemoteSourceType,
            isLoadingInBackground = true,
            reloadFunction = reloadFunction,
        ) { throw exception }

        assertEquals(
            errorContainer(exception, RemoteSourceType, true, reloadFunction),
            container,
        )
    }

}
