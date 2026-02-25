# Container

[![Maven Central](https://img.shields.io/maven-central/v/com.elveum/container.svg?label=Maven%20Central)](https://uandcode.com/sh/container)
![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)
[![License: Apache 2](https://img.shields.io/github/license/romychab/container)](LICENSE)

Container is a library for simplifying state management and data loading in
Android applications. It provides a small set of building blocks
that cover the most common reactive patterns: wrapping async results in a
typed status, managing derived state from multiple flows, and lazily loading
data on demand.

## Table of Contents

- [Installation](#installation)
- [Core Concepts](#core-concepts)
  - [Container Type](#container-type)
  - [Reducer Pattern](#reducer-pattern)
  - [LazyFlowSubject](#lazyflowsubject)
  - [LazyCache](#lazycache)
- [Detailed Documentation](#detailed-documentation)

## Installation

Add the following line to your `build.gradle` file:

```
implementation "com.elveum:container:2.0.0-beta15"
```

## Core Concepts

The library is built around three building blocks:

- **`Container<T>`** - a sealed type that represents the state of an async operation as `Pending`, `Success<T>`, or `Error`
- **`Reducer<State>`** - converts one or more Kotlin `Flow`s into a `StateFlow<State>`, with support for manual state updates
- **`LazyFlowSubject<T>`** - wraps a loader function in a lazily-started `Flow<Container<T>>` with built-in caching, reloading, and `Container` status handling

### Container Type

`Container<T>` is a sealed class that represents the current status of an
asynchronous load or operation. It has three possible states:

- `Container.Pending` - the operation is still in progress
- `Container.Success<T>` - the operation completed successfully and holds a `value: T`
- `Container.Error` - the operation failed and holds an `exception: Exception`

Use the Kotlin `when` keyword or the `fold` call to handle all three states in one place:

```kotlin
val container: Container<String> = successContainer("Hello")

container.fold(
    onPending = { /* show a progress spinner */ },
    onError = { exception -> /* show an error message */ },
    onSuccess = { value -> /* render the data */ },
)
```

Containers can be created with factory functions:

```kotlin
val pending = pendingContainer()
val success = successContainer("data")
val error   = errorContainer(IOException("network error"))
```

For a complete guide (including value extraction, transformations,
combining flows, and more), see [Container Type](docs/container-type.md).

### Reducer Pattern

`Reducer<State>` converts any Kotlin `Flow` into a `StateFlow<State>` while
also allowing manual state updates. This makes it easy to drive a screen's
UI state from one or more reactive sources, with the ability to apply local
changes on top.

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val getItems: GetItemsUseCase,
) : ViewModel() {

    data class State(
        val items: List<String> = emptyList(),
        val filter: String = "",
    )

    private val reducer = getItems() // Flow<List<String>>
        .toReducer(
            initialState = State(),
            nextState = State::copy,
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
        )

    val stateFlow: StateFlow<State> = reducer.stateFlow

    fun applyFilter(filter: String) {
        reducer.update { it.copy(filter = filter) }
    }
}
```

`ContainerReducer<State>` is the container-aware variant. It exposes a
`StateFlow<Container<State>>` so the UI automatically sees `Pending`,
`Error`, and `Success` states without any manual bookkeeping:

```kotlin
private val reducer: ContainerReducer<State> = getItems()
    .toContainerReducer(
        initialState = ::State,
        nextState = State::copy,
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
    )

val stateFlow: StateFlow<Container<State>> = reducer.stateFlow
```

For the full API (combining multiple flows, the `ReducerOwner` interface,
and the public-interface / private-implementation state pattern), see
[Reducer Pattern](docs/reducer-pattern.md).

### LazyFlowSubject

`LazyFlowSubject<T>` converts a loader function into a `Flow<Container<T>>`.
The loader runs lazily (only when at least one subscriber is active) and its
latest result is cached so that new subscribers do not re-trigger loading:

`LazyFlowSubject` is more powerful and leads to simpler code than the built-in
`stateIn` / `shareIn` operators: it does not require a `CoroutineScope`, automatically
wraps results in `Container<T>` to handle loading and error states, supports reloading
out of the box, and is compatible with any caching strategy.

```kotlin
class ProductRepository(
    private val localDataSource: ProductsLocalDataSource,
    private val remoteDataSource: ProductsRemoteDataSource,
) {

    private val productsSubject = LazyFlowSubject.create {
        val local = localDataSource.getProducts()
        if (local != null) emit(local)
        val remote = remoteDataSource.getProducts()
        localDataSource.save(remote)
        emit(remote)
    }

    // ListContainerFlow<T> is an alias for Flow<Container<List<T>>>
    fun listenProducts(): ListContainerFlow<Product> = productsSubject.listen()

    fun reload() = productsSubject.reloadAsync()
}
```

Key behaviours:

- Instead of `listen()`, you can use `listenReloadable()` call, which attaches
  a reload function to every emitted container, enabling pull-to-refresh patterns
  out of the box (no need to write a separate `reload()` function).
- The loader is cancelled when the last subscriber stops collecting (after
  a configurable timeout, default 1 s).
- After the timeout the cached value is cleared, so the next subscriber
  triggers a fresh load
- You can replace the loader at any time with `newLoad` / `newSimpleLoad`
- You can push a value directly with `updateWith`

For advanced usage (load triggers, source types, flow dependencies, and
`SubjectFactory` for testability) see [Subjects & Cache](docs/subjects.md).

### LazyCache

`LazyCache<Arg, T>` is a collection of `LazyFlowSubject` instances keyed by
an argument. Use it whenever you need to load and cache data for multiple
distinct identifiers (e.g. user IDs, product IDs):

```kotlin
private val usersCache = LazyCache.create<Long, User> { id ->
    val local = localDataSource.getUserById(id)
    if (local != null) emit(local)
    emit(remoteDataSource.getUserById(id))
}

fun getUser(id: Long): Flow<Container<User>> = usersCache.listen(id)

fun reloadUser(id: Long) = usersCache.reloadAsync(id)
```

Each key has its own independent loading lifecycle, caching behaviour, and
subscriber count. See [Subjects & Cache](docs/subjects.md) for the complete API.

## Detailed Documentation

| Topic | Description |
|-------|-------------|
| [Container Type](docs/container-type.md) | States, value extraction, transformations, flow extensions, combining flows |
| [Reducer Pattern](docs/reducer-pattern.md) | `Reducer`, `ContainerReducer`, combining flows, `ReducerOwner` |
| [Subjects & Cache](docs/subjects.md) | `LazyFlowSubject`, `LazyCache`, `FlowSubject`, metadata, source types |
