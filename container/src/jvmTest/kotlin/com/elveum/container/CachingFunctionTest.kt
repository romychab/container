package com.elveum.container

import io.mockk.MockKAnnotations
import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CachingFunctionTest {

    private val input = "input"
    private val input1 = "input1"
    private val input2 = "input2"

    @MockK
    private lateinit var originFunction: (String) -> String

    private lateinit var cachingFunction: CachingFunction<String, String>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        cachingFunction = CachingFunction(originFunction)
        every { originFunction.invoke(any()) } answers {
            expectedOutput(firstArg())
        }
    }

    @Test
    fun originFunction_isNotCalledInitially() {
        verify {
            originFunction wasNot called
        }
    }

    @Test
    fun invoke_onFirstCall_executesOriginFunction() {
        cachingFunction.invoke(input)

        verify(exactly = 1) {
            originFunction.invoke(input)
        }
    }

    @Test
    fun invoke_returnsResultFromOriginFunction() {
        val output = cachingFunction.invoke(input)

        assertEquals(expectedOutput(input), output)
    }

    @Test
    fun invoke_onSecondCallWithSameInput_returnsCachedResult() {
        val output1 = cachingFunction.invoke(input)
        val output2 = cachingFunction.invoke(input)

        assertEquals(output1, output2)
        verify(exactly = 1) {
            originFunction.invoke(input)
        }
    }

    @Test
    fun invoke_onSecondCallWithOtherInput_executesOriginFunction() {
        cachingFunction.invoke(input1)

        val output2 = cachingFunction.invoke(input2)

        assertEquals(expectedOutput(input2), output2)
        verify(exactly = 1) {
            originFunction.invoke(input2)
        }
    }

    @Test
    fun invoke_cachesOnlyPreviousResult() {
        cachingFunction.invoke(input1)
        cachingFunction.invoke(input2)
        val output = cachingFunction.invoke(input1)

        assertEquals(expectedOutput(input1), output)
        verify(exactly = 2) {
            originFunction.invoke(input1)
        }
    }

    private fun expectedOutput(input: String): String {
        return "processed-$input"
    }

}