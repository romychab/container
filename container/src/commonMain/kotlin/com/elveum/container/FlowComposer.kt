package com.elveum.container

import kotlinx.coroutines.flow.Flow

public interface FlowComposer {

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

    /**
     * Optional reload configuration that can be used as a key
     * in [dependsOnFlow] and [dependsOnContainerFlow] calls. It defines
     * how the reloading is processed (e.g. whether the Loading state
     * is emitted to consumers, etc.)
     *
     * @property loadConfig if set to `null` - the latest load config hosted by subject is used.
     * @property reloadDependencies whether all other dependencies are reloaded too.
     */
    public data class Config(
        val loadConfig: LoadConfig? = null,
        val reloadDependencies: Boolean = false,
    )

}
