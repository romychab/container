package com.elveum.container

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContainerTest {

    @Test
    fun map_forPendingContainerAndNonEmptyMapper_returnsSameInstance() = runTest {
        val inputContainer: Container<String> = Container.Pending
        val outputContainer: Container<Int> = inputContainer.map { 1 }
        assertSame(inputContainer, outputContainer)
    }

    @Test
    fun map_forErrorContainerAndNonEmptyMapper_returnsErrorInstance() = runTest {
        val inputContainer: Container<String> = Container.Error(IllegalStateException())
        val outputContainer: Container<Int> = inputContainer.map { 1 }
        assertEquals(inputContainer, outputContainer)
    }

    @Test
    fun map_forSuccessContainerWithMapper_mapsValue() = runTest {
        val inputContainer = Container.Success("123")

        val outputContainer = inputContainer.map { it.toInt() }

        assertEquals(123, (outputContainer as Container.Success).value)
    }

    @Test
    fun map_forSuccessContainer_retainsSourceIndicator() = runTest {
        val inputContainer = Container.Success(
            value = "123",
            source = LocalSourceType
        )

        val outputContainer = inputContainer.map { it.toInt() }

        assertEquals(LocalSourceType, (outputContainer as Container.Success).source)
    }

    @Test
    fun new_successContainer_usesUnknownSourceIndicator() {
        assertEquals(UnknownSourceType, Container.Success("123").source)
    }

}