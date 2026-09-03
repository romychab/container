# Container Type Reference

`Container<T>` is the core type of `com.elveum:container:3.6.0`. It represents
the current status of an async fetch/operation: still loading, failed, or
finished with a value. Every other building block in this library (subjects,
caches, reducers, paging) produces or consumes `Container<T>` values.

All symbols below live in package `com.elveum.container` unless noted
otherwise.

## 1. The three states

`Container<T>` is a sealed class with three concrete states:

```kotlin
sealed class Container<out T> {
    data object Pending : Container<Nothing>()

    sealed class Completed<out T> : Container<T>()

    data class Success<T> internal constructor(
        val value: T,
        override val metadata: ContainerMetadata,
    ) : Completed<T>()

    data class Error internal constructor(
        val exception: Exception,
        override val metadata: ContainerMetadata,
    ) : Completed<Nothing>()
}
```

- **`Container.Pending`** - an operation is in progress; no value is available yet.
- **`Container.Success<T>`** - the operation finished successfully; the loaded
  value is in `value`.
- **`Container.Error`** - the operation failed; the reason is in `exception`
  (a Kotlin `Exception`, not `Throwable`).
- **`Container.Completed<T>`** - abstract base for `Success` and `Error`; use
  it as a parameter type when you want to accept "any finished state" and
  reject `Pending` at compile time.

`Success` and `Error` both declare `internal constructor` - you cannot
instantiate them directly from application code (`Container.Success(v, m)`
will not compile outside the library's own module). Always go through the
top-level factory functions in section 2, or the transformation/catch
functions in section 5, which build new instances internally.

## 2. Creating containers

```kotlin
val pending: Container<Product> = pendingContainer()
val success: Container<Product> = successContainer(Product(1, "Book"))
val error: Container<Product> = errorContainer(IOException("no network"))
```

- `pendingContainer(): Container.Pending`
- `successContainer(value: T): Container.Success<T>`
- `errorContainer(exception: Exception): Container.Error`

For computed results, prefer `containerOf { }`, which runs a block and wraps
its outcome automatically - a normal return becomes `Container.Success`, a
thrown `Exception` becomes `Container.Error` (a thrown `CancellationException`
is rethrown, not captured):

```kotlin
private fun combining(a: Container<Int>, b: Container<Int>): Container<Int> {
    return containerOf { a.unwrap() + b.unwrap() }
}
```

The Flow equivalent is `containerFlowOf { }`: it emits `pendingContainer()`
immediately, then wraps every value emitted inside the block into
`successContainer(...)`, and wraps a thrown `Exception` into `errorContainer(...)`:

```kotlin
private fun getUserFlow(): Flow<Container<Product>> =
    containerFlowOf { emit(Product(1, "User")) }
```

`containerFlowOf`'s block runs with `FlowCollector<T>` as its receiver (`T` is
the *unwrapped* value type), so call `emit(value)` with plain values, not
containers.

## 3. Extracting values

| Function                        | Returns                      | Behaviour |
|----------------------------------|-------------------------------|-----------|
| `getOrNull()`                    | `T?`                          | Value if `Success`, otherwise `null` |
| `exceptionOrNull()`               | `Exception?`                  | Exception if `Error`, otherwise `null` |
| `unwrap()`                        | `T`                           | Value if `Success`; throws `LoadNotFinishedException` if `Pending`, or rethrows the wrapped exception if `Error` |
| `getContainerValueOrNull()`       | `ContainerValue<T>?`          | Value + metadata if `Success`, otherwise `null` |
| `getContainerExceptionOrNull()`   | `ContainerValue<Exception>?`  | Exception + metadata if `Error`, otherwise `null` |
| `unwrapContainerValue()`          | `ContainerValue<T>`           | Value + metadata if `Success`; throws like `unwrap()` otherwise |

```kotlin
val value: Product? = success.getOrNull()
val exception: Exception? = error.exceptionOrNull()
val unwrapped: Product = success.unwrap()
val containerValue = success.getContainerValueOrNull()
val containerException = error.getContainerExceptionOrNull()
val unwrappedContainerValue = success.unwrapContainerValue()
```

`ContainerValue<T>` is a small data class pairing `value: T` with
`metadata: ContainerMetadata`; it is what the `*ContainerValue*` functions
above return instead of the bare value, so metadata is not lost during
extraction.

## 4. Pattern matching

### `when`

```kotlin
val label: String = when (success) {
    is Container.Pending -> "loading"
    is Container.Error -> "failed"
    is Container.Success -> success.value.name
}
```

### `fold`

`fold` is the primary way to handle all three states exhaustively in one
expression:

```kotlin
val folded: String = error.fold(
    onPending = { "loading" },
    onError = { e -> e.message.orEmpty() },
    onSuccess = { value -> value.name },
)
```

Signature: `fold(onPending: () -> R, onError: (Exception) -> R, onSuccess: (T) -> R): R`
- argument order is always `onPending, onError, onSuccess`.
- `onError` and `onSuccess` run with a `ContainerMapperScope` receiver, which
  additionally exposes `sourceType`, `backgroundLoadState`, and `reload()`
  (see the paging/subjects references for how these are populated).

Two convenience variants exist for when you don't need to handle every
branch explicitly:

```kotlin
val defaulted: String = error.foldDefault(
    defaultValue = "-",
    onError = { e -> "Error: ${e.message}" },
    onSuccess = { value -> value.name },
)
val nullable: String? = success.foldNullable(
    onSuccess = { value -> value.name },
)
```

`foldDefault`/`foldNullable` fill in any callback you omit with
`defaultValue`/`null` respectively.

### `isXxx` extensions

- `isSuccess()` - smart-casts the receiver to `Container.Success<T>`
- `isError()` - smart-casts the receiver to `Container.Error`
- `isPending()` - smart-casts the receiver to `Container.Pending`
- `isCompleted()` - smart-casts the receiver to `Container.Completed<T>`

```kotlin
if (success.isSuccess()) {
    // success is smart-cast to Container.Success<Product> here
    check(success.value.name.isNotEmpty())
}
check(error.isError())
check(pending.isPending())
check(success.isCompleted())
```

## 5. Transformations

All transformation functions pass `Pending` through unchanged. `Error` is
also passed through unless the specific function is designed to handle it
(`transform`, `catch`, `catchAll`, `recover`). A thrown `Exception` inside any
transformation lambda is caught automatically and turned into
`Container.Error`; a thrown `CancellationException` is rethrown.

```kotlin
// map: transform only the Success value
val mapped: Container<String> = container.map { it.toString() }

// transform: full control over both branches, must return a Container
val transformed: Container<String> = container.transform(
    onError = { ex -> errorContainer(RuntimeException("Wrapped", ex)) },
    onSuccess = { value -> successContainer(value.toString().uppercase()) },
)

// catch: intercept one specific exception type via KClass
val caught: Container<Int> = container.catch(NetworkException::class) { ex ->
    successContainer(0)
}

// catchAll: intercept every exception type
val caughtAll: Container<Int> = container.catchAll { ex ->
    successContainer(0)
}

// mapException: rewrite the exception without changing Success/Error outcome
val exceptionMapped: Container<Int> = container.mapException(IOException::class) { ioEx ->
    RuntimeException("IO failure", ioEx)
}

// recover: like catch, but the lambda returns a plain value that is
// automatically wrapped into successContainer(...)
val recovered: Container<Int> = container.recover(TimeoutException::class) { ex ->
    -1
}
```

`catch`, `mapException`, and `recover` all take the target exception type as
a `KClass<E>` first argument (e.g. `NetworkException::class`), not a reified
type parameter.

## 6. Flow extensions

Every `container*` flow operator mirrors a container-level function from
section 5 (or `fold` from section 4), applied per-emission to a
`Flow<Container<T>>`. `Pending` is always passed through unchanged.

```kotlin
// containerMap / containerMapLatest: transform the Success value
val stringFlow: Flow<Container<String>> = intFlow.containerMap { number -> number.toString() }
val stringFlowLatest: Flow<Container<String>> = intFlow.containerMapLatest { number -> number.toString() }

// containerFlatMapLatest: switch to a new Flow<Container<R>> per Success value,
// cancelling the previous inner flow. The operator itself does not inject a
// Pending emission before switching - it emits pendingContainer() only when
// the OUTER container is Pending. Any Pending you see between switching and
// the inner flow's first value comes from the inner flow itself (here,
// containerFlowOf { } emits Pending as its first value on its own); an inner
// flow that starts straight at Success (e.g. flowOf(successContainer(x)))
// produces no intermediate Pending at all.
val flatMapped: Flow<Container<String>> = intFlow.containerFlatMapLatest { number ->
    containerFlowOf { emit(number.toString()) }
}

// containerFilter / containerFilterNot: keep/drop Success values by predicate;
// Pending and Error are always passed through
val filtered: Flow<Container<Int>> = intFlow.containerFilter { it > 0 }
val filteredNot: Flow<Container<Int>> = intFlow.containerFilterNot { it <= 0 }

// containerFold / containerFoldDefault / containerFoldNullable:
// collapse Flow<Container<T>> into Flow<R>
val folded: Flow<String> = intFlow.containerFold(
    onSuccess = { value -> value.toString() },
    onError = { ex -> "Error: ${ex.message}" },
    onPending = { "Loading..." },
)
val foldedDefault: Flow<String> = intFlow.containerFoldDefault(
    defaultValue = "-",
    onSuccess = { value -> value.toString() },
    onError = { "-" },
    onPending = { "-" },
)
val foldedNullable: Flow<String?> = intFlow.containerFoldNullable(
    onSuccess = { value -> value.toString() },
    onError = { null },
    onPending = { null },
)

// containerTransform: emit a different container per Success/Error
val transformed: Flow<Container<String>> = intFlow.containerTransform(
    onSuccess = { value -> successContainer(value.toString().uppercase()) },
    onError = { ex -> errorContainer(RuntimeException("Wrapped", ex)) },
)

// containerCatch / containerCatchAll / containerRecover / containerMapException:
// flow-level mirrors of catch / catchAll / recover / mapException
val caught: Flow<Container<Int>> = intFlow.containerCatch(NetworkException::class) { ex ->
    successContainer(0)
}
val caughtAll: Flow<Container<Int>> = intFlow.containerCatchAll { ex -> successContainer(0) }
val recovered: Flow<Container<Int>> = intFlow.containerRecover(TimeoutException::class) { ex -> -1 }
val exceptionMapped: Flow<Container<Int>> = intFlow.containerMapException(IOException::class) { ioEx ->
    RuntimeException("IO failure", ioEx)
}

// containerUpdate: mutate metadata on every emitted container, value untouched
val updated: Flow<Container<Int>> = intFlow.containerUpdate {
    // block runs with ContainerUpdater receiver; e.g. `reloadFunction = ...`
}
```

`containerFoldDefault` and `containerFoldNullable` declare defaults for
`onSuccess`/`onError`/`onPending`, so you may pass only the callbacks you need
and let the rest fall back to `defaultValue` / `null`. Their callbacks are
`suspend`, so they may call suspend functions.

## 7. Combining containers and container flows

To combine several *containers* (not flows) into one, use the `containerOf { }`
idiom from section 2 together with `unwrap()`: it fails fast (turning into
`Container.Error`) if either input is not yet `Success`.

```kotlin
private fun combining(a: Container<Int>, b: Container<Int>): Container<Int> {
    return containerOf { a.unwrap() + b.unwrap() }
}
```

To combine several `Flow<Container<T>>` sources, use `combineContainerFlows`.
The result is `Error` (first error wins) if any input is `Error`, `Pending` if
any input is `Pending` and none is `Error`, and `Success` only once every input
is `Success`. `Error` beats `Pending`: the implementation looks for an `Error`
input first and only falls back to `Pending`.
Overloads exist for two through five named flows, plus a list overload for
an arbitrary number:

```kotlin
val combined: Flow<Container<String>> = combineContainerFlows(
    flow1 = getUserFlow(),   // Flow<Container<Product>>
    flow2 = getItemsFlow(),  // Flow<Container<List<Product>>>
) { user, items ->
    "${user.name}: ${items.size} items"
}

val combinedList: Flow<Container<String>> = combineContainerFlows(
    flows = listOf(getUserFlow(), getUserFlow(), getUserFlow()),
) { values -> values.joinToString() }
```

To combine a `Flow<Container<T>>` with one or more *plain* (non-container)
flows, use `containerCombineWith`. The result's state (`Pending`/`Error`/
`Success`) always follows the container flow; the plain flow values are
simply merged in once the container side is `Success`:

```kotlin
val combinedWith: Flow<Container<String>> = getUserFlow()
    .containerCombineWith(plainFlow) { value, extra ->
        "${value.name} ($extra)"
    }
```

## 8. Updating one item in a list

`updateItem` replaces an element of a `List<T>` by identity rather than by
position, returning a new list. When no element matches, the original list is
returned unchanged - lists are often filtered, sorted or paged differently from
wherever the new item came from, so a miss is normal.

```kotlin
// replace with a newer copy
val patched: List<Product> = products.updateItem(updatedProduct) { it.id }

// derive the new value from the old one; transform is skipped if the id is absent
val favourited: List<Product> =
    products.updateItem(productId, { it.id }) { it.copy(isFavorite = true) }
```

The `itemId` selector has the same shape `pageLoader` takes, so one selector
serves both. Both overloads also exist on `Container<List<T>>`; non-`Success`
containers pass through unchanged.

## 9. Type aliases

```kotlin
public typealias ListContainer<T>     = Container<List<T>>
public typealias ContainerFlow<T>     = Flow<Container<T>>
public typealias ListContainerFlow<T> = Flow<Container<List<T>>>
```

Use these instead of spelling out the nested generics, especially in function
signatures:

```kotlin
fun getProducts(): ListContainerFlow<Product> = ...

private fun aliases(flow: Flow<Container<Product>>): Pair<ContainerFlow<Product>, ListContainerFlow<Product>?> {
    val listContainer: ListContainer<Product> = successContainer(listOf(Product(1, "Book")))
    return flow to null
}
```
