# Container Type

This page describes `Container<T>` in depth: its states, how to create and
inspect containers, how to transform them, and how to work with them in
Kotlin Flows.

## Table of Contents

- [Container States](#container-states)
- [Creating Containers](#creating-containers)
- [Extracting Values](#extracting-values)
- [Pattern Matching with when / fold](#pattern-matching-with-when-fold)
- [Transformations](#transformations)
  - [map](#map)
  - [transform](#transform)
  - [catch and catchAll](#catch-and-catchall)
  - [mapException](#mapexception)
  - [recover](#recover)
- [Flow Extensions](#flow-extensions)
  - [containerMap and containerMapLatest](#containermap-and-containermaplatest)
  - [containerFlatMapLatest](#containerflatmaplatest)
  - [containerFilter and containerFilterNot](#containerfilter-and-containerfilternot)
  - [containerFold Variants](#containerfold-variants)
  - [containerTransform](#containertransform)
  - [containerCatch, containerCatchAll, containerRecover](#containercatch-containercatchall-containerrecover)
  - [containerUpdate](#containerupdate)
- [Combining Container Flows](#combining-container-flows)
  - [combineContainerFlows](#combinecontainerflows)
  - [containerCombineWith](#containercombinewith)
- [Type Aliases](#type-aliases)
- [Metadata](#metadata)

## Container States

`Container<T>` is a sealed class with three concrete states:

```kotlin
sealed class Container<out T> {
    object Pending : Container<Nothing>()

    sealed class Completed<out T> : Container<T>()

    data class Success<out T>(
        val value: T,
        val metadata: ContainerMetadata,
    ) : Completed<T>()

    data class Error(
        val exception: Exception,
        val metadata: ContainerMetadata,
    ) : Completed<Nothing>()
}
```

- **`Container.Pending`** - an operation is in progress; no value is available yet
- **`Container.Success<T>`** - the operation finished successfully; the loaded
  value is in `value`
- **`Container.Error`** - the operation failed; the reason is in `exception`
- **`Container.Completed<T>`** - abstract base for `Success` and `Error`;
  use it when you want to match any finished state

Both `Success` and `Error` carry an optional `metadata: ContainerMetadata`
bag that can hold additional information such as the data source, a reload
function, or background-load indicators. See [Subjects & Cache](subjects.md)
for details on metadata.

## Creating Containers

Use the top-level factory functions:

```kotlin
val pending = pendingContainer()

val success = successContainer("Hello, world!")
val successWithMeta = successContainer("Hello", SourceTypeMetadata(RemoteSourceType))

val error = errorContainer(IOException("network error"))
```

You can also combine metadata after creation using the `+` operator:

```kotlin
val container = successContainer("data") + ReloadFunctionMetadata { reload() }
```

## Extracting Values

Several extension functions allow you to pull data out of a container
without pattern matching:

| Function                        | Returns                      | Behaviour |
|---------------------------------|------------------------------|-----------|
| `getOrNull()`                   | `T?`                         | Value if `Success`, otherwise `null` |
| `unwrap()`                      | `T`                          | Value if `Success`, throws otherwise |
| `exceptionOrNull()`             | `Exception?`                 | Exception if `Error`, otherwise `null` |
| `getContainerValueOrNull()`     | `ContainerValue<T>?`         | Value + metadata if `Success` |
| `getContainerExceptionOrNull()` | `ContainerValue<Exception>?` | Exception + metadata if `Error` |
| `unwrapContainerValue()`        | `ContainerValue<T>`          | Value + metadata if `Success`, throws otherwise |

```kotlin
val container: Container<String> = ...

val value: String? = container.getOrNull()
val error: Exception? = container.exceptionOrNull()

// throws LoadNotFinishedException if Pending, or re-throws the exception if Error
val value: String = container.unwrap()
```

`ContainerValue<T>` is a simple wrapper that gives you access to both the
value and the associated metadata:

```kotlin
val cv: ContainerValue<String> = container.getContainerValueOrNull() ?: return
println(cv.value)
println(cv.source)               // shorthand for metadata.sourceType
println(cv.isLoadingInBackground)
```

## Pattern Matching with when / fold

You can use Kotlin's `when` expression to pattern-match on all three states:

```kotlin
when (container) {
    is Container.Pending -> { /* show a progress spinner */ }
    is Container.Error   -> { /* show an error message */ }
    is Container.Success -> { /* render the data */ }
}
```

Alternatively, `fold` is the primary way to handle all three states exhaustively
and return a value in one expression:

```kotlin
val result: String = container.fold(
    onPending = { "Loading..." },
    onError   = { exception -> "Error: ${exception.message}" },
    onSuccess = { value -> value },
)
```

Two convenience variants are also available:

```kotlin
// returns defaultValue for unhandled states:
val text = container.foldDefault(
    defaultValue = "-",
    onError      = { ex -> "Error: ${ex.message}" },
    onSuccess    = { value -> value },
)

// returns null for unhandled states:
val text: String? = container.foldNullable(
    onSuccess = { value -> value },
)
```


## Transformations

All transformation functions pass `Pending` through unchanged. `Error` is
also passed through unless the specific function handles it.

### map

Transform the value inside a `Success` container:

```kotlin
val container: Container<Int> = successContainer(42)
val mapped: Container<String> = container.map { it.toString() }
```

### transform

Full control over the output container. Receives the value (on success) or
exception (on error) and must return a `Container`:

```kotlin
val result: Container<String> = container.transform(
    onError   = { ex -> errorContainer(RuntimeException("Wrapped", ex)) },
    onSuccess = { value -> successContainer(value.uppercase()) },
)
```

### catch and catchAll

Intercept exceptions and replace the `Error` state with a new `Container`:

```kotlin
// catch a specific exception type:
val safe: Container<List<Item>> = container.catch(NetworkException::class) { ex ->
    successContainer(emptyList())
}

// catch any exception:
val safe: Container<List<Item>> = container.catchAll { ex ->
    successContainer(emptyList())
}
```

### mapException

Map one exception type to another without changing the `Error`/`Success`
outcome:

```kotlin
val mapped: Container<String> = container.mapException(IOException::class) { ioEx ->
    RuntimeException("IO failure", ioEx)
}
```

### recover

Similar to `catch`, but easier to use if you want to recover from an exception to a success value.
The recovery lambda receives the exception and returns a new `successContainer` out of the box:

```kotlin
val result: Container<String> = container.recover(TimeoutException::class) { ex ->
    "default value"
}
```

## Flow Extensions

All `Flow<Container<T>>` extension functions follow the same naming convention:
they mirror the container-level functions but operate on the flow level.
`Pending` states are always passed through unchanged by default.

### containerMap and containerMapLatest

```kotlin
val stringFlow: Flow<Container<String>> = intFlow
    .containerMap { number -> number.toString() }

// cancels the previous transform when a new container arrives:
val stringFlow: Flow<Container<String>> = intFlow
    .containerMapLatest { number ->
        delay(100)
        number.toString()
    }
```

`StateFlow<Container<T>>` has a dedicated variant that returns a
`StateFlow<Container<R>>`:

```kotlin
val stringState: StateFlow<Container<String>> = intState
    .containerStateMap { it.toString() }
```

### containerFlatMapLatest

Transforms each `Success` value into a new `Flow<Container<R>>`, cancelling
the previous inner flow when a new outer value arrives. The resulting flow
emits `Pending` while waiting for the inner flow:

```kotlin
val detailsFlow: Flow<Container<Details>> = idsFlow
    .containerFlatMapLatest { id ->
        repository.getDetails(id) // Flow<Container<Details>>
    }
```

### containerFilter and containerFilterNot

Keep or drop `Success` containers whose values match a predicate. `Pending`
and `Error` states are always passed through:

```kotlin
val nonEmptyFlow = listFlow.containerFilter { list -> list.isNotEmpty() }
val nonEmptyFlow = listFlow.containerFilterNot { list -> list.isEmpty() }
```

### containerFold Variants

Collapse a `Flow<Container<T>>` into a `Flow<R>` by mapping each container
state to a value:

```kotlin
val textFlow: Flow<String> = containerFlow.containerFold(
    onPending = { "Loading..." },
    onError   = { ex -> "Error: ${ex.message}" },
    onSuccess = { value -> value.toString() },
)

// with a default value for unhandled cases:
val textFlow: Flow<String> = containerFlow.containerFoldDefault(
    defaultValue = "-",
    onSuccess    = { value -> value.toString() },
)

// nullable result:
val textFlow: Flow<String?> = containerFlow.containerFoldNullable(
    onSuccess = { value -> value.toString() },
)
```

### containerTransform

Emit a different container for each incoming `Success` or `Error`. Useful
when you need to replace an error with a success or vice versa:

```kotlin
val result: Flow<Container<String>> = containerFlow.containerTransform(
    onError   = { ex -> errorContainer(RuntimeException("Wrapped", ex)) },
    onSuccess = { value -> successContainer(value.uppercase()) },
)
```

### containerCatch, containerCatchAll, containerRecover

Mirror of the single-container functions, applied across the flow:

```kotlin
// convert a specific exception type to `successContainer`:
val resultFlow = flow.containerCatch(NetworkException::class) { ex ->
    successContainer(emptyList())
}

// convert any exception to `successContainer`:
val resultFlow = flow.containerCatchAll { ex -> successContainer(emptyList()) }

// recover with a value returned from lambda wrapping it into `successContainer`:
val resultFlow = flow.containerRecover(TimeoutException::class) { ex ->
    "default"
}

// map exception type to another:
val mapped = flow.containerMapException(IOException::class) { ex ->
    RuntimeException("IO failure", ex)
}
```

### containerUpdate

Update metadata on every container in the flow without changing the value:

```kotlin
val flow: Flow<Container<String>> = source
    .containerUpdate {
        reloadFunction = ::loadList
    }
```

## Combining Container Flows

### combineContainerFlows

Combine two or more `Flow<Container<T>>` into a single flow. The result is:

- `Container.Pending` if any input is `Pending`
- `Container.Error` (first error wins) if any input is `Error`
- `Container.Success` only when every input is `Success`

```kotlin
val combined: Flow<Container<String>> = combineContainerFlows(
    flow1 = getUserFlow(),   // Flow<Container<User>>
    flow2 = getItemsFlow(),  // Flow<Container<List<Item>>>
) { user, items ->
    "${user.name}: ${items.size} items"
}
```

Up to five flows can be combined with overloads; an arbitrary number can be
combined using the list overload:

```kotlin
val combined = combineContainerFlows(
    flows = listOf(flow1, flow2, flow3),
) { values -> values.joinToString() }
```

### containerCombineWith

Combine a `Flow<Container<T>>` with one or more plain (non-container) flows.
The container state is propagated; the plain flow values are simply merged in:

```kotlin
val combined: Flow<Container<String>> = containerFlow
    .containerCombineWith(plainFlow) { value, extra ->
        "$value ($extra)"
    }
```

## Type Aliases

The library provides several type aliases to reduce boilerplate:

```kotlin
typealias ListContainer<T>     = Container<List<T>>
typealias ContainerFlow<T>     = Flow<Container<T>>
typealias ListContainerFlow<T> = Flow<Container<List<T>>>
```

Usage:

```kotlin
fun getProducts(): ListContainerFlow<Product> = ...
```

## Metadata

Both `Container.Success` and `Container.Error` carry a `metadata: ContainerMetadata`
bag. The standard metadata properties are:

| Property                | Type             | Meaning |
|-------------------------|------------------|---------|
| `source`                | `SourceType`     | Where the data came from |
| `isLoadingInBackground` | `Boolean`        | A newer value is being fetched in the background |
| `reloadFunction`        | `ReloadFunction` | Call this to re-trigger the load |

Access them through shorthand extension properties on the container or through
the metadata bag:

```kotlin
val success: Container.Success<String> = ...
val source = success.source  // shorthand
val source = success.metadata.sourceType  // metadata extension
val source = success.metadata.get<SourceTypeMetadata>()?.sourceType  // explicit
```

For detailed metadata information, including `SourceType` values,
`LoadTrigger`, and how to attach custom metadata, see
[Subjects & Cache](subjects.md).
