@file:OptIn(ExperimentalCoroutinesApi::class)

package com.elveum.container.subject

import com.elveum.container.utils.JobStatus
import com.elveum.container.utils.runFlowTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import org.junit.Assert.*
import org.junit.Test

class FlowSubjectImplTest {

    @Test
    fun onNext_sendsItemToFlow() = runFlowTest {
        val subject = FlowSubjectImpl<String>()

        val collectedItems = subject.flow().startCollectingToList()
        subject.onNext("111")
        subject.onNext("222")

        assertEquals(listOf("111", "222"), collectedItems)
    }

    @Test
    fun flow_holdsLatestValueSentViaNext() = runFlowTest {
        val subject = FlowSubjectImpl<String>()

        subject.onNext("111")
        subject.onNext("222")
        val collectedItems = subject.flow().startCollectingToList()
        subject.onNext("333")
        subject.onNext("444")

        assertEquals(listOf("222", "333", "444"), collectedItems)
    }

    @Test
    fun flow_afterCompletion_holdsLatestValue() = runFlowTest {
        val subject = FlowSubjectImpl<String>()
        subject.onNext("111")
        subject.onNext("222")
        subject.onComplete()

        val collectedState = subject.flow().startCollecting()

        assertEquals(listOf("222"), collectedState.collectedItems)
        assertEquals(JobStatus.Completed, collectedState.jobStatus)
    }

    @Test
    fun flow_afterError_doesNotHoldLatestValue() = runFlowTest {
        val subject = FlowSubjectImpl<String>()
        subject.onNext("111")
        subject.onNext("222")
        subject.onError(IllegalStateException())

        val collectedState = subject.flow().startCollecting()

        assertEquals(emptyList<String>(), collectedState.collectedItems)
        assertTrue(collectedState.jobStatus is JobStatus.Failed)
    }

    @Test
    fun flow_isActiveByDefault() = runFlowTest {
        val subject = FlowSubjectImpl<String>()
        var completedInOperator = false
        var errorInOperator = false

        val state = subject.flow()
            .onCompletion {
                completedInOperator = true
            }
            .catch {
                errorInOperator = true
                throw it
            }
            .startCollecting()

        assertFalse(completedInOperator)
        assertFalse(errorInOperator)
        assertEquals(JobStatus.Collecting, state.jobStatus)
    }

    @Test
    fun onComplete_closesFlowWithSuccess() = runFlowTest {
        val subject = FlowSubjectImpl<String>()
        var completedInOperator = false

        val state = subject.flow()
            .onCompletion {
                completedInOperator = true
            }
            .startCollecting()
        subject.onNext("111")
        subject.onComplete()
        subject.onNext("222")

        assertEquals(listOf("111"), state.collectedItems)
        assertTrue(completedInOperator)
        assertEquals(JobStatus.Completed, state.jobStatus)
    }

    @Test
    fun onError_closesFlowWithException() = runFlowTest {
        val subject = FlowSubjectImpl<String>()
        var caughtInOperatorException: Throwable? = null

        val state = subject.flow()
            .catch {
                caughtInOperatorException = it
                throw it
            }
            .startCollecting()
        subject.onNext("111")
        subject.onError(CustomException("test"))
        subject.onNext("222")

        assertEquals(listOf("111"), state.collectedItems)
        val jobStatus = state.jobStatus
        assertTrue(
            jobStatus is JobStatus.Failed
                    && (jobStatus.error as CustomException).data == "test"
        )
        caughtInOperatorException.let {
            assertTrue(it is CustomException && it.data == "test")
        }
    }

    @Test(expected = Test.None::class)
    fun onComplete_takesEffectOnlyOnceAndDoesNotThrowExceptionAtSecondCall() = runFlowTest {
        val subject = FlowSubjectImpl<String>()

        val state = subject.flow().startCollecting()
        subject.onComplete()
        subject.onComplete()

        assertEquals(JobStatus.Completed, state.jobStatus)
    }

    @Test(expected = Test.None::class)
    fun onError_takesEffectOnlyOnceAndDoesNotThrowExceptionAtSecondCall() = runFlowTest {
        val subject = FlowSubjectImpl<String>()
        val exception1 = CustomException("111")
        val exception2 = CustomException("222")

        val state = subject.flow().startCollecting()
        subject.onError(exception1)
        subject.onError(exception2)

        val jobStatus = state.jobStatus as JobStatus.Failed
        val exception = jobStatus.error as CustomException
        assertEquals("111", exception.data)
    }

    private class CustomException(val data: String) : Exception()

}