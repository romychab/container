package com.elveum.container

public interface ContainerMapperScope {
    public val source: SourceType
}

public typealias ContainerMapper<T, R> = ContainerMapperScope.(T) -> R


/**
 * Represents the current status of async fetch/operation.
 * @see Container.Pending
 * @see Container.Error
 * @see Container.Success
 */
public sealed class Container<out T> {

    /**
     * The operation is still in progress.
     */
    public data object Pending : Container<Nothing>() {
        override fun toString(): String {
            return "Pending"
        }
    }

    /**
     * The operation has been failed.
     */
    public data class Error(
        val exception: Throwable,
    ) : Container<Nothing>() {
        override fun toString(): String {
            return "Error(${exception::class.java.simpleName})"
        }
    }

    /**
     * The operation has been finished with success.
     */
    public data class Success<T>(
        val value: T,
        override val source: SourceType = UnknownSourceType,
    ) : Container<T>(), ContainerMapperScope {
        override fun toString(): String {
            return "Success($value)"
        }
    }

}

/**
 * Convert the container type to another type by using a lambda [mapper].
 */
public inline fun <T, R> Container<T>.map(
    mapper: ContainerMapper<T, R>,
): Container<R> {
    return when (this) {
        Container.Pending -> Container.Pending
        is Container.Error -> this
        is Container.Success -> {
            try {
                Container.Success(mapper(this, this.value), source)
            } catch (e: Exception) {
                Container.Error(e)
            }
        }
    }
}

public fun <T, R> Container<T>.map(): Container<R> {
    return when (this) {
        Container.Pending -> Container.Pending
        is Container.Error -> this
        is Container.Success ->
            throw IllegalStateException("Can't map Container.Success without mapper() lambda")
    }
}

