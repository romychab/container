package com.elveum.container

interface ContainerMapperScope {
    val source: SourceIndicator
}

typealias ContainerMapper<T, R> = suspend ContainerMapperScope.(T) -> R


/**
 * Represents the current status of async fetch/operation.
 * @see Container.Pending
 * @see Container.Error
 * @see Container.Success
 */
sealed class Container<out T> {

    /**
     * Convert the container type to another type by using a suspend lambda.
     * @throws IllegalStateException if the container is [Success] and [mapper] is not provided
     */
    abstract suspend fun <R> suspendMap(mapper: ContainerMapper<T, R>? = null): Container<R>

    /**
     * The operation is still in progress.
     */
    object Pending : Container<Nothing>() {

        override suspend fun <R> suspendMap(mapper: ContainerMapper<Nothing, R>?): Container<R> {
            return this
        }

        override fun toString(): String {
            return "Pending"
        }
    }

    /**
     * The operation has been failed.
     */
    data class Error(
        val exception: Throwable,
    ) : Container<Nothing>() {

        override suspend fun <R> suspendMap(mapper: ContainerMapper<Nothing, R>?): Container<R> {
            return this
        }

        override fun toString(): String {
            return "Error(${exception::class.java.simpleName})"
        }
    }

    /**
     * The operation has been finished with success.
     */
    data class Success<T>(
        val value: T,
        override val source: SourceIndicator = UnknownSourceIndicator,
    ) : Container<T>(), ContainerMapperScope {

        override suspend fun <R> suspendMap(mapper: ContainerMapper<T, R>?): Container<R> {
            if (mapper == null) throw IllegalStateException("Can't map Container.Success without mapper")
            return try {
                Success(mapper(value), source)
            } catch (e: Exception) {
                Error(e)
            }
        }

        override fun toString(): String {
            return "Success($value)"
        }
    }

}