# Reducer Reference

`Reducer<State>` and `ContainerReducer<State>` are the ViewModel-layer
building blocks of `com.elveum:container:3.6.0`. They convert one or more
`Flow`s (plain or of `Container<T>`) into a hot `StateFlow` that a screen
observes, while still allowing manual, flow-independent updates on top -
things like a locally toggled filter that don't come from any repository.

See [`container-type.md`](container-type.md) for `Container<T>`, its three
states, `fold`, and `pendingContainer()` / `successContainer()`, used
throughout this file.

All symbols below live in package `com.elveum.container.reducer` unless noted
otherwise.

## 1. `Reducer` vs `ContainerReducer`

| Type                      | `stateFlow` type              | Use when |
|---------------------------|--------------------------------|----------|
| `Reducer<State>`          | `StateFlow<State>`             | The UI only needs a plain value, not loading/error status |
| `ContainerReducer<State>` | `StateFlow<Container<State>>`  | The UI needs to render `Pending` / `Error` / `Success` |

```kotlin
public interface Reducer<State> {
    public val stateFlow: StateFlow<State>
    public fun update(transform: suspend (State) -> State)
}

public interface ContainerReducer<State> : Reducer<Container<State>> {
    public override val stateFlow: StateFlow<Container<State>>
    public override fun update(transform: suspend (Container<State>) -> Container<State>)
    public fun updateState(transform: suspend (State) -> State)
}
```

`ContainerReducer<State>` **extends** `Reducer<Container<State>>` - its
`update` replaces the whole container. `updateState` is the extra member: it
edits the value wrapped inside `Container.Success` and is a no-op for
`Pending`/`Error`.

## 2. Creating a `Reducer`

### From a plain flow - `Flow<T>.toReducer`

```kotlin
private data class NumberState(val number: Int = 0, val label: String = "")

private fun toReducerExplicit(flow: Flow<Int>, scope: CoroutineScope): Reducer<NumberState> {
    return flow.toReducer(
        initialState = ::NumberState,
        nextState = { oldState, value -> oldState.copy(number = value) },
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
    )
}
```

- `initialState: () -> State` - a factory, evaluated once.
- `nextState: suspend (State, T) -> State` - merges each new flow value into
  the current state.
- If `State` and the flow's element type are the same, `nextState` can be
  omitted; the single-argument overload replaces the whole state with each
  new value.

### From a flow of containers

`toReducer` has no special container-unwrapping overload - unwrap inside
`nextState` yourself:

```kotlin
private fun fromContainerFlow(
    flow: Flow<Container<Int>>,
    scope: CoroutineScope,
): Reducer<NumberState> {
    return flow.toReducer(
        initialState = ::NumberState,
        nextState = { oldState, container ->
            oldState.copy(number = container.getOrNull() ?: oldState.number)
        },
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
    )
}
```

If you want the container's `Pending`/`Error`/`Success` state to drive the
resulting `StateFlow` instead, use a `ContainerReducer` (section 3) rather
than unwrapping by hand.

## 3. Creating a `ContainerReducer`

### From a plain flow - `Flow<T>.toContainerReducer`

Emits `pendingContainer()` until the source flow produces its first value,
then wraps every value in `successContainer(...)`:

```kotlin
private fun toContainerReducerExplicit(
    flow: Flow<Int>,
    scope: CoroutineScope,
): ContainerReducer<NumberState> {
    return flow.toContainerReducer(
        initialState = { value -> NumberState(number = value) },
        nextState = { oldState, value -> oldState.copy(number = value) },
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
    )
}
```

- `initialState: suspend (T) -> State` - builds the state from the first (or
  first-after-Pending/Error) value.
- `nextState: suspend (State, T) -> State` - defaults to calling
  `initialState` again, so it can be omitted if there is nothing to merge.

### From a flow of containers - `Flow<Container<T>>.containerToReducer`

The container states are mirrored directly into the result: an upstream
`Pending`/`Error` produces the same `Pending`/`Error` in `stateFlow`, and
`nextState` only ever runs between two `Success` values.

```kotlin
private fun containerToReducerExplicit(
    flow: Flow<Container<Int>>,
    scope: CoroutineScope,
): ContainerReducer<NumberState> {
    return flow.containerToReducer(
        initialState = { value -> NumberState(number = value) },
        nextState = { oldState, value -> oldState.copy(number = value) },
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
    )
}
```

`initialState`/`nextState` have the same shapes and the same optionality as
`toContainerReducer` above.

## 4. Combining flows

Three families of combine functions exist, one per reducer/source shape.
Each has overloads for 2 through 5 named flows (`flow1, flow2, ...`), plus a
list-based overload (`flows: Iterable<Flow<*>>` / `Iterable<Flow<Container<*>>>`)
for an arbitrary number, where `nextState`/`initialState` receive a `List<*>`
that must be cast positionally.

| Function | Source flows | Result |
|----------|---------------|--------|
| `combineToReducer` | plain `Flow<T1>, Flow<T2>, ...` | `Reducer<State>` |
| `combineToContainerReducer` | plain `Flow<T1>, Flow<T2>, ...` | `ContainerReducer<State>` |
| `combineContainersToReducer` | `Flow<Container<T1>>, Flow<Container<T2>>, ...` | `ContainerReducer<State>` |

```kotlin
// combineToReducer - initialState is () -> State, nextState is required.
private fun combineToReducerExplicit(
    flow1: Flow<Int>,
    flow2: Flow<String>,
    scope: CoroutineScope,
): Reducer<NumberState> {
    return combineToReducer(
        flow1,
        flow2,
        initialState = ::NumberState,
        nextState = { _, number, label -> NumberState(number, label) },
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
    )
}

// combineToContainerReducer - initialState is (T1, T2, ...) -> State,
// nextState defaults to calling initialState again.
private fun combineToContainerReducerExplicit(
    flow1: Flow<Int>,
    flow2: Flow<String>,
    scope: CoroutineScope,
): ContainerReducer<NumberState> {
    return combineToContainerReducer(
        flow1,
        flow2,
        initialState = { number, label -> NumberState(number, label) },
        nextState = { _, number, label -> NumberState(number, label) },
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
    )
}

// combineContainersToReducer - flows are Flow<Container<T>>; a single
// Pending/Error input makes the combined result Pending/Error, same rule
// as combineContainerFlows (see container-type.md section 7).
private fun combineContainersToReducerExplicit(
    flow1: Flow<Container<Int>>,
    flow2: Flow<Container<String>>,
    scope: CoroutineScope,
): ContainerReducer<NumberState> {
    return combineContainersToReducer(
        flow1,
        flow2,
        initialState = { number, label -> NumberState(number, label) },
        nextState = { _, number, label -> NumberState(number, label) },
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
    )
}
```

`combineToReducer`'s `initialState` is a plain `() -> State` factory (same
shape as `toReducer`'s), while `combineToContainerReducer`'s and
`combineContainersToReducer`'s `initialState` takes the flows' values
(`(T1, T2, ...) -> State`, same shape as `toContainerReducer`'s) - the naming
is shared across all three families but the signature follows the reducer
kind, not the function name.

## 5. Manual updates

```kotlin
// Reducer<State>:
reducer.update { oldState -> oldState.copy(filter = "book") }

// ContainerReducer<State>: replace the whole container...
containerReducer.update { oldContainer -> successContainer(someNewState) }
// ...or update just the wrapped value (no-op unless the current container is Success):
containerReducer.updateState { oldState -> oldState.copy(filter = "book") }
```

`update`/`updateState`'s transform lambdas are `suspend`, so they may call
suspend functions directly; both are safe to call from any coroutine.

### Mixing flow-driven fields with manually updated ones

This is where reducers are most often written wrong. When part of the state
comes from the origin flow and part of it is set locally through `update { }`,
`nextState` **must build on `oldState`**. `nextState` runs on every upstream
emission and its return value replaces the whole state - so a `nextState` that
constructs a fresh state from the flow value alone silently wipes every manual
field each time the repository emits:

```kotlin
private data class BooksState(
    val books: List<String> = emptyList(),
    val query: String = "",   // set locally, never comes from the flow
)

// BROKEN - `query` is reset to "" on every emission of booksFlow:
private fun brokenBooksReducer(
    booksFlow: Flow<List<String>>,
    scope: CoroutineScope,
): Reducer<BooksState> {
    return booksFlow.toReducer(
        initialState = ::BooksState,
        nextState = { _, books -> BooksState(books = books) },
        scope = scope,
        started = SharingStarted.Eagerly,
    )
}

// CORRECT - copy the new flow value onto the state that is already there:
private fun booksReducer(
    booksFlow: Flow<List<String>>,
    scope: CoroutineScope,
): Reducer<BooksState> {
    return booksFlow.toReducer(
        initialState = ::BooksState,
        nextState = { oldState, books -> oldState.copy(books = books) },
        scope = scope,
        started = SharingStarted.Eagerly,
    )
}
```

```kotlin
private fun compareBooksReducers(scope: CoroutineScope) {
    val booksFlow = MutableStateFlow(listOf("Dune"))

    val broken = brokenBooksReducer(booksFlow, scope)
    broken.update { oldState -> oldState.copy(query = "kotlin") }
    booksFlow.value = listOf("Dune", "Neuromancer")
    broken.stateFlow.value.query   // "" - the local edit is gone

    val correct = booksReducer(booksFlow, scope)
    correct.update { oldState -> oldState.copy(query = "kotlin") }
    booksFlow.value = listOf("Dune", "Neuromancer", "Solaris")
    correct.stateFlow.value.query  // "kotlin" - preserved
}
```

(`update { }` dispatches onto the reducer's `scope`, so the commented-out values
are what you read once each step has actually been applied; a test makes that
deterministic with the `UnconfinedTestDispatcher` setup from `testing.md`
section 7, which runs the reducer's dispatched work inline so no explicit
advancing between steps is needed.)

The failure is easy to miss because the broken version looks right whenever you
test it without a manual update in flight, and the state loss only shows up
when an upstream emission happens to land after a user action. Two rules keep
it away:

- in `nextState`, always start from `oldState` and `copy` only the fields the
  flow owns. The `_` placeholder for `oldState` is a red flag unless the flow
  owns every field of the state.
- the same applies to `combineToReducer` and friends (section 4), whose
  `nextState` takes `(oldState, value1, value2, ...)`: their `initialState` may
  ignore the old state, but `nextState` may not.

**`nextState`'s default is the broken form.** On `toContainerReducer`,
`containerToReducer`, `combineToContainerReducer` and
`combineContainersToReducer`, `nextState` is optional and defaults to
`{ oldState, newValue -> initialState(newValue) }` - which discards `oldState`.
Section 3 describes that default as "can be omitted if there is nothing to
merge", and a manually updated field *is* something to merge. As soon as any
field of the state is set by `update`/`updateState` rather than by the flow,
pass an explicit `nextState`.

There is one more reset that `nextState` cannot guard against:
`containerToReducer` and `combineContainersToReducer` call `initialState`, never
`nextState`, whenever the previous state container was `Pending` or `Error`
(that is the mirroring rule from section 3). So a reload under the default
`LoadConfig.Normal`, which sends the upstream container back through
`Container.Pending` (`subjects.md` section 3), rebuilds the state from
`initialState` and loses manual `updateState` edits even with a correct
`nextState`. Either keep locally-owned fields in a separate `Reducer` or 
`MutableStateFlow` from the container-backed one, or reload with
`LoadConfig.SilentLoading` so the upstream container never leaves `Success`.

The `-> State` shape is the reason none of this is automatic: `nextState` is a
full replacement, not a patch. `initialState` is the only lambda that
legitimately builds a state from nothing.

## 6. `ReducerOwner`

```kotlin
public interface ReducerOwner {
    public val reducerCoroutineScope: CoroutineScope
    public val reducerSharingStarted: SharingStarted
}
```

Implementing it once lets every reducer-creating call below omit `scope` and
`started`. A typical base class:

```kotlin
abstract class AbstractViewModel : ViewModel(), ReducerOwner {
    override val reducerCoroutineScope: CoroutineScope = viewModelScope
    override val reducerSharingStarted: SharingStarted = SharingStarted.WhileSubscribed(
        stopTimeoutMillis = 1000,
        replayExpirationMillis = 1000,
    )
}
```

Every concrete ViewModel then extends `AbstractViewModel` instead of `ViewModel()`
directly, and drops `scope`/`started` from its own reducer calls (subject to
the compiler-flag boundary in section 7).

### Choosing `reducerSharingStarted` (and the `started` of any `stateIn`)

`SharingStarted.WhileSubscribed(...)` is the right default when the origin flow
is a plain cold flow that keeps costing something for as long as it is
collected - a database query, a `callbackFlow` over a sensor or socket, a
polling flow. Stopping that collection while the UI is not observing is the
whole purpose of `WhileSubscribed`.

When the origin flow comes from a `LazyFlowSubject` (`listen()` /
`listenReloadable()`) or a `LazyCache` (`listen(arg)` / `listenReloadable(arg)`),
that work is already done one layer down, and `SharingStarted.Lazily` is enough -
it is what `ReducerOwner`'s own KDoc uses. The subject counts the collectors of
its `listen()` flow itself (`activeCollectorsCount`), starts the load on the
first one, and once the last one leaves it waits `cacheTimeoutMillis` and then
cancels its loader scope and drops the cached value (`subjects.md` section 2).
Wrapping `WhileSubscribed` around that is a second copy of the same
reference-counting, with a second timeout to reason about.

With `Lazily` the reducer starts collecting at its first `stateFlow` subscriber
and then holds that collection for the life of `reducerCoroutineScope`. When
that scope is `viewModelScope`, the subject sees exactly one collector for
exactly the ViewModel's lifetime: no reload on rotation or on return from the
background, and a clean release when the ViewModel is cleared and the scope is
cancelled.

Two concrete costs of `WhileSubscribed` over a subject-backed flow:

- every stop/restart cycle (a rotation, or a backgrounded app outliving
  `stopTimeoutMillis`) takes the subject down to zero collectors and starts
  *its* cache timeout; if that expires, the next subscriber pays for a full
  reload of data the ViewModel still had.
- `WhileSubscribed(replayExpirationMillis = ...)` - as in the
  `AbstractViewModel` above - emits `SharingCommand.STOP_AND_RESET_REPLAY_CACHE`
  once the replay expires, and the reducer answers it by putting `stateFlow`
  back to `initialState`. Manual `update { }` edits (section 5) live in that
  same state and are reset with it. `Lazily` never resets.

So: `Lazily` when every input flow is subject- or cache-backed;
`WhileSubscribed` when some input is a cold flow whose collection genuinely has
to stop. The same reasoning applies to the `started` argument of `stateIn` /
`shareIn` and to the explicit `started` parameter of every function in
sections 2-4.

## 7. The `-Xcontext-parameters` boundary

Every function in sections 2-4 above has two forms: an explicit
`scope, started` form (always compiles) and a shorter form that reads `scope`
and `started` off a `ReducerOwner` instead. Which mechanism the shorter form
uses - and therefore whether it needs a compiler flag - depends on the
function family, and the two do **not** line up the way their similar names
suggest:

| Family | Shorthand mechanism | Needs `-Xcontext-parameters`? |
|--------|----------------------|-------------------------------|
| `combineToReducer`, `combineToContainerReducer`, `combineContainersToReducer` (all arities) | `ReducerOwner.` **extension-receiver** overload | No - plain Kotlin extension function, resolves via `this` in any `ReducerOwner` subclass |
| `Flow<T>.toReducer`, `Flow<T>.toContainerReducer`, `Flow<Container<T>>.containerToReducer` | `context(owner: ReducerOwner)` overload | Yes |
| `Flow<T>.stateIn(initialValue)`, `Flow<T>.shareIn(replay)` | `context(owner: ReducerOwner)` | Yes |

So `combineToReducer(flow1, flow2, initialState = ..., nextState = ...)`
(no `scope`/`started`) works inside any `ReducerOwner` with no build-script
change - it is defined as `fun <T1, T2, State> ReducerOwner.combineToReducer(...)`
in `CombineToReducer.kt`, an ordinary extension function, not a context
parameter. But `someFlow.toReducer(initialState, nextState)` with no
`scope`/`started` only resolves if the module enables:

```kotlin
// build.gradle.kts
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
```

The library itself enables this flag, so every shorthand form compiles
inside it - that is not evidence a downstream module without the flag can
use the `toReducer`/`toContainerReducer`/`containerToReducer`/`stateIn`/
`shareIn` shorthands. Without the flag, use the
explicit `scope`/`started` form shown in sections 2-3 for those functions;
the `combineTo*` family's `ReducerOwner` shorthand needs no flag either way.

`stateIn`/`shareIn` also need an explicit import even when the flag is
enabled: `kotlinx.coroutines.flow` declares plain (non-context) functions
with the same names, and an unqualified call resolves to those instead of
the `ReducerOwner`-context ones unless you import
`com.elveum.container.reducer.stateIn` / `com.elveum.container.reducer.shareIn`
explicitly.

## 8. Public-interface / private-implementation state pattern

Keep a ViewModel's exposed `State` narrow by typing the reducer to a private
implementation class and exposing only a public interface. No mapping step
is needed because `StateFlow` is declaration-site covariant in its type
parameter, so `StateFlow<StateImpl>` is already a `StateFlow<State>`:

```kotlin
// Public interface - only what the UI needs:
private interface TreeState {
    val expandedCount: Int
}

// Private implementation - holds the full internal state:
private data class TreeStateImpl(
    val expandedNodes: Set<Long> = emptySet(),
) : TreeState {
    override val expandedCount: Int
        get() = expandedNodes.size
}

private class TreeModel(
    scope: CoroutineScope,
    nodesFlow: Flow<Set<Long>>,
) : ReducerOwner {

    override val reducerCoroutineScope: CoroutineScope = scope
    override val reducerSharingStarted: SharingStarted = SharingStarted.WhileSubscribed(
        stopTimeoutMillis = 1000,
        replayExpirationMillis = 1000,
    )

    private val reducer: Reducer<TreeStateImpl> = nodesFlow.toReducer(
        initialState = { TreeStateImpl() },
        nextState = { oldState, nodes -> oldState.copy(expandedNodes = nodes) },
    )

    // StateFlow<TreeStateImpl> is a StateFlow<TreeState> via declaration-site variance.
    val stateFlow: StateFlow<TreeState> = reducer.stateFlow

    fun toggle(id: Long) = reducer.update { oldState ->
        val newExpanded = if (oldState.expandedNodes.contains(id)) {
            oldState.expandedNodes - id
        } else {
            oldState.expandedNodes + id
        }
        oldState.copy(expandedNodes = newExpanded)
    }
}
```

The reducer is typed to `TreeStateImpl` throughout (`update`'s transform
lambda receives and returns `TreeStateImpl`), and only the exposed
`stateFlow` property widens to the public `TreeState` interface. The same
pattern applies against `ContainerReducer` instead of `Reducer`.

`TreeModel` implements `ReducerOwner` directly here, rather than extending a
shared base class, so this example is self-contained. In an app with several
such classes, pulling the `reducerCoroutineScope`/`reducerSharingStarted`
overrides into one shared base - the same `AbstractViewModel` from section 6
- is the usual choice; `TreeModel` would then extend that instead of
implementing `ReducerOwner` itself, with no other change to this pattern.
