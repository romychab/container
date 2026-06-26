package com.elveum.store.load

import com.elveum.store.stores.base.BaseStore
import com.elveum.store.stores.keyed.KeyedStore
import kotlinx.coroutines.flow.Flow
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/**
 * Convert any nullable value into [Optional].
 * - `null` is mapped to an empty optional
 * - non-null values are mapped to non-empty optional
 */
public fun <T : Any> T?.toOptional(): Optional<T> {
    return if (this == null) Optional.empty() else Optional.of(this)
}

/**
 * Await and return the first non-empty successful result ot throw exception.
 */
public suspend fun <T : Any> Flow<StoreResult<Optional<T>>>.firstOptionalGetOrThrow(): T {
    return firstGetOrThrow().get()
}

/**
 * Try to extract non-empty optional value if it exists, or return `null`.
 */
public fun <T : Any> StoreResult<Optional<T>>.getOptionalValueOrNull(): T? {
    return this.getOrNull()?.getOrNull()
}

/**
 * The most actual result in the `Optional`-based store.
 */
public fun <T : Any> BaseStore<Optional<T>>.getOptionalValueOrNull(): T? {
    return get().getOptionalValueOrNull()
}

/**
 * The most actual result in the `Optional`-based keyed store.
 */
public fun <Key : Any, T : Any> KeyedStore<Key, Optional<T>>.getOptionalValueOrNull(key: Key): T? {
    return get(key).getOptionalValueOrNull()
}

/**
 * Whether the `Optional`-based store contains Empty value.
 */
public fun <T : Any> BaseStore<Optional<T>>.isOptionalEmpty(): Boolean {
    val optional = get().getOrNull() ?: return false
    return optional.getOrNull() == null
}

/**
 * Whether the `Optional`-based keyed store contains Empty value.
 */
public fun <Key : Any, T : Any> KeyedStore<Key, Optional<T>>.isOptionalEmpty(key: Key): Boolean {
    val optional = get(key).getOrNull() ?: return false
    return optional.getOrNull() == null
}
