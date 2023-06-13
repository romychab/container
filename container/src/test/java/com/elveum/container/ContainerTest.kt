package com.elveum.container

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContainerTest {

    @Test
    fun suspendMap_forPendingContainerAndEmptyMapper_returnsSameInstance() = runTest {
        val inputContainer: Container<String> = Container.Pending
        val outputContainer: Container<Int> = inputContainer.suspendMap()
        assertSame(inputContainer, outputContainer)
    }

    @Test
    fun suspendMap_forPendingContainerAndNonEmptyMapper_returnsSameInstance() = runTest {
        val inputContainer: Container<String> = Container.Pending
        val outputContainer: Container<Int> = inputContainer.suspendMap { 1 }
        assertSame(inputContainer, outputContainer)
    }

    @Test
    fun suspendMap_forErrorContainerAndEmptyMapper_returnsSameInstance() = runTest {
        val inputContainer: Container<String> = Container.Error(IllegalStateException())
        val outputContainer: Container<Int> = inputContainer.suspendMap()
        assertSame(inputContainer, outputContainer)
    }

    @Test
    fun suspendMap_forErrorContainerAndNonEmptyMapper_returnsSameInstance() = runTest {
        val inputContainer: Container<String> = Container.Error(IllegalStateException())
        val outputContainer: Container<Int> = inputContainer.suspendMap { 1 }
        assertSame(inputContainer, outputContainer)
    }

    @Test(expected = IllegalStateException::class)
    fun suspendMap_forSuccessContainerWithoutMapper_throwsException() = runTest {
        val inputContainer: Container<String> = Container.Success("123")
        inputContainer.suspendMap<Int>()
    }

    @Test
    fun suspendMap_forSuccessContainerWithMapper_mapsValue() = runTest {
        val inputContainer = Container.Success("123")

        val outputContainer = inputContainer.suspendMap { it.toInt() }

        assertEquals(123, (outputContainer as Container.Success).value)
    }

    @Test
    fun suspendMap_forSuccessContainer_retainsSourceIndicator() = runTest {
        val inputContainer = Container.Success(
            value = "123",
            source = LocalSourceIndicator
        )

        val outputContainer = inputContainer.suspendMap { it.toInt() }

        assertEquals(LocalSourceIndicator, (outputContainer as Container.Success).source)
    }

    @Test
    fun new_successContainer_usesUnknownSourceIndicator() {
        assertEquals(UnknownSourceIndicator, Container.Success("123").source)
    }

}