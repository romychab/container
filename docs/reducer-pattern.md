# Reducer Pattern

Reducers make state management easy by letting you convert one or more Kotlin
Flows into a single `StateFlow` while keeping the ability to apply manual
updates on top. This page covers everything from basic usage to advanced
patterns.

## Table of Contents

- [Reducer vs ContainerReducer](#reducer-vs-containerreducer)
- [Creating a Reducer](#creating-a-reducer)
  - [From a Plain Flow](#from-a-plain-flow)
  - [From a Flow of Containers](#from-a-flow-of-containers)
- [Creating a ContainerReducer](#creating-a-containerreducer)
  - [From a Plain Flow](#from-a-plain-flow-1)
  - [From a Flow of Containers](#from-a-flow-of-containers-1)
- [Combining Multiple Flows](#combining-multiple-flows)
  - [combineToReducer](#combinetoreducer)
  - [combineToContainerReducer](#combinetocontainerreducer)
  - [combineContainersToReducer](#combinecontainerstoreducer)
- [Manual State Updates](#manual-state-updates)
- [ReducerOwner Interface](#reducerowner-interface)
  - [Setting Up AbstractViewModel](#setting-up-abstractviewmodel)
  - [Simplified Reducer Creation](#simplified-reducer-creation)
- [Public Interface / Private Implementation Pattern](#public-interface--private-implementation-pattern)
- [API Reference](#api-reference)

## Reducer vs ContainerReducer

The library provides two reducer types:

| Type                      | `stateFlow` type              | Use when |
|---------------------------|-------------------------------|----------|
| `Reducer<State>`          | `StateFlow<State>`            | The UI only needs to see a plain value, not loading/error status |
| `ContainerReducer<State>` | `StateFlow<Container<State>>` | The UI needs to handle `Pending`, `Error`, and `Success` states |

Both types expose a `stateFlow` property and an `update` method. In addition,
`ContainerReducer` has an `updateState` method that modifies the unwrapped
value inside a `Container.Success` (and is a no-op for other states).

## Creating a Reducer

### From a Plain Flow

Convert any `Flow<T>` to a `Reducer<State>` by specifying an initial state
and a `nextState` function that merges the latest flow value into your state:

```kotlin
data class State(
    val items: List<String> = emptyList(),
    val filterEnabled: Boolean = false,
)

private val reducer: Reducer<State> = getItemsFlow() // Flow<List<String>>
    .toReducer(
        initialState = State(),
        nextState    = State::copy,  // (state, items) -> state.copy(items = items)
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5000),
    )

val stateFlow: StateFlow<State> = reducer.stateFlow
```

The `nextState` parameter is a function of type `(State, T) -> State`, which
`data class`'s generated `copy(items = ...)` satisfies automatically as a
method reference when the fields match.

If your state class _is_ the same type as the flow value (no wrapping needed),
you can omit `nextState`:

```kotlin
private val reducer: Reducer<List<String>> = getItemsFlow()
    .toReducer(
        initialState = emptyList(),
        scope        = viewModelScope,
        started      = SharingStarted.Lazily,
    )
```

### From a Flow of Containers

Use `toReducer` and unwrap the input container inside `nextState` if you want
to convert `Flow<Container<T>>` into a plain `StateFlow<State>` manually:

```kotlin
data class State(
    val user: User = User.Anonymous,
)

private val reducer: Reducer<State> = usersRepository.getCurrentUser() // Flow<Container<User>>
    .toReducer(
        initialState = State(),
        nextState    = { oldState, container ->
            oldState.copy(user = container.getOrNull() ?: User.Anonymous)
        },
        scope   = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
    )
```

## Creating a ContainerReducer

### From a Plain Flow

`toContainerReducer` wraps the flow values inside a `Container.Success`
automatically and emits `Container.Pending` until the first value arrives:

```kotlin
data class State(
    val number: Int = 0,
    val extra: Boolean = false,
)

private val reducer: ContainerReducer<State> = getIntFlow() // Flow<Int>
    .toContainerReducer(
        initialState = ::State,   // () -> State (constructor reference)
        nextState    = State::copy,
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5000),
    )

val stateFlow: StateFlow<Container<State>> = reducer.stateFlow
```

If you don't need to update the state manually, `nextState` is optional:

```kotlin
private val reducer: ContainerReducer<State> = getIntFlow()
    .toContainerReducer(
        initialState = ::State,
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5000),
    )
```

### From a Flow of Containers

When the source already emits `Container<T>`, use `containerToReducer`:

```kotlin
data class State(
    val items: List<Item>,
    val extra: Boolean = false,
)

private val reducer: ContainerReducer<State> =
    itemsRepository.getItems() // Flow<Container<List<Item>>>
        .containerToReducer(
            initialState = ::State,
            nextState    = State::copy,
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
        )
```

The container states (`Pending`, `Error`, `Success`) are mirrored directly
into the resulting `StateFlow<Container<State>>`.

## Combining Multiple Flows

### combineToReducer

Combine two or more flows into a single `Reducer<State>`. Each flow value
is merged into the state using the provided `nextState` function:

```kotlin
data class State(
    val number: Int = 0,    // from flow1
    val text: String = "",   // from flow2
    val local: Boolean = false, // updated manually
)

private val reducer: Reducer<State> = combineToReducer(
    flow1        = getNumberFlow(),   // Flow<Int>
    flow2        = getTextFlow(),     // Flow<String>
    initialState = State(),
    nextState    = State::copy,
    scope        = viewModelScope,
    started      = SharingStarted.WhileSubscribed(5000),
)
```

Overloads exist for up to five input flows. For more flows, use the list
overload:

```kotlin
private val reducer: Reducer<State> = combineToReducer(
    flows        = listOf(flow1, flow2, flow3),
    initialState = State(),
    nextState    = { state, values -> state.copy(...) },
    scope        = viewModelScope,
    started      = SharingStarted.Lazily,
)
```

### combineToContainerReducer

Same as above, but wraps the result in `Container<State>`:

```kotlin
private val reducer: ContainerReducer<State> = combineToContainerReducer(
    flow1        = getIntFlow(),    // Flow<Int>
    flow2        = getStringFlow(), // Flow<String>
    initialState = ::State,
    nextState    = State::copy,
    scope        = viewModelScope,
    started      = SharingStarted.WhileSubscribed(5000),
)
```

The resulting `StateFlow<Container<State>>` emits `Pending` until all input
flows have produced at least one value.

### combineContainersToReducer

When the input flows are themselves `Flow<Container<T>>`, use
`combineContainersToReducer`. Container states are merged using the same
rules as `combineContainerFlows`: a single `Pending` or `Error` input
makes the result `Pending` or `Error` respectively:

```kotlin
data class State(
    val number: Int = 0,  // from flow1: Flow<Container<Int>>
    val text: String = "", // from flow2: Flow<Container<String>>
)

private val reducer: ContainerReducer<State> = combineContainersToReducer(
    flow1        = getContainerIntFlow(), // Flow<Container<Int>>
    flow2        = getContainerStringFlow(), // Flow<Container<String>>
    initialState = ::State,
    nextState    = State::copy, // optional, omit if no manual updates needed
    scope        = viewModelScope,
    started      = SharingStarted.WhileSubscribed(5000),
)
```

## Manual State Updates

All reducer types provide an `update` method for applying changes that don't
come from the source flow:

```kotlin
// Reducer<State>:
reducer.update { oldState -> oldState.copy(filterEnabled = true) }

// ContainerReducer<State>: update the whole container:
reducer.update { oldContainer -> successContainer(someNewState) }

// ContainerReducer<State>: update just the value (no-op if not Success):
reducer.updateState { oldState -> oldState.copy(isLoading = false) }
```

Manual updates and flow emissions are safe to call from any coroutine.

## ReducerOwner Interface

Supplying `scope` and `started` to every reducer constructor is repetitive.
The `ReducerOwner` interface lets you define them once and then omit them at
every call site.

To make it work, enabling Kotlin Context Parameters first is required for the
context-parameter overloads:

```kotlin
// build.gradle
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
```

### Setting Up AbstractViewModel

Create an abstract base ViewModel that implements `ReducerOwner`:

```kotlin
abstract class AbstractViewModel : ViewModel(), ReducerOwner {
    override val reducerCoroutineScope: CoroutineScope
        get() = viewModelScope
    override val reducerSharingStarted: SharingStarted
        get() = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000)
}
```

### Simplified Reducer Creation

Once `AbstractViewModel` is in place, child view-models no longer need to
pass `scope` or `started`:

```kotlin
class ItemListViewModel @Inject constructor(
    private val itemsRepository: ItemsRepository,
) : AbstractViewModel() {

    data class State(val items: List<Item> = emptyList())

    private val reducer: ContainerReducer<State> =
        itemsRepository.getItems()          // Flow<Container<List<Item>>>
            .containerToReducer(
                initialState = ::State,
                nextState    = State::copy,
            )

    val stateFlow: StateFlow<Container<State>> = reducer.stateFlow

    fun deleteItem(id: Long) {
        reducer.updateState { it.copy(items = it.items.filter { item -> item.id != id }) }
    }
}
```

The same shorthand is available for all combining functions:

```kotlin
// within a ReducerOwner (e.g. AbstractViewModel and its children):
private val reducer = combineToContainerReducer(
    flow1        = getIntFlow(),
    flow2        = getStringFlow(),
    initialState = ::State,
    nextState    = State::copy,
)
```

Additionally, `ReducerOwner` provides short variants of `stateIn` and
`shareIn`:

```kotlin
val derived: StateFlow<String> = someFlow.stateIn(initialValue = "")
val shared: SharedFlow<String> = someFlow.shareIn(replay = 1)
```

## Public Interface / Private Implementation Pattern

A useful pattern for keeping your ViewModel's public API clean is to separate
the public `State` interface from a private implementation class. The
implementation holds all private fields and computes the public properties
on the fly, so no mapping step is required:

```kotlin
// Public interface - only expose what the UI needs:
interface State {
    val displayItems: List<String>
}

// Private implementation - holds full internal state:
private data class StateImpl(
    val rawItems: List<String> = emptyList(),
    val isUppercase: Boolean = false,
) : State {
    override val displayItems: List<String>
        get() = if (isUppercase) rawItems.map { it.uppercase() } else rawItems
}

// Reducer typed to the private class, but the public StateFlow uses the interface:
private val reducer: Reducer<StateImpl> = getItemsFlow()
    .toReducer(
        initialState = StateImpl(),
        nextState    = StateImpl::copy,
    )

val stateFlow: StateFlow<State> = reducer.stateFlow  // StateImpl is a State
```

This approach works because `StateImpl` implements `State`, so
`StateFlow<StateImpl>` is a `StateFlow<State>` via Kotlin's type variance.

## API Reference

### Reducer

```
Reducer<State>
  stateFlow: StateFlow<State>
  update(transform: (State) -> State)
```

### ContainerReducer

```
ContainerReducer<State>  // extends Reducer<Container<State>>
  stateFlow: StateFlow<Container<State>>
  update(transform: (Container<State>) -> Container<State>)
  updateState(transform: (State) -> State) // no-op if not Success
```

### Flow to Reducer conversion functions

```
Flow<T>.toReducer(initialState, nextState?, scope, started)           -> Reducer<State>
Flow<T>.toContainerReducer(initialState?, nextState?, scope, started) -> ContainerReducer<State>
Flow<Container<T>>.containerToReducer(initialState?, nextState?, scope, started) -> ContainerReducer<State>
```

### Combine functions

```
combineToReducer(flow1, flow2, ..., initialState, nextState, scope, started)            -> Reducer<State>
combineToContainerReducer(flow1, flow2, ..., initialState, nextState?, scope, started)  -> ContainerReducer<State>
combineContainersToReducer(flow1, flow2, ..., initialState, nextState?, scope, started) -> ContainerReducer<State>
```

All combine functions have overloads for 2–5 input flows and a list-based
overload for an arbitrary number.

When used inside a `ReducerOwner`, `scope` and `started` can be omitted from
all of the above functions.
