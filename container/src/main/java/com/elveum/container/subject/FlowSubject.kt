package com.elveum.container.subject

import kotlinx.coroutines.flow.Flow


/**
 * Represents a finite flow which emissions can be controlled
 * by the following methods:
 * - [onNext]
 * - [onComplete]
 * - [onError]
 *
 * Use [flow] method to obtain the flow itself.
 */
interface FlowSubject<T> {

    /**
     * Emit a new value to the flow returned by [flow]
     */
    fun onNext(value: T)

    /**
     * Complete the flow returned by [flow] call with the provided exception.
     * Any further actions will not take effect.
     */
    fun onError(e: Throwable)

    /**
     * Complete the flow returned by [flow] call with success.
     * Any further actions will not take effect.
     */
    fun onComplete()

    /**
     * The flow controlled by this subject. It emits values sent via
     * [onNext] call. The flow can be finished manually by calling
     * [onError] or [onComplete] methods. Also
     * the flow holds the latest value sent by [onNext] so even if
     * the latest value has been sent before starting collecting
     * you can still receive it.
     */
    fun flow(): Flow<T>

    companion object {
        fun <T> create(): FlowSubject<T> {
            val defaultConfiguration = FlowSubjects.defaultConfiguration
            return defaultConfiguration.flowSubjectFactory.create()
        }
    }
}