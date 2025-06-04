# Container

[![Maven Central](https://img.shields.io/maven-central/v/com.elveum/container.svg?label=Maven%20Central)](https://uandcode.com/sh/container)
![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)
[![License: Apache 2](https://img.shields.io/github/license/romychab/container)](LICENSE)

Pure Kotlin Library which adds one sealed class
named `Container` and a couple of helper methods and classes for
Kotlin Flow and for converting async data loaders into reactive Flows.

## Installation

Just add the following line to your `build.gradle` file:

```
implementation "com.elveum:container:1.0.1"
```

## Documentation

Click [here](https://docs.uandcode.com/container/).

## Short Description

The library contains:
- `Container<T>` class - represents the status of async operation (`Pending`, `Error`, `Success`)
- A couple of helper extension methods for easier working (mapping, filtering, etc.) with
  Kotlin Flows containing `Container<T>` or `Container<List<T>>` instances
- Two subjects:
  - `FlowSubject` - represents a simple __finite__ flow which emissions can be controlled
    outside
  - `LazyFlowSubject` - provides a convenient way of transforming suspend functions
    into Kotlin Flow with the possibility to update values, replace loaders, load multiple
    values, cache latest loaded value and with loading values only on demand
- `LazyCache` which can hold multiple instances of `LazyFlowSubject`
- `StateFlow` extensions:
  - `stateMap` function which can convert `StateFlow<T>` into `StateFlow<R>` (origin `map` function
    converts `StateFlow<T>` into more generic `Flow<R>`)
  - `combineStates` function which can merge 2 or more `StateFlow<*>` instances into one `StateFlow<R>` instance
    (origin `combine` function merges flows into more generic `Flow<R>`, but not into `StateFlow<R>`)

## About Container

`Container` is a simple class which represents the status of async operation:
- `Container.Pending` - data is loading
- `Container.Success<T>(value)` - data has been loaded successfully
- `Container.Error(exception)` - loading has been failed with error

There are a couple of methods which can simplify a code working with such
statuses:
- `map`
- `unwrap`, `unwrapOrNull`
- `getOrNull`, `exceptionOrNull`

## Integration with Kotlin Flows

- `Flow<Container<T>>.unwrapFirst()` returns the first non-pending T value from the flow
- `Flow<Container<T>>.unwrapFirstOrElse()` and `Flow<Container<T>>.unwrapFirstOrDefault()` returns
  the first non-pending T value from the flow or the default value otherwise
- `Flow<Container<T>>.containerMap()` converts the flow of type `Container<T>` into a flow
  of type `Container<R>`
- `Flow<Container<T>>.containerFilter()` filters all `Container.Success<T>` values by a given predicate

Also, there are the following type aliases:
- `ListContainer<T>` = `Container<List<T>>`
- `ContainerFlow<T>` = `Flow<Container<T>>`
- `ListContainerFlow<T>` = `Flow<Container<List<T>>>`

## Subjects

Subjects are classes controlling flow emissions (name is taken from Reactive Streams)

### FlowSubject

`FlowSubject` represents a finite `Flow` which emission is controlled outside
(like `StateFlow` and `SharedFlow`). The `FlowSubject` holds the latest value but
there are differences from `StateFlow`:
- `FlowSubject` is a finite flow and it can be completed by using `onComplete`
  and `onError` methods
- `FlowSubject` doesn't need a starting default value
- `FlowSubject` doesn't hold the latest value if it has been completed with error

Usage example:

```kotlin
val flowSubject = FlowSubject.create<String>()
flowSubject.onNext("first")
flowSubject.onNext("second")
flowSubject.onComplete()
flowSubject.listen().collect {
  // ...
}
```

### LazyFlowSubject

`LazyFlowSubject<T>` provides a mechanism of converting a load
function into a `Flow<Container<T>>`.

Features:

1. The load function is usually executed only once (except #6 and #8)
2. The load function is executed only when at least one subscriber
   starts collecting the flow
3. The load function can emit more than one value
4. The load function is cancelled when the last subscriber stops
   collecting the flow after timeout (default timeout = 1sec)
5. The latest result is cached, so any new subscriber can receive the
   most actual loaded value without triggering the load function again and again
6. There is a timeout (default value is 1sec) after the last subscriber
   stops collecting the flow. When timeout expires, the cached value is
   cleared so any further subscribers will execute the load function again
7. The flow can be collected by using `listen()` method
8. You can replace the load function at any time by using the following methods:
   - `newLoad` - assign a new load function which can emit more than one value.
     This method also returns a separate flow which differs from the flow returned by `listen()`:
     it emits only values emitted by a new load function and completes as soon
     as a new load function completes
   - `newAsyncLoad` - the same as `newLoad` but it returns Unit immediately
   - `newSimpleLoad` - assign a new load function which can emit only one value.
     This is a suspend function and it waits until a new load function completes and
     returns its result (or throws an exception)
   - `newSimpleAsyncLoad` - the same as `newSimpleLoad` but it doesn't wait for load
     results and returns immediately
9. Also you can use `updateWith` method in order to cancel any active loader
   and place your own value immediately to the subject. The previous loader function
   will be used again if you call `reload()` or if cache has been expired.

Usage example:

```kotlin
class ProductRepository(
  private val productsLocalDataSource: ProductsLocalDataSource,
  private val productsRemoteDataSource: ProductsRemoteDataSource,
) {

    private val productsSubject = LazyFlowSubject.create {
        val localProducts = productsLocalDataSource.getProducts()
        if (localProducts != null) emit(localProducts)
        val remoteProducts = productsRemoteDataSource.getProducts()
        productsLocalDataSource.saveProducts(remoteProducts)
        emit(remoteProducts)
    }

    // ListContainerFlow<T> is an alias to Flow<Container<List<T>>>
    fun listenProducts(): ListContainerFlow<Product> {
        return productsSubject.listen()
    }

    fun reload() {
        productsSubject.reloadAsync()
    }

}
```

#### Load Triggers

You can access an additional field named `loadTrigger` within the
`LazyFlowSubject.create { ... }` block. Depending on its value, you can change the load
logic. For example, you can skip loading data from the local cache if the loading process has
been initiated by `reload` call:

```kotlin
private val productsSubject = LazyFlowSubject.create {
    if (loadTrigger != LoadTrigger.Reload) {
        val localProducts = productsLocalDataSource.getProducts()
        if (localProducts != null) emit(localProducts)
    }
    val remoteProducts = productsRemoteDataSource.getProducts()
    productsLocalDataSource.saveProducts(localProducts)
    emit(remoteProducts)
}
```

#### Source Types

Optionally you can assign a `SourceType` to any `Container.Success` value
just to let them know about an actual source where data arrived from.

```kotlin
// in loader function:
val subject = LazyFlowSubject.create {
    val remoteProducts = productsRemoteDataSource.getProducts()
    emit(remoteProducts, RemoteSourceType)
}

// in `Container.Success()` directly:
subject.updateWith(Container.Success("hello", FakeSourceType))

// in `Container.updateIfSuccess`:
subject.updateIfSuccess(ImmediateSourceType) { oldValue ->
    oldValue.copy(isFavorite = true)
}
```

Source types can be accessed via `Container.Success` instance:

```kotlin
subject.listen()
    .filterIsInstance<Container.Success<String>>()
    .collectLatest { successContainer ->
        val value = successContainer.value
        val sourceType = successContainer.source
        println("$value, isRemote = ${sourceType == RemoteSourceType}")
    }
```

## Lazy Cache

`LazyCache` is a store of multiple `LazyFlowSubject` instances. It allows you
defining listeners and loader functions with additional arguments.

```kotlin

val lazyCache = LazyCache.create<Long, User> { id ->
    val localUser = localUsersDataSource.getUserById(id)
    if (localUser != null) {
        emit(localUser)
    }
    val remoteUser = remoteUsersDataSource.getUserById(id)
    localUsersDataSource.save(remoteUser)
    emit(remoteUser)
}

fun getUserById(id: Long): Flow<Container<User>> {
    return lazyCache.listen(id)
}

```
