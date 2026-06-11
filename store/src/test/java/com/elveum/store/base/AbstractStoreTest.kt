package com.elveum.store.base

import com.elveum.container.factory.CoroutineScopeFactory
import com.elveum.store.load.StoreResult
import com.uandcode.flowtest.FlowTestScope
import com.uandcode.flowtest.runFlowTest
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Before

abstract class AbstractStoreTest {

    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        testScope = TestScope()
    }

    protected fun <T> assertResult(
        expected: StoreResult<T>,
        actual: StoreResult<T>,
    ) {
        val actualWithoutMetadata = when (actual) {
            is StoreResult.Failed -> StoreResult.Failed(actual.exception)
            is StoreResult.Loaded -> StoreResult.Loaded(actual.value)
            StoreResult.Loading -> StoreResult.Loading
        }
        assertEquals(expected, actualWithoutMetadata)
    }

    protected fun runFlowTest(block: suspend FlowTestScope.() -> Unit) = testScope.runFlowTest(block)

    protected fun createStoreScopeFactory() = CoroutineScopeFactory {
        TestScope(testScope.testScheduler)
    }

}