# Store Results

`StoreResult<T>` is the sealed class emitted by all stores. This page
covers its states, value extraction, transformations, combining multiple
stores, and converting store flows into UI state with
`StoreResultReducer`.

## Table of Contents

- [States](#states)
- [Checking the State](#checking-the-state)
- [Metadata](#metadata)
- [Transformations](#transformations)
  - [map / storeMap](#map--storemap)
  - [storeFlatMapResultLatest / storeFlatMapLatest](#storeflatmapresultlatest--storeflatmaplatest)
  - [storeListFlatMapLatest](#storelistflatmaplatest)
  - [filterLoaded](#filterloaded)
- [Reading the Latest Value Synchronously](#reading-the-latest-value-synchronously)
- [Combining Stores](#combining-stores)
- [StoreResultReducer](#storeresultreducer)
  - [Creating a Reducer](#creating-a-reducer)
  - [Combining Stores into a Reducer](#combining-stores-into-a-reducer)
  - [Manual State Updates](#manual-state-updates)
  - [ReducerOwner](#reducerowner)

## States

```kotlin
public sealed class StoreResult<out T> {
    public data object Loading : StoreResult<Nothing>()
    public sealed class Completed<T> : StoreResult<T>()
    public data class Loaded<T>(val value: T, ...) : Completed<T>()
    public data class Failed(val exception: Exception, ...) : Completed<Nothing>()
}
```

- **`StoreResult.Loading`** - no data is available in the in-memory cache;
  the store is loading it from the configured sources
- **`StoreResult.Loaded<T>`** - data has been loaded successfully and is
  available as `value`
- **`StoreResult.Failed`** - the load failed; the cause is available as
  `exception`

`Loaded` and `Failed` share the supertype `StoreResult.Completed<T>`,
which is useful when you only care whether the load has finished.

Handle all states with a `when` expression:

```kotlin
when (val result = storeResult) {
    StoreResult.Loading -> showProgressBar()
    is StoreResult.Failed -> showError(result.exception)
    is StoreResult.Loaded -> showData(result.value)
}
```

## Checking the State

A set of extension functions makes state checks concise. All of them use
Kotlin contracts, so smart-casts work after the check:

```kotlin
if (result.isLoaded()) {
    println(result.value) // smart-cast to StoreResult.Loaded
}

if (result.isFailed()) {
    println(result.exception) // smart-cast to StoreResult.Failed
}
```

| Function                | Returns `true` when                                         |
|-------------------------|-------------------------------------------------------------|
| `isLoaded()`            | The result is `Loaded`                                      |
| `isFailed()`            | The result is `Failed`                                      |
| `isCompleted()`         | The result is `Loaded` or `Failed`                          |
| `isForegroundLoading()` | The result is `Loading` (shown instead of content)          |
| `isBackgroundLoading()` | The result is completed, but a background reload is running |
| `hasAnyLoading()`       | Either foreground or background loading is in progress      |
| `getOrNull()`           | returns `value` for `Loaded`, otherwise `null`              |
| `failureOrNull()`       | returns `exception` for `Failed`, otherwise `null`          |

`isBackgroundLoading()` is the standard way to drive a pull-to-refresh
indicator while the old content stays on screen (see
[Load Requests](load-requests.md)).

## Metadata

Every result carries a `metadata: ContainerMetadata` object (shared with
the Container library) and exposes shortcuts to its common entries:

| Property              | Type                  | Description                                                                   |
|-----------------------|-----------------------|-------------------------------------------------------------------------------|
| `sourceType`          | `SourceType`          | Origin of the value: `LocalSourceType`, `RemoteSourceType`, etc.              |
| `backgroundLoadState` | `BackgroundLoadState` | State of a background reload running behind the cached value                  |
| `nextPageState`       | `PageState`           | Paged stores only: state of the next-page load (`Idle` / `Pending` / `Error`) |
| `totalPagedItemsCount`| `Int`                 | Paged stores only: total item count reported via `PagedList(totalCount = …)`; `-1` when unknown |

For example, with a local + remote store you can show a "data may be
stale" hint while the value loaded from the local storage is being
refreshed:

```kotlin
if (result.isLoaded() && result.sourceType == LocalSourceType) {
    ShowStaleDataBadge()
}
```

### Acting on the origin store through a result

The same metadata lets you act on the store that emitted a result without
holding a reference to the store itself - handy when all the UI has is the
`StoreResult`:

| Function                       | Effect                                                                                                |
|--------------------------------|-------------------------------------------------------------------------------------------------------|
| `invalidate(config? = null)`   | Reload the origin store. With `config = null` (the default) each observer keeps the load config it is already using; pass a `LoadConfig` to override it for this reload |
| `onItemRendered(index)`        | Paged stores only: report that the item at `index` was rendered, which may trigger the next-page load |

Because the result can reload itself, pull-to-refresh and "try again" usually
need **no** ViewModel/repository function at all - the UI wires the gesture
straight to `result::invalidate` (combine it with `LoadRequest.Silent` to keep
the current content visible during a pull-to-refresh; see
[Load Requests](load-requests.md)).

```kotlin
// retry button rendered from a Failed result, without a store reference:
Button(onClick = { result.invalidate() }) { Text("Try again") }

// paged list driven purely by the result it renders:
itemsIndexed(result.value) { index, item ->
    LaunchedEffect(index) { result.onItemRendered(index) }
    ItemCard(item)
}
```

## Transformations

### map / storeMap

`map` converts the value inside a single `StoreResult`; `Loading` and
`Failed` pass through unchanged. If the mapper throws, the result becomes
`Failed`:

```kotlin
val nameResult: StoreResult<String> = profileResult.map { it.name }
```

`storeMap` is the same operation applied to a whole flow:

```kotlin
fun isInCart(productId: Long): Flow<StoreResult<Boolean>> {
    return getCart().storeMap { cart ->
        cart.any { it.productId == productId }
    }
}
```

### storeFlatMapResultLatest / storeFlatMapLatest

`storeFlatMapResultLatest` observes the latest result and, for each
`Loaded` value, switches to a new inner flow of store results. Use it for
data that depends on previously loaded data (basic info + details, 1-N
relations):

```kotlin
fun getOrdersOfCurrentUser(): Flow<StoreResult<List<Order>>> {
    return userStore.observe()
        .storeFlatMapResultLatest { user -> ordersStore.observe(user.id) }
}
```

`storeFlatMapLatest` is the same, but for inner flows emitting plain
values instead of `StoreResult` - each emission is wrapped into
`StoreResult.Loaded` automatically.

### storeListFlatMapLatest

For flows of lists where each item needs extra data observed per item
(usually from a [keyed store](keyed-store.md)):

```kotlin
fun getFullCart(): Flow<StoreResult<List<CartProductItem>>> {
    return cartStore
        .observe()
        .storeListFlatMapLatest(
            observer = { cartItem -> productStore.observe(cartItem.productId) },
            mapper = { cartItem, productResult ->
                CartProductItem(cartItem.productId, productResult.getOrNull(), cartItem.quantity)
            },
        )
}
```

The outer list is emitted as soon as it is loaded. For every item, the
`observer` flow is collected, and each emission re-runs `mapper` and
re-emits the merged list. The `mapper` receives the item's `StoreResult`,
so the UI can render placeholders (`getOrNull()` returns `null`) until
each item's data arrives.

### filterLoaded

`filterLoaded()` reduces a `Flow<StoreResult<T>>` to a `Flow<T>` by keeping
only `Loaded` results and unwrapping their values; `Loading` and `Failed`
emissions are dropped. Use it when a consumer only cares about successfully
loaded values:

```kotlin
val names: Flow<String> = userStore.observe()
    .filterLoaded()      // Flow<UserProfile>
    .map { it.name }     // Flow<String>
```

## Awaiting a Single Result

When you need a one-shot value rather than an ongoing observation,
`firstGetOrThrow()` suspends until the flow produces a completed result
(`Loaded` or `Failed`), skipping intermediate `Loading` emissions. It
returns the loaded value or re-throws the failure's exception:

```kotlin
suspend fun loadProfileOnce(): UserProfile {
    return store.observe().firstGetOrThrow()
}
```

## Optional Values

Store value types are non-nullable (`T : Any`), so an entity that may not
exist is modelled as `java.util.Optional<T>` - e.g.
`SimpleStore<Optional<UserProfile>>`. A set of helpers makes such stores
ergonomic to work with:

| Function                                   | Receiver                          | Behaviour                                                                      |
|--------------------------------------------|-----------------------------------|--------------------------------------------------------------------------------|
| `T?.toOptional()`                          | any nullable value                | Wraps a value into an `Optional` (`null` → empty, non-null → present). Handy when fetching into an `Optional`-typed store |
| `firstOptionalGetOrThrow()`                | `Flow<StoreResult<Optional<T>>>`  | Awaits the first completed result and unwraps the `Optional`; throws on a failed result or an empty `Optional` |
| `getOptionalValueOrNull()`                 | `StoreResult<Optional<T>>`        | Returns the unwrapped value for a `Loaded` non-empty `Optional`, otherwise `null` (for `Loading`, `Failed`, or empty `Optional`) |
| `getOptionalValueOrNull()` / `getOptionalValueOrNull(key)` | `BaseStore` / `KeyedStore` of `Optional<T>` | Shortcut for `get().getOptionalValueOrNull()` - reads the latest unwrapped value synchronously |
| `isOptionalEmpty()` / `isOptionalEmpty(key)` | `BaseStore` / `KeyedStore` of `Optional<T>` | `true` only when the latest result is a `Loaded` empty `Optional`; `false` while loading, on failure, or when a value is present |

```kotlin
// fetch into an Optional-typed store:
val store = StoreFactory.simpleStoreBuilder<Optional<UserProfile>>()
    .build { api.findProfile().toOptional() }   // null -> Optional.empty()

// suspend until the first completed result and unwrap the Optional
val profile: UserProfile = store.observe().firstOptionalGetOrThrow()

// read the latest value synchronously, or null if absent/not loaded yet
val profileOrNull: UserProfile? = store.getOptionalValueOrNull()

// distinguish "loaded but absent" from "not loaded yet / failed"
if (store.isOptionalEmpty()) showEmptyState()
```

## Reading the Latest Value Synchronously

`get()` (or `get(key)` for keyed stores) returns the latest
`StoreResult<T>` from the in-memory cache. When you only need the loaded
value or the failure - not the full result - the following shortcuts read
the cache directly:

| Function                        | Receiver                  | Behaviour                                                      |
|---------------------------------|---------------------------|----------------------------------------------------------------|
| `getOrNull()`                   | `BaseStore<T>`            | Latest loaded value, or `null` while loading/failed            |
| `getOrNull(key)`                | `KeyedStore<Key, T>`      | Latest loaded value for `key`, or `null` while loading/failed  |
| `failureOrNull()`               | `BaseStore<T>`            | Latest failure, or `null` when the latest result is not failed |
| `failureOrNull(key)`            | `KeyedStore<Key, T>`      | Latest failure for `key`, or `null` when not failed            |

```kotlin
// instead of: store.get().getOrNull()
val profile: UserProfile? = profileStore.getOrNull()

// keyed store, per key:
val book: Book? = booksStore.getOrNull(bookId)
val error: Exception? = booksStore.failureOrNull(bookId)
```

These are synchronous snapshots of the cache; for an ongoing stream use
`observe()` and the [state-checking helpers](#checking-the-state).

## Combining Stores

`combineStores` combines 2 to 5 store flows (plus a list-based overload
for more) into a single `Flow<StoreResult<R>>`:

```kotlin
val stateFlow: Flow<StoreResult<State>> = combineStores(
    productsRepository.getProducts(),
    cartRepository.getMinimalCart(),
) { products, cartItems ->
    State(
        products.map { product ->
            val quantity = cartItems.firstOrNull { it.productId == product.id }?.quantity ?: 0
            ProductListItem(product, quantity)
        }
    )
}
```

The combined result follows the usual rules:

- `Loaded` only when **all** source results are `Loaded`
- `Failed` if any source result is `Failed`
- `Loading` otherwise

The list-based overload accepts any number of flows:

```kotlin
combineStores(listOf(flow1, flow2, flow3)) { values -> /* List<*> -> R */ }
```

## StoreResultReducer

`StoreResultReducer<State>` converts a flow into a
`StateFlow<StoreResult<State>>` and allows manual updates of the loaded
state on top of the source emissions. It extends the `Reducer` type from
the Container library (see [Reducer Pattern](../../docs/reducer-pattern.md)).

### Creating a Reducer

From an existing `Flow<StoreResult<T>>` (the usual case - a store flow):

```kotlin
@HiltViewModel
class CatListViewModel @Inject constructor(
    catsRepository: CatsRepository,
) : AbstractViewModel() {

    data class State(
        val cats: List<Cat>,
        val deleteInProgressIds: Set<Long> = emptySet(),
    )

    private val reducer: StoreResultReducer<State> = catsRepository
        .getCats() // Flow<StoreResult<List<Cat>>
        .storeResultToReducer(
            initialState = ::State,      // (T) -> State, on the first Loaded value
            nextState = State::copy,     // (State, T) -> State, on subsequent values
        )

    val stateFlow: StateFlow<StoreResult<State>> = reducer.stateFlow
}
```

- `initialState` builds the state from the first loaded value (or after an
  error/loading reset)
- `nextState` merges a newly loaded value into the existing state, so
  locally added fields (like `deleteInProgressIds` above) survive store
  re-emissions; with a data class, `State::copy` does exactly that

`Loading` and `Failed` results pass through to `stateFlow` unchanged.

From a plain `Flow<T>` (no store involved), use `toStoreResultReducer` -
the flow values are wrapped into `StoreResult.Loaded`, and the initial
value of `stateFlow` is `StoreResult.Loading`.

Both functions also have single-argument overloads without
`initialState` / `nextState` when the state type equals the flow's value
type.

### Combining Stores into a Reducer

When the state is built from several store flows, `combineStoresToReducer`
combines them and produces a `StoreResultReducer<State>` in one step - it is
`combineStores` followed by `storeResultToReducer`:

```kotlin
data class State(
    val products: List<Product>,          // from productsRepository.getProducts()
    val cartItems: List<CartItem>,        // from cartRepository.getMinimalCart()
    val selectedIds: Set<Long> = emptySet(), // updated manually
)

private val reducer: StoreResultReducer<State> = combineStoresToReducer(
    productsRepository.getProducts(),  // Flow<StoreResult<List<Product>>>
    cartRepository.getMinimalCart(),   // Flow<StoreResult<List<CartItem>>>
    initialState = ::State,            // (T1, T2) -> State, on the first combined Loaded value
    nextState = State::copy,           // (State, T1, T2) -> State, on subsequent values
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
)

val stateFlow: StateFlow<StoreResult<State>> = reducer.stateFlow
```

The combined result follows the same rules as `combineStores`: the reducer
holds `Loading` until **all** sources are `Loaded`, becomes `Failed` if any
source is `Failed`, and only then produces a `Loaded` state. As with
`storeResultToReducer`, `nextState` merges subsequent source values so
manual updates (like `selectedIds`) survive re-emissions; omit it to
re-initialize on every emission.

Overloads exist for 2 to 5 input flows plus a list-based overload for an
arbitrary number:

```kotlin
private val reducer = combineStoresToReducer(
    flows = listOf(flow1, flow2, flow3),
    initialState = { values -> State(/* values: List<*> */) },
    nextState = { state, values -> state.copy(/* ... */) },
    scope = viewModelScope,
    started = SharingStarted.Lazily,
)
```

All overloads also have `ReducerOwner` variants that omit `scope` /
`started` (see [ReducerOwner](#reducerowner) below).

### Manual State Updates

`updateState` modifies the current state **only if it is `Loaded`**; it is
a no-op while the reducer holds `Loading` or `Failed`:

```kotlin
fun delete(cat: Cat) {
    viewModelScope.launch {
        try {
            reducer.updateState { it.copy(deleteInProgressIds = it.deleteInProgressIds + cat.id) }
            repository.delete(cat)
        } finally {
            reducer.updateState { it.copy(deleteInProgressIds = it.deleteInProgressIds - cat.id) }
        }
    }
}
```

This is the recommended place for transient, screen-local flags
(in-progress indicators, selections) that should be layered on top of the
store data without touching the store cache itself.

### ReducerOwner

`storeResultToReducer` / `toStoreResultReducer` need a `CoroutineScope`
and a `SharingStarted` strategy. You can pass them explicitly:

```kotlin
private val reducer = repository.getCats().storeResultToReducer(
    initialState = ::State,
    nextState = State::copy,
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
)
```

Or implement `ReducerOwner` once in a base ViewModel and omit them - the
library provides `context(ReducerOwner)` overloads of both functions:

```kotlin
abstract class AbstractViewModel : ViewModel(), ReducerOwner {
    override val reducerCoroutineScope: CoroutineScope get() = viewModelScope
    override val reducerSharingStarted: SharingStarted = SharingStarted.Lazily
}
```

The context-parameter overloads require the Kotlin
`-Xcontext-parameters` compiler flag.
