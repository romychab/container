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
public interface FlowSubject<T> {

    /**
     * Emit a new value to the flow returned by [flow]
     */
    public fun onNext(value: T)

    /**
     * Complete the flow returned by [flow] call with the provided exception.
     * Any further actions will not take effect.
     */
    public fun onError(e: Exception)

    /**
     * Complete the flow returned by [flow] call with success.
     * Any further actions will not take effect.
     */
    public fun onComplete()

    /**
     * The flow controlled by this subject. It emits values sent via
     * [onNext] call. The flow can be finished manually by calling
     * [onError] or [onComplete] methods. Also
     * the flow holds the latest value sent by [onNext] so even if
     * the latest value has been sent before starting collecting
     * you can still receive it.
     */
    public fun flow(): Flow<T>

    public companion object {
        /**
         * Create a new instance of default implementation of [FlowSubject].
         */
        public fun <T> create(): FlowSubject<T> {
            return FlowSubjectImpl()
        }
    }
}
