# Container

[![Maven Central](https://img.shields.io/maven-central/v/com.elveum/container.svg?label=Maven%20Central)](https://uandcode.com/sh/container)
![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)
[![License: Apache 2](https://img.shields.io/github/license/romychab/container)](LICENSE)

Container is a library for simplifying state management and data loading in
android applications.

## Installation

Just add the following line to your `build.gradle` file:

```
implementation "com.elveum:container:2.0.0-beta10"
```

## Documentation

Click [here](https://docs.uandcode.com/container/).

## Container Type

`Container` type representing the status of async/load operation:
- `Container.Pending` - data is loading
- `Container.Completed` - data loading is finished:
  - `Container.Success` - data has been loaded successfully
  - `Container.Error` - loading has been failed with error

Both `Container.Success` and `Container.Error` extends `Container.Completed`.

You can create containers by using the following functions:
- `successContainer(value)`
- `errorContainer(exception)`
- `pendingContainer()`

## Reducer Pattern

Reducers make state management very easy to implement in your app. The main difference
from `MutableStateFlow` is the possibility to update values from multiple
sources: from input Kotlin Flows, along with manual updates if needed. 

Reducers can be created by using:

- extension functions on any Kotlin Flow, such as `Flow<T>.toReducer()`
- combine functions, e.g. `combineToReducer`

These functions works very similar to `stateIn` and/or `shareIn` operators, since
they accept a `CoroutineScope` instance and `SharingStarted` strategy. And at the same
time, the returned type (`Reducer`) gives you an `update()` call, in contrast to 
`stateIn` operator, which returns non-mutable `StateFlow`.

So, any existing Kotlin Flow can be converted into Reducer:

  ```kotlin

  interface GetItemsUseCase {
      operator fun invoke(): Flow<List<String>>
  }

  @HiltViewModel
  class MyViewModel @Inject constructor(
      private val getItems: GetItemsUseCase,
  ) : ViewModel() {
  
      data class State(
          val items: List<String> = emptyList(),
      )
  
      private val reducer = getItems() // Flow<List<String>>
          .toReducer(
              initialState = State(),
              nextState = State::copy,
              scope = viewModelScope,
              started = SharingStarted.Lazily,
          ) // Reducer<State>
      val stateFlow: StateFlow<State> = reducer.stateFlow
  }
  ```

You can even simplify the creation of any reducer if you enable Kotlin Context
Parameters feature. For example:

1) Modify your `build.gradle` file:

   ```kotlin
   kotlin {
       compilerOptions {
           freeCompilerArgs.add("-Xcontext-parameters")
       }
   }
   ```

2) Create an abstract ViewModel implementing `ReducerOwner` interface:

   ```kotlin
   abstract class AbstractViewModel : ViewModel(), ReducerOwner {
       override val reducerCoroutineScope: CoroutineScope
           get() = viewModelScope
       override val reducerSharingStarted: SharingStarted
           get() = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000)
   }
   ```

3) Now, you can create reducers without specifying `CoroutineScope` and `SharingStarted`
   values in child view-models:

   ```kotlin
   class MyViewModel : AbstractViewModel() { // <-- extend abstract view-model
       private val reducer: Reducer<State> = getItems()
           .toReducer(
               initialState = State(),
               nextState = State::copy,
           )
       val stateFlow: StateFlow<State> = reducer.stateFlow
   }
   ```

As a result, you have a public `stateFlow: StateFlow<State>` property, which is
automatically updated any time when a new value comes from the Flow returned by
`GetItemsUseCase`. Additionally, you can update state manually:

```kotlin
reducer.update { oldState ->
    oldState.copy(items = emptyList())
}
```

Another one feature is the possibility to split your state class into public
interface and private implementation without mappings. For example, let's add
an additional property which defines how string items must be displayed:

```kotlin
// private state:
private data class StateImpl(
    val originItems: List<String> = emptyList(),
    // when enabled, all strings must be uppercased:
    val isUppercase: Boolean = false,
)
```

Next, define a public state interface:

```kotlin
interface State {
    val items: List<String>
}
```

Implement the interface in your private state:

```kotlin
private data class StateImpl(
    val originItems: List<String> = emptyList(),
    val isUppercase: Boolean = false,
) : State {
    override val items = if (isUppercase) {
        originItems.map { it.uppercase() }
    } else {
        originItems
    }
}
```

And the last step - create a reducer of private state:

```kotlin
// Reducer with private StateImpl:
private val reducer: Reducer<StateImpl> = getItems()
    .toReducer(
        initialState = StateImpl(),
        nextState = StateImpl::copy,
    )

// Output StateFlow with public State:
val stateFlow: StateFlow<State> = reducer.stateFlow
```

All examples above leveraged the usage of `Reducer<T>` type. Now let's consider
all ways how reducers can be created. Here is the list of converter functions:

- `Flow<T>.toReducer()` - creates a simple `Reducer<T>`
- `Flow<T>.toContainerReducer()` - creates a `ContainerReducer<T>`
- `Flow<Container<T>>.containerToReducer()` - creates a `ContainerReducer<T>` with
  automatic management of containers coming from origin flow

Also, you can combine 2 or more Kotlin Flows into one Reducer by using:
- `combineToReducer(): Reducer<State>`
- `combineToContainerReducer(): ContainerReducer<State>`
- `combineContainersToReducer(): ContainerReducer<State>`

### Reducer vs ContainerReducer
As you see, the library has 2 types of reducers:
- `Reducer` - provides `StateFlow<T>` for observing state, and `update()` method
  for updating values
- `ContainerReducer` - provides `StateFlow<Container<T>>` for observing state wrapped
  into `Container` along with 2 methods for updating: `update()` and `updateState()`. The
  `updateState()` method allows you to modify values directly in the container.

Effectively, `ContainerReducer<T>` is a `Reducer<Container<T>>`

### Combining flows into Reducer

Let's look at the example of combining flows into Reducer:

```kotlin
data class State(
    // collected from getNumberFlow(): Flow<Int>:
    val number: Int = 0,
    // collected from getTextFlow(): Flow<String>:
    val text: String = "",  
    // updated manually:
    val other: Boolean = false,
)

// combine flows to Reducer:
private val reducer: Reducer<State> = combineToReducer(
    flow1 = getNumberFlow(), // Flow<Int>
    flow2 = getTextFlow(),   // Flow<String>
    initialState = State(),
    nextState = State::copy,
    
    // 'scope' and 'started' can be omitted within ReducerOwner:
    scope = viewModelScope,
    started = SharingStarted.Lazily,
)
val stateFlow: StateFlow<State> = reducer.stateFlow
```

State is updated automatically whenever new values come from input flows using
implicit `copy()` method from data class in this case. Also, you can update your
state manually:

```kotlin
reducer.update { oldState ->
  oldState.copy(other = true)
}
```

### Combining flows into ContainerReducer

Sometimes you may need to wrap resulting State class into container to 
indicate loading status in your screen. For a such purposes, you can
convert Flows into ContainerReducer.

Example of using `combineToContainerReducer`:

```kotlin
data class State(
    val number: Int = 0, // from flow1
    val text: String = "", // from flow2
    val other: Boolean = false,
)

private val reducer: ContainerReducer<State> = combineToContainerReducer(
    flow1 = getIntFlow(), // Flow<Int>
    flow2 = getStringFlow(), // Flow<String>
    initialState = ::State,
    nextState = State::copy,
    scope = viewModelScope,
    started = SharingStarted.Lazily,
)
val stateFlow: StateFlow<Container<State>> = reducer.stateFlow
```

Example of simple converting of one origin flow:

```kotlin
data class State(
    val number: Int = 0,
    val other: Boolean = false,
)

private val reducer: ContainerReducer<State> = getIntFlow() // Flow<Int>
    .toContainerReducer(
        initialState = ::State,
        nextState = State::copy,
        scope = viewModelScope,
        started = SharingStarted.Lazily,
    )
val stateFlow: StateFlow<Container<State>> = reducer.stateFlow
```

By the way, `nextState` arg is optional upon creating a `ContainerReducer` instance, 
if you don't need to update your state class manually. Also, as mentioned above,
`scope` and `started` args may be also omitted within `ReducerOwner` implementations:

```kotlin
data class State(
    val number: Int = 0,
)

private val reducer: ContainerReducer<State> = getIntFlow() // Flow<Int>
    .toContainerReducer(initialState = ::State)
val stateFlow: StateFlow<Container<State>> = reducer.stateFlow
```

### Container Flows to Container Reducers

If your input flows have a type `Flow<Container<T>>`, you can use another two
functions for creating `ContainerReducers` that will mirror container status
from origin flows:

```kotlin
data class State(
    val number: Int = 0,
)

private val reducer: ContainerReducer<State> = getContainerIntFlow() // Flow<Container<Int>>
    .containerToReducer(initialState = ::State)
val stateFlow: StateFlow<Container<State>> = reducer.stateFlow
```

Or:

```kotlin
data class State(
    val number: Int = 0, // from flow1
    val text: String = "", // from flow2
)

private val reducer: ContainerReducer<State> = combineContainersToReducer(
    flow1 = getContainerIntFlow(), // Flow<Container<Int>>
    flow2 = getContainerStringFlow(), // Flow<Container<String>>
    initialState = ::State,
)
val stateFlow: StateFlow<Container<State>> = reducer.stateFlow
```

In case if you need to add additional properties to your state which are
not updated by input flows, you need to provide `nextState` arg:

```kotlin
data class State(
    val number: Int = 0, // from flow1
    val text: String = "", // from flow2
    val manuallyUpdatedProperty: Boolean = false,
)

private val reducer: ContainerReducer<State> = combineContainersToReducer(
    flow1 = getContainerIntFlow(), // Flow<Container<Int>>
    flow2 = getContainerStringFlow(), // Flow<Container<String>>
    initialState = ::State,
    nextState = State::copy,
)
val stateFlow: StateFlow<Container<State>> = reducer.stateFlow

reducer.updateState { oldState ->
    oldState.copy(manuallyUpdatedProperty = true)
}
```

## Extension functions for Container types

There are a couple of extension functions which can simplify code working 
with `Container<T>` instances:
- `fold`, `foldDefault`, `foldNullable`
- `transform`
- `map`, `mapException`
- `catch`, `catchAll`
- `getOrNull`, `exceptionOrNull`, `unwrap`

And ready-to-use integration with Kotlin Flows is available out of the box:

- `Flow<Container<T>>.containerMap()` converts the flow of type `Container<T>` into a flow
  of type `Container<R>`
- `StateFlow<Container<T>>.containerStateMap()` converts StateFlow of type `Container<T>` into a
  StateFlow of type `Container<R>`
- `Flow<Container<T>>.containerFilter()` filters all `Container.Success<T>` values by a given predicate

Other extension functions:
- `Flow<Container<T>>.containerFilterNot`
- `Flow<Container<T>>.containerMapLatest`
- `Flow<Container<T>>.containerFold`
- `Flow<Container<T>>.containerFoldDefault`
- `Flow<Container<T>>.containerFoldNullable`
- `Flow<Container<T>>.containerTransform`
- `Flow<Container<T>>.containerCatch`
- `Flow<Container<T>>.containerCatchAll`

Also, there are the following type aliases:
- `ListContainer<T>` = `Container<List<T>>`
- `ContainerFlow<T>` = `Flow<Container<T>>`
- `ListContainerFlow<T>` = `Flow<Container<List<T>>>`

Combine container flows:

```kotlin
val flow1: Flow<Container<String>> = ...
val flow2: Flow<Container<Int>> = ...
val combinedFlow: Flow<Container<String>> = 
    combineContainerFlows(flow1, flow2) { string, number ->
       "$string,$number"
    }
```

Combine a container flow with other flows:

```kotlin
val flow1: Flow<Container<String>> = ...
val flow2: Flow<Int> = ...
val combinedFlow: Flow<Container<String>> = flow1
    .containerCombineWith(flow2) { string, number ->
        "$string,$number"
    }
```

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

1. The load function is executed only when at least one subscriber starts collecting the flow
2. The load function can emit more than one value
3. The load function is cancelled when the last subscriber stops collecting the flow after timeout (default timeout = 1sec)
4. The latest result is cached, so any new subscriber can receive the
   most actual loaded value without triggering the load function again and again
5. There is a timeout (default value is 1sec) after the last subscriber
   stops collecting the flow. When timeout expires, the cached value is
   cleared so any further subscribers will execute the load function again
6. The flow can be collected by using `listen()` method
7. You can replace the load function at any time by using the following methods:
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
8. Also you can use `updateWith` method in order to cancel any active loader
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

#### Build-In Reload Function

All success and error containers have an additional property named `reloadFunction`.
By default, it is empty, but optionally you can configure the `LazyFlowSubject.listen()`
call with additional `ContainerConfiguration(emitReloadFunction = true)` argument. In this
case, calling `reloadFunction` causes a full reload of the corresponding `LazeFlowSubject`
instance (similar to call of `LazyFlowSubject.reloadAsync()`).

You can re-assign the reloading function by:

- creating a new container: `successContainer(value, reloadFunction = customReloadFunction)`

- mapping an existing container using `update` function:
  
  ```kotlin
  Container<T>.update { 
      reloadFunction = { 
          println("Reloading...")
          reloadFunction(it)  // call origin reload function if needed
      } 
  }
  ```
- mapping an existing container in a Kotlin Flow:

  ```kotlin
  val flow: Flow<Container<String>> = getFlow()
  return flow
      .containerUpdate {
          println("Reloading...")
          reloadFunction(it)  // call origin reload function if needed
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
