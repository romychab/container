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
- [Combining Stores](#combining-stores)
- [StoreResultReducer](#storeresultreducer)
  - [Creating a Reducer](#creating-a-reducer)
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
| `getOrNull()`           | - returns `value` for `Loaded`, otherwise `null`            |

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

For example, with a local + remote store you can show a "data may be
stale" hint while the value loaded from the local storage is being
refreshed:

```kotlin
if (result.isLoaded() && result.sourceType == LocalSourceType) {
    ShowStaleDataBadge()
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
