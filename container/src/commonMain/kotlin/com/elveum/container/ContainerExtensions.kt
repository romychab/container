@file:OptIn(ExperimentalContracts::class)

package com.elveum.container

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


/**
 * Update additional metadata in the container.
 */
public fun <T> Container<T>.update(
    block: ContainerUpdater.() -> Unit,
): Container<T> {
    return transform(
        onSuccess = { value ->
            with(applyUpdater(block)) {
                successContainer(value, metadata)
            }
        },
        onError = { exception ->
            with(applyUpdater(block)) {
                errorContainer(exception, metadata)
            }
        }
    )
}

public fun <T> Container<T>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is Container.Success<T>)
    }
    return this is Container.Success<T>
}

public fun <T> Container<T>.isCompleted(): Boolean {
    contract {
        returns(true) implies (this@isCompleted is Container.Completed<T>)
    }
    return this is Container.Completed<T>
}

public fun <T> Container<T>.isError(): Boolean {
    contract {
        returns(true) implies (this@isError is Container.Error)
    }
    return this is Container.Error
}

public fun <T> Container<T>.isPending(): Boolean {
    contract {
        returns(true) implies (this@isPending is Container.Pending)
    }
    return this is Container.Pending
}

public fun <T> Container<T>.isDataLoading(): Boolean = isPending() || backgroundLoadState == BackgroundLoadState.Loading

private fun ContainerMapperScope.applyUpdater(block: ContainerUpdater.() -> Unit): ContainerUpdater {
    val updater = ContainerUpdaterImpl(this)
    updater.apply(block)
    return updater
}
