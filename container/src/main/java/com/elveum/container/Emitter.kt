package com.elveum.container

/**
 * Interface for emitting new loaded items.
 */
public interface Emitter<T> {

    /**
     * Reason why a loader function has been executed.
     */
    public val loadTrigger: LoadTrigger

    /**
     * Emit a new value to the flow.
     *
     * @param value The value to emit.
     * @param source The source of the item, defaults to [UnknownSourceType].
     * @param isLastValue Optional indicator that you are going to emit the last
     *                   value; may be used to increase performance
     */
    public suspend fun emit(
        value: T,
        source: SourceType = UnknownSourceType,
        isLastValue: Boolean = false,
    )

}
