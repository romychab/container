package com.elveum.container

import kotlinx.coroutines.flow.Flow

/**
 * Interface for emitting new loaded items.
 */
public interface Emitter<T> {

    /**
     * Input metadata of the current load. It may contain the load trigger,
     * or custom user request input values, etc.
     */
    public val metadata: ContainerMetadata

    /**
     * Reason why a loader function has been executed.
     */
    public val loadTrigger: LoadTrigger get() = metadata.loadTrigger

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
        source: SourceType,
        isLastValue: Boolean = false,
    ): Unit = emit(value, SourceTypeMetadata(source), isLastValue)

    /**
     * Emit a new value to the flow.
     *
     * @param value The value to emit.
     * @param metadata Additional metadata to be attached to the emitted value.
     *   This metadata is added in addition to metadata located in [Emitter.metadata].
     * @param isLastValue Optional indicator that you are going to emit the last
     *                   value; may be used to increase performance
     */
    public suspend fun emit(
        value: T,
        metadata: ContainerMetadata = EmptyMetadata,
        isLastValue: Boolean = false,
    )

    /**
     * An external flow which the loader function depends on.
     *
     * Whenever the [flow] emits a new value, a loader function is re-executed.
     *
     * Please note, you must supply one or more unique keys representing
     * arguments and the returned flow itself. For example:
     *
     * ```
     * val user: User = dependsOnFlow("getUserById", userId) {
     *     usersFetcher.getUserById(userId)
     * }
     * ```
     */
    public suspend fun <R> dependsOnFlow(
        key: Any,
        vararg keys: Any,
        flow: () -> Flow<R>,
    ): R

    /**
     * An external container flow which the loader function depends on.
     *
     * Whenever the [flow] emits a new success container, the loader
     * function is re-executed. In case of error container, the loader function
     * fails with the exception encapsulated into the error container.
     *
     * Please note, you must supply one or more unique keys representing
     * arguments and the returned flow itself. For example:
     *
     * ```
     * val user: User = dependsOnContainerFlow("getUserById", userId) {
     *     usersFetcher.getUserById(userId)
     * }
     * ```
     */
    public suspend fun <R> dependsOnContainerFlow(
        key: Any,
        vararg keys: Any,
        flow: () -> Flow<Container<R>>,
    ): R

}
