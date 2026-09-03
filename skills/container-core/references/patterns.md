# Architecture Patterns Reference

How the pieces of `com.elveum:container:3.6.0` fit together in a real app:
which layer owns which type, what crosses each boundary, and which
conventions save you from writing plumbing the library already provides.

This file assumes the other five references and does not re-explain them:

- [`container-type.md`](container-type.md) - `Container<T>`, its three states,
  `fold`, extraction, transformations, the `ContainerFlow` / `ListContainerFlow`
  aliases.
- [`subjects.md`](subjects.md) - `LazyFlowSubject`, `LazyCache`,
  `listenReloadable`, `LoadConfig`, `SubjectFactory`, `LoaderDecorator`,
  metadata, flow dependencies.
- [`paging.md`](paging.md) - `pageLoader`, `PageEmitter`, `onItemRendered`,
  `nextPageState`.
- [`reducers.md`](reducers.md) - `Reducer`, `ContainerReducer`, `ReducerOwner`,
  the combine families, the `-Xcontext-parameters` boundary.
- [`testing.md`](testing.md) - flow-test patterns for all of the above.

Every Kotlin snippet below builds on the ones before it; read the file top to
bottom and each example's types are already declared.

## 1. The four-layer map

```
┌─ Data sources ────────────────────────────────────────────────┐
│  `suspend fun`s. Retrofit / Room / DataStore live here and    │
│  nowhere else. No Flow, no subjects - and a `Container` only  │
│  if the project picked that convention (section 2).           │
└───────────────────────────────┬───────────────────────────────┘
                                │  domain models
┌───────────────────────────────▼───────────────────────────────┐
│  Repositories (@Singleton)                                     │
│  Own a private LazyFlowSubject / LazyCache.                    │
│  Expose listenReloadable() as Flow<Container<T>>.              │
│  Mutations: write to the source, then updateIfSuccess.         │
└───────────────────────────────┬───────────────────────────────┘
                                │  Flow<Container<T>>
┌───────────────────────────────▼───────────────────────────────┐
│  ViewModels (ReducerOwner)                                     │
│  One StateFlow<State> (or StateFlow<Container<State>>) per      │
│  screen, built with a Reducer / ContainerReducer.               │
│  Screen-local state (query, filter, selection) combined in here.│
└───────────────────────────────┬───────────────────────────────┘
                                │  StateFlow<State>
┌───────────────────────────────▼───────────────────────────────┐
│  Compose screens                                               │
│  collectAsState() + fold(). Retry/reload buttons call ::reload  │
│  on the container itself - no reload plumbing back down.        │
└───────────────────────────────────────────────────────────────┘
```

Two rules define the boundaries:

- **`Container<T>` dies in the composable.** It is born in the repository -
  or, in projects that adopt section 2's option B, already in the data
  source's call adapter. ViewModels pass it through (or reshape it with a
  `ContainerReducer`); the screen unwraps it with `fold`.
- **The reload function travels *with* the container, downward.** Nothing
  travels back up. Section 6 is entirely about this.

## 2. Data sources

A data source is a class of `suspend` functions that owns exactly one
technology - a Retrofit interface, a Room DAO, DataStore - and hides it.
Retrofit interfaces, Room DAOs, DTOs and entities stay behind that boundary.

Two conventions have to be settled **once, for the whole project**, before
the first data source is written. Both are defensible either way; what is
never defensible is half the data sources following one and half the other.

### Convention 1: plain values, or `Container`?

**Option A - plain `suspend` functions.** The data source has no library
import at all. A failure is a thrown exception, and the subject's loader is
what turns it into `Container.Error`.

```kotlin
data class Product(val id: Long, val name: String, val isFavorite: Boolean = false)

// Retrofit lives inside; only `Product` crosses the boundary.
class ProductsRemoteDataSource @Inject constructor() {
    suspend fun getProducts(): List<Product> = TODO("call the API, map DTOs to Product")
    suspend fun getProduct(id: Long): Product = TODO("call the API, map the DTO to Product")
    suspend fun setFavorite(id: Long, isFavorite: Boolean): Product = TODO("PUT, return the updated Product")
}

// Room lives inside; only `Product` crosses the boundary.
class ProductsLocalDataSource @Inject constructor() {
    suspend fun readProducts(): List<Product> = TODO("read entities, map to Product")
    suspend fun writeProducts(products: List<Product>) { TODO("map to entities, write") }
}
```

**Option B - the data source returns `Container.Completed<T>`.** Retrofit accepts a
custom call adapter, so an API interface can declare
`suspend fun apiCall(): Container.Completed<T>` directly: the adapter catches transport
and HTTP failures and hands back a `Container.Error` instead of throwing. A
subject's loader then simply calls `unwrap()` on whatever the API returned,
and code that is *not* a loader moves between the two worlds with
`containerOf { }` and `unwrap()`.

```kotlin
// Retrofit interface with a Container-returning call adapter.
interface ProductsApi {
    // Completed symbol can be imported from: com.elveum.container.Container.Completed
    suspend fun getProducts(): Completed<List<Product>>
    suspend fun setFavorite(id: Long, isFavorite: Boolean): Completed<Product>
}

@Singleton
class ApiProductRepository @Inject constructor(
    private val api: ProductsApi,
) {

    // In a loader: unwrap(). It rethrows the Error container's exception, and the
    // subject turns it back into Container.Error for collectors.
    private val subject = LazyFlowSubject.create {
        emit(api.getProducts().unwrap(), RemoteSourceType, isLastValue = true)
    }

    fun getProducts(): ListContainerFlow<Product> = subject.listenReloadable()

    // Outside a loader: containerOf { } catches, so the caller gets a container
    // instead of writing a try/catch.
    suspend fun setFavorite(id: Long, isFavorite: Boolean): Container<Product> {
        return containerOf { api.setFavorite(id, isFavorite).unwrap() }
    }
}
```

What each option buys:

| | Option A - plain values | Option B - `Container` |
|---|---|---|
| Library coupling | none below the repository | data sources import `com.elveum.container` |
| Error classification | per loader, or once in a `LoaderDecorator` | once, in the call adapter |
| Faking in tests | return a value, or `throw` | build `successContainer` / `errorContainer` |
| Mutating functions | return the model; caller writes try/catch | return `Container<T>`; caller `fold`s |

`unwrap()` is what keeps option B honest. Inside a loader you *want* the
exception back, because the subject's machinery - `LoadConfig`,
`BackgroundLoadState`, the reload function - is driven by a loader that
throws, and cannot be driven by a container the loader returns. Never `emit`
a `Container` as the subject's value; that gives you `Container<Container<T>>`
and a screen that has to unwrap twice.

### Convention 2: who maps DTOs to domain models?

Option A's snippet has each data source map its own DTOs. The other half of
the choice is just as common in real projects: the data source returns the
unmapped JSON DTO or Room entity, and the **repository** owns the mapping,
usually delegating it to extension functions or dedicated mapper classes.

```kotlin
data class ProductDto(val id: Long, val title: String, val favorite: Boolean)

fun ProductDto.toProduct(): Product = Product(id, title, favorite)

class ProductsDtoDataSource @Inject constructor() {
    suspend fun getProducts(): List<ProductDto> = TODO("call the API, return DTOs")
}

@Singleton
class MappingProductRepository @Inject constructor(
    private val remote: ProductsDtoDataSource,
) {

    private val subject = LazyFlowSubject.create {
        val dtos = remote.getProducts()
        emit(dtos.map(ProductDto::toProduct), RemoteSourceType, isLastValue = true)
    }

    fun getProducts(): ListContainerFlow<Product> = subject.listenReloadable()
}
```

**The rule must be the same across the project:** either data sources return
mapped domain values, or they all return unmapped ones. A codebase where you
have to open a data source to learn which layer owns its mapping has the
costs of both conventions and the benefit of neither.

What does *not* change with the choice is where the DTO stops. The subject's
`T` is a domain model either way, so:

- **Testability.** The repository's tests fake the data source; nothing needs
  a Retrofit or Room test harness (see [`testing.md`](testing.md)).
- **Swappability.** A `LoaderDecorator` (section 7) can wrap every load
  without touching a single data source.
- **A leaked DTO is contagious.** If the *subject* were built over
  `List<ProductDto>`, that type would surface in the ViewModel's `State` and
  in the composable - and a backend field rename would then be a four-layer
  change. Mapping in the repository is fine; mapping in the ViewModel is the
  leak.

A write returns the updated model rather than `Unit` whenever the backend
gives you one; section 3's mutation pattern needs it.

For simple projects you can omit data sources and inject Retrofit APi interfaces
and / or Room DAOs directly into the repository implementation.

## 3. Repositories

The repository is where the library actually starts. It holds a
`LazyFlowSubject` (one logical value) or a `LazyCache` (one value per id) in a
**private** field and exposes `listenReloadable()`.

### The canonical shape

```kotlin
@Singleton
class ProductRepository @Inject constructor(
    private val local: ProductsLocalDataSource,
    private val remote: ProductsRemoteDataSource,
) {

    private val subject = LazyFlowSubject.create {
        val cached = local.readProducts()
        if (cached.isNotEmpty()) emit(cached, LocalSourceType)
        val fresh = remote.getProducts()
        emit(fresh, RemoteSourceType, isLastValue = true)
        local.writeProducts(fresh)
    }

    fun getProducts(): ListContainerFlow<Product> = subject.listenReloadable()

    suspend fun setFavorite(product: Product, isFavorite: Boolean) {
        val updated = remote.setFavorite(product.id, isFavorite)
        subject.updateIfSuccess { products ->
            products.map { if (it.id == updated.id) updated else it }
        }
    }
}
```

Four things are happening, each deliberate:

- **`private val subject`.** Nothing outside the repository ever touches the
  subject. If callers can reach it, they can start competing loads and the
  single-source-of-truth guarantee is gone.
- **The local-then-remote double `emit`.** One loader, two emissions, tagged
  with `LocalSourceType` / `RemoteSourceType`. The screen renders the cached
  list instantly and swaps in fresh data when it lands; `container.sourceType`
  tells the UI which one it is showing. `isLastValue = true` on the final
  emission is the hint that nothing more follows. Writing the cache *after*
  the last `emit` keeps the disk write off the display path.
- **`listenReloadable()` on the way out**, not `listen()`. It is
  `listen(ContainerConfiguration(emitBackgroundLoads = true, emitReloadFunction = true))`,
  which is exactly what sections 5 and 6 need. A bare `listen()` produces
  containers whose `reload()` does nothing and whose `backgroundLoadState` is
  always `Idle` - the two most common "why doesn't my retry button work"
  bugs.
- **The return type is `ListContainerFlow<Product>`** (=
  `Flow<Container<List<Product>>>`) - a `Flow`, not the `StateFlow` that
  `listenReloadable` actually returns. Publishing the narrower type keeps
  callers from reading `.value` and bypassing collection, which is what starts
  the load in the first place.

### Mutations: write first, reconcile second

```kotlin
suspend fun setFavorite(product: Product, isFavorite: Boolean) {
    val updated = remote.setFavorite(product.id, isFavorite)   // 1. write
    subject.updateIfSuccess { products ->                       // 2. reconcile
        products.map { if (it.id == updated.id) updated else it }
    }
}
```

Do the remote write first and patch the cached value with the server's
response. `updateIfSuccess` only applies when the current container is
`Success`, so a mutation that races an in-flight initial load is a silent
no-op instead of a corruption - the load that finishes will carry the server
state anyway.

The alternatives, and when they are right:

| Instead of `updateIfSuccess` | Use when |
|------------------------------|----------|
| `subject.updateWith(successContainer(value))` | You are replacing the whole value, not editing the current one |
| `subject.reloadAsync(LoadConfig.SilentLoading)` | The write invalidates more than you can patch locally, and a re-fetch is cheap |
| `subject.updateIfSuccess { ... }` **before** the write, then reload on failure | Optimistic UI. You accept showing a state the server has not confirmed |

Avoid a full `reloadAsync()` (non-silent) after a mutation: it resets the
container to `Pending`, so a "like" tap blanks the whole list.

### Per-id entities: `LazyCache`

When the screen shows one entity selected by id, and different screens can
observe different ids at once, use a cache. Each argument gets its own load,
its own cached value, and its own timeout.

```kotlin
@Singleton
class ProductDetailsRepository @Inject constructor(
    private val remote: ProductsRemoteDataSource,
) {

    private val cache = LazyCache.create<Long, Product> { id ->
        emit(remote.getProduct(id), RemoteSourceType, isLastValue = true)
    }

    fun getProduct(id: Long): ContainerFlow<Product> = cache.listenReloadable(id)

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        val updated = remote.setFavorite(id, isFavorite)
        cache.updateIfSuccess(id, RemoteSourceType) { updated }
    }
}
```

Every call takes the argument: `listenReloadable(id)`, `updateIfSuccess(id) { }`,
`reloadAsync(id)`. Two API differences bite constantly (the full table is in
[`subjects.md`](subjects.md)):

- the snapshot accessor is `cache.get(id)`, **not** `currentValue()`;
- `updateIfSuccess`'s second parameter is a `SourceType?` on a cache and a
  `ContainerMetadata` on a subject.

And one behavioural difference that is not symmetric: `LazyFlowSubject.updateWith`
with **no** collectors is queued and handed to the next collector, whereas
`LazyCache.updateWith(arg, ...)` for an argument nobody has ever collected -
or whose entry has since expired - is **discarded**: the cache entry does
not exist. Do not write a cache "pre-seed" on that assumption.

### Keeping a subject and a cache in sync

A list screen backed by `LazyFlowSubject<List<Product>>` in one repository and
a details screen backed by `LazyCache<Long, Product>` in another are two
independent caches of the same rows. A save on the details screen has to land
in both, or the user navigates back to a stale list.

Three pieces:

1. **An event flow** carrying "this entity changed". It exists only because
   the two caches live in *different* repositories - if the subject and the
   cache are fields of one class, drop this piece and patch both inline.
2. **The mutating function** writes the backend, patches its own cache, then
   emits the event.
3. **The list subject observes those events inside `whenActive { }`** and
   patches itself with `updateIfSuccess`.

```kotlin
data class ProductUpdatedEvent(val updatedProduct: Product)

interface ProductEvents {
    val productUpdates: Flow<ProductUpdatedEvent>
    suspend fun emitProductUpdated(product: Product)
}

@Singleton
class DefaultProductEvents @Inject constructor() : ProductEvents {

    private val events = MutableSharedFlow<ProductUpdatedEvent>()

    override val productUpdates: Flow<ProductUpdatedEvent> = events

    override suspend fun emitProductUpdated(product: Product) {
        events.emit(ProductUpdatedEvent(product))
    }
}
```

The writer - the per-id repository:

```kotlin
@Singleton
class SyncedProductDetailsRepository @Inject constructor(
    private val remote: ProductsRemoteDataSource,
    private val events: ProductEvents,
) {

    private val cache = LazyCache.create<Long, Product> { id ->
        emit(remote.getProduct(id), RemoteSourceType, isLastValue = true)
    }

    fun getProduct(id: Long): ContainerFlow<Product> = cache.listenReloadable(id)

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        val updated = remote.setFavorite(id, isFavorite)         // 1. write
        cache.updateIfSuccess(id, RemoteSourceType) { updated }  // 2. patch this repository's cache
        events.emitProductUpdated(updated)                       // 3. tell the other repositories
    }
}
```

The reader - the list repository:

```kotlin
@Singleton
class SyncedProductRepository @Inject constructor(
    private val remote: ProductsRemoteDataSource,
    private val events: ProductEvents,
) {

    private val subject = LazyFlowSubject.create {
        emit(remote.getProducts(), RemoteSourceType, isLastValue = true)
    }.whenActive {
        events.productUpdates.collect { event ->
            updateIfSuccess { products ->
                products.updateItem(event.updatedProduct) { it.id }
            }
        }
    }

    fun getProducts(): ListContainerFlow<Product> = subject.listenReloadable()
}
```

Why `whenActive` rather than a repository-level `CoroutineScope`:

- Its block is launched when the subject gets its **first** collector and
  cancelled `cacheTimeoutMillis` after the **last** one leaves - exactly the
  window in which there is a cached list worth patching. A permanently
  collecting scope would pin the subject forever and make
  `cacheTimeoutMillis` meaningless.
- The block's receiver is a `ScopedLazyFlowSubject<T>`, which *is* the
  subject and also a `CoroutineScope`. So the bare `updateIfSuccess { }`
  inside needs no captured reference, and the collection is cancelled for you.
- `whenActive` returns the subject, so it chains onto the constructor call.
  Register it at construction time: a block added while the subject is
  already active is not started until the next time the subject becomes
  active.
- `updateIfSuccess` is a no-op unless the container is `Success`, so an event
  arriving mid-load is dropped deliberately - the load in flight will bring
  the server's version anyway.

Two things to keep straight:

- **Direction.** The writer patches its own cache directly and emits for
  everyone else. If both repositories emitted *and* observed the same event
  type, each patch would echo back as another event.
- **Identity, not position.** `updateItem` locates the row by id and returns
  the list unchanged when it is absent. The list may be filtered, sorted or
  paged differently from the cache, so the incoming entity is not guaranteed
  to be in it at all. `updateItem` also has an overload taking an id and a
  `transform`, for when the new value is derived from the old one, and a
  `Container<List<T>>` form for patching a container directly.

### Paged repositories

A paged repository is the same shape with a `pageLoader`-built
`valueLoader`; see [`paging.md`](paging.md) for the loader itself. The
repository's public surface does not change at all - still one
`Flow<Container<List<T>>>`.

```kotlin
data class Order(val id: Long, val title: String)
data class OrderPage(val orders: List<Order>, val nextKey: Int?)

class OrdersDataSource @Inject constructor() {
    suspend fun fetchPage(key: Int): OrderPage = TODO("call the API")
}

@Singleton
class OrderRepository @Inject constructor(
    private val dataSource: OrdersDataSource,
) {

    private val subject = LazyFlowSubject.create(
        valueLoader = pageLoader<Int, Order>(
            initialKey = 0,
            itemId = Order::id,
        ) { pageKey ->
            val page = dataSource.fetchPage(pageKey)
            emitPage(page.orders)
            page.nextKey?.let { emitNextKey(it) }
        }
    )

    fun getOrders(): ListContainerFlow<Order> = subject.listenReloadable()
}
```

### Screen-local state does *not* belong here

A search query or a filter that only one screen cares about belongs in the
ViewModel (section 4). Push it into the repository only when the *loader*
needs it - i.e. when the backend does the filtering - and then via a
`MutableStateFlow` + `dependsOnFlow` inside the loader, so a new filter value
re-runs the load automatically. That mechanism is documented in
[`subjects.md`](subjects.md)'s flow-dependencies section.

## 4. ViewModels

One `StateFlow` per screen, produced by a reducer. Implement `ReducerOwner`
once in a base class so no reducer call ever repeats `scope` and `started`:

```kotlin
abstract class AbstractViewModel : ViewModel(), ReducerOwner {

    override val reducerCoroutineScope: CoroutineScope = viewModelScope
    override val reducerSharingStarted: SharingStarted = SharingStarted.WhileSubscribed(
        stopTimeoutMillis = 1000,
        replayExpirationMillis = 1000,
    )

}
```

That is where `SharingStarted` belongs: in **one** place, in the ViewModel
layer. Not in the repository (a repository has no lifecycle to be scoped to,
and its sharing window is `cacheTimeoutMillis` instead), and not repeated at
every call site.

### Shape A: `Reducer<State>` - container as a field

When the screen has repository data *and* screen-local state, combine them
into one plain `State` and let the container ride along as a field:

```kotlin
@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val repository: ProductRepository,
) : AbstractViewModel() {

    private val queryFlow = MutableStateFlow("")

    private val reducer: Reducer<State> = combineToReducer(
        repository.getProducts(),
        queryFlow,
        initialState = ::State,
        nextState = State::copy,
    )

    val stateFlow: StateFlow<State> = reducer.stateFlow

    fun setQuery(query: String) {
        queryFlow.value = query
    }

    data class State(
        val products: Container<List<Product>> = pendingContainer(),
        val query: String = "",
    )
}
```

`initialState = ::State` works because every `State` property has a default;
`nextState = State::copy` works because `copy`'s parameters line up
positionally with the combined flows. Keep that alignment and the whole
reducer is two references.

Note that `combineToReducer` here treats `Flow<Container<List<Product>>>` as
an ordinary flow of values - the container is data inside the state, not the
state's own status. This is the shape to use when the screen must render
something (a search field, a filter chip row) *while* the data is still
`Pending`.

### Shape B: `ContainerReducer<State>` - container wraps the state

When the whole screen is either loading, failed, or showing content, let the
container states drive the `StateFlow` and keep `State` free of them:

```kotlin
@HiltViewModel
class ProductsContainerViewModel @Inject constructor(
    repository: ProductRepository,
) : AbstractViewModel() {

    private val reducer: ContainerReducer<State> = repository
        .getProducts()
        .containerToReducer(
            initialState = ::State,
            nextState = State::copy,
        )

    val stateFlow: StateFlow<Container<State>> = reducer.stateFlow

    data class State(
        val products: List<Product>,
    )
}
```

`State` has no `Container` inside it and no defaults - `initialState` receives
the first successful value. The screen writes one `fold` around everything.

Choosing between A and B: **is there UI to show while the data is Pending?**
Yes -> shape A. No -> shape B.

### The ViewModel for a paged screen

```kotlin
@HiltViewModel
class OrdersViewModel @Inject constructor(
    repository: OrderRepository,
) : AbstractViewModel() {

    private val reducer: ContainerReducer<List<Order>> = repository
        .getOrders()
        .containerToReducer()

    val ordersFlow: StateFlow<Container<List<Order>>> = reducer.stateFlow
}
```

`containerToReducer()` with no arguments is legal when `State` is the flow's
own value type - here `List<Order>`. This is the minimum a ViewModel can be:
a repository flow made hot for the screen's lifetime.

### Two flag-sensitive shortcuts

`containerToReducer` (and `toReducer` / `toContainerReducer` / `stateIn` /
`shareIn`) rely on a `context(ReducerOwner)` parameter, so **the module must
enable `-Xcontext-parameters`**. The `combineToReducer` family does not - it
uses plain `ReducerOwner.` extension receivers. Section 8's checklist covers
what to do in a module without the flag; the full table is in
[`reducers.md`](reducers.md) section 7.

And if you use `stateIn` / `shareIn`, import them explicitly from
`com.elveum.container.reducer` - `kotlinx.coroutines.flow` declares
same-named functions that silently shadow the `ReducerOwner` overloads.

## 5. Compose screens

The screen collects one `StateFlow` and `fold`s the container. `onError` and
`onSuccess` run with a `ContainerMapperScope` receiver, which is where
`::reload`, `metadata`, `sourceType` and `backgroundLoadState` come from.
**`onPending` does not** - it is declared as a plain `() -> R`, so `::reload`
and `metadata` are unresolved references inside it. There is nothing to reload
or describe yet in that branch; if you need the reload function while the
container is `Pending`, reach it from outside the `fold` as
`container.reload(...)`.

```kotlin
@Composable
fun ProductsScreen(viewModel: ProductsViewModel) {
    val state by viewModel.stateFlow.collectAsState()
    state.products.fold(
        onPending = { CircularProgressIndicator() },
        onError = { exception ->
            Column {
                Text(exception.message.orEmpty())
                Button(onClick = ::reload) { Text("Try Again") }
            }
        },
        onSuccess = { products ->
            Column {
                products.forEach { Text(it.name) }
                Button(onClick = ::reload) { Text("Reload") }
            }
        },
    )
}
```

`::reload` is a method reference to `ContainerMapperScope.reload`, so it is
usable directly as an `onClick`. Section 6 explains why this is the whole
retry story.

### Pull-to-refresh

Two ingredients, both already on the container because the repository used
`listenReloadable()`: `backgroundLoadState` drives the indicator, and a
`LoadConfig.SilentLoading` keeps the current content on screen while the
reload runs. The config applies to this reload only and leaves the subject's
own configuration alone - see `subjects.md`'s `LoadConfig` section.

```kotlin
@Composable
fun ProductsPullToRefreshScreen(viewModel: ProductsContainerViewModel) {
    val container by viewModel.stateFlow.collectAsState()

    PullToRefreshBox(
        isRefreshing = container.backgroundLoadState == BackgroundLoadState.Loading,
        onRefresh = {
            container.reload(LoadConfig.SilentLoading)
        },
    ) {
        container.fold(
            onPending = { CircularProgressIndicator() },
            onError = { exception ->
                Column {
                    Text(exception.message.orEmpty())
                    Button(onClick = ::reload) { Text("Try Again") }
                }
            },
            onSuccess = { state ->
                LazyColumn {
                    itemsIndexed(state.products, key = { _, product -> product.id }) { _, product ->
                        Text(product.name)
                    }
                }
            },
        )
    }
}
```

Note the two different reload calls, deliberately: the one-shot silent reload
outside `fold` (`reload` is a `Container` member, so `container.` is required
there), and bare `::reload` inside `onError` / `onSuccess` (the
`ContainerMapperScope` receiver, which reloads with whatever config the
subject is already using - `LoadConfig.Normal` unless something else changed
it). Both hit the same reload function.

A plain `reload()` here instead of the one-shot silent reload would drop the
container to `Pending`, unmount the list, and leave the refresh indicator
with nothing underneath it.

### The paged screen: `onItemRendered` and the footer

Inside `fold`'s `onError` / `onSuccess`, `metadata` is the
`ContainerMapperScope`'s own property - it needs no `container.` prefix, and
it stays in scope inside nested lambdas such as `LazyColumn { }`,
`itemsIndexed { }` and `LaunchedEffect { }`:

```kotlin
@Composable
fun OrdersScreen(viewModel: OrdersViewModel) {
    val container by viewModel.ordersFlow.collectAsState()

    PullToRefreshBox(
        isRefreshing = container.backgroundLoadState == BackgroundLoadState.Loading,
        onRefresh = {
            container.reload(LoadConfig.SilentLoading)
        },
    ) {
        container.fold(
            onPending = { CircularProgressIndicator() },
            onError = { exception ->
                Column {
                    Text(exception.message.orEmpty())
                    Button(onClick = ::reload) { Text("Try Again") }
                }
            },
            onSuccess = { orders ->
                LazyColumn {
                    itemsIndexed(orders, key = { _, order -> order.id }) { index, order ->
                        LaunchedEffect(index) {
                            metadata.onItemRendered(index)
                        }
                        Text(order.title)
                    }
                    item {
                        when (val state = metadata.nextPageState) {
                            PageState.Idle -> {}
                            PageState.Pending -> CircularProgressIndicator()
                            is PageState.Error -> Button(onClick = { state.retry() }) {
                                Text("Retry: ${state.exception.message}")
                            }
                        }
                    }
                }
            },
        )
    }
}
```

- `LaunchedEffect(index)` reports the index once per item composition rather
  than on every recomposition - `onItemRendered` is what tells the loader the
  user has scrolled close enough to the end to fetch the next page. Without
  it a paged list loads page 1 and stops forever.
- The footer `item { }` renders `nextPageState`, which is the *next* page's
  status and is independent of the container's own state. `PageState.Error`
  only ever means "the list is on screen and the next page failed"; a
  first-page failure arrives as `Container.Error` in `onError` above.
- `key = { _, order -> order.id }` must use the same stable id you passed as
  `pageLoader`'s `itemId`.
- Outside `onError` / `onSuccess` there is no receiver - including inside
  `onPending`, which takes none - so it is
  `container.metadata.onItemRendered(index)` and
  `container.metadata.nextPageState`.

### Do not fold in the ViewModel

Mapping a container to three UI states inside the ViewModel (`isLoading`,
`errorText`, `items`) throws away `metadata` - including the reload function -
and forces you to rebuild it as three more fields. Pass the container through
and fold once, in the composable.

## 6. Reloads need no plumbing

**This is the single most common thing to get wrong.** The reflex from
plain-Flow architectures is to thread a retry action back down the stack:

```kotlin
// ANTI-PATTERN - do not write this
class ProductRepository {
    private val subject = ...
    fun reload() = subject.reloadAsync()          // 1. a reload on the repository
}

class ProductsViewModel : AbstractViewModel() {
    fun reload() = repository.reload()            // 2. forwarding it
}

@Composable
fun ProductsScreen(viewModel: ProductsViewModel) {
    Button(onClick = viewModel::reload) { ... }   // 3. calling it from the UI
}
```

All three steps are dead code. `listenReloadable()` already attached a reload
function to **every container the repository emits**, so the composable can
reload the exact data it is rendering:

```kotlin
onError = { exception ->
    Column {
        Text(exception.message.orEmpty())
        Button(onClick = ::reload) { Text("Try Again") }
    }
},
```

Why this is better than the plumbing, not merely shorter:

- **It cannot go stale.** The reload function is part of the container the
  branch is rendering, so a screen showing several containers gets a correct
  per-container retry for free. A `viewModel.reload()` has to guess which one.
- **It survives refactoring.** Swapping the repository's `LazyFlowSubject` for
  a `LazyCache` changes `reloadAsync()` to `reloadAsync(id)` - a signature
  change that ripples through a plumbed `reload()` and through nothing at all
  when the UI uses `::reload`.
- **It carries the config.** The call site that knows which reload it wants -
  a retry button, a pull-to-refresh - picks it directly on `container.reload(...)`,
  instead of a repository-level `reload()` having to pick one for everybody.
  Use one-shot metadata rather than a persistent `config` for this: see the
  trap `subjects.md`'s `LoadConfig` section documents, and the pull-to-refresh
  example above.

The precondition is the repository using `listenReloadable()` (or
`listen(ContainerConfiguration(emitReloadFunction = true))`). With a bare
`listen()`, `reload()` on the container is a silent no-op and this whole
section fails - which is exactly what the plumbing gets written to work
around.

### When a wrapper *is* justified

Three cases, and only these:

1. **The reload needs extra logic.** Clearing a selection, resetting a
   filter's `MutableStateFlow`, invalidating a second repository, logging an
   analytics event. Even then, prefer rewriting the container's own reload
   function over adding a ViewModel method - see the next subsection. If you
   do add a method, put it on the ViewModel, not on the repository: the
   repository has no business knowing why it is reloading.
2. **No container is available at the call site.** A toolbar action, a
   pull-to-refresh at the top of a screen whose content is `Pending`, a
   deep-link handler, a "retry all" button covering several sources. The
   `Container.reload` member (`container.reload(...)`) covers most of these
   without leaving the composable, but where the state is genuinely not
   reachable, a ViewModel method holding the subject reference is correct.
3. **The project already established the convention.** Consistency inside one
   codebase beats the ideal pattern in one new file. Section 8.

### Rewriting the reload function instead of wrapping it

Case 1 above usually has a better answer than a ViewModel method: leave
`::reload` in the UI and change what it *does*, by replacing the reload
function carried in the container's metadata. `Container.update { }` does it
for a single container, `Flow<Container<T>>.containerUpdate { }` for every
container a flow emits. Both give you a `ContainerUpdater` receiver whose
`reloadFunction` is a `var`.

```kotlin
interface Analytics {
    suspend fun reportReload()
}

@HiltViewModel
class DecoratedProductsViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val analytics: Analytics,
) : AbstractViewModel() {

    private val productsFlow: ListContainerFlow<Product> = repository.getProducts()
        .containerUpdate {
            val originReloadFunction = reloadFunction
            reloadFunction = { config: LoadConfig?, metadata: ContainerMetadata ->
                viewModelScope.launch { analytics.reportReload() } // suspending extra work
                originReloadFunction(config, metadata)             // then the real reload
            }
        }

    private val reducer: ContainerReducer<List<Product>> = productsFlow.containerToReducer()

    val stateFlow: StateFlow<Container<List<Product>>> = reducer.stateFlow
}
```

The same shape on a single container rather than a flow:

```kotlin
fun Container<List<Product>>.withResetOnReload(reset: () -> Unit): Container<List<Product>> {
    return update {
        val originReloadFunction = reloadFunction
        reloadFunction = { config: LoadConfig?, metadata: ContainerMetadata ->
            reset()
            originReloadFunction(config, metadata)
        }
    }
}
```

The screen is untouched: it still calls `::reload`, and the extra work now
travels with the container instead of being a second thing the UI has to
remember to call.

Four things this depends on:

- **`ReloadFunction` is `(config: LoadConfig?, metadata: ContainerMetadata) -> Unit`
  and is *not* suspending.** Anything suspending inside the replacement needs
  its own coroutine - `viewModelScope.launch { }` in a ViewModel. Marking the
  lambda `suspend` will not type-check.
- **Capture the original into a local *before* assigning.** The
  `reloadFunction` getter reads back out of the metadata you are about to
  overwrite, so a replacement that calls `reloadFunction(...)` rather than
  `originReloadFunction(...)` recurses into itself.
- **Pass `config` and `metadata` straight through.** They are how
  `reload(LoadConfig.SilentLoading)` and one-shot metadata from the call site
  reach the subject. Dropping them turns a pull-to-refresh back into a
  blanking reload.
- **`update` / `containerUpdate` only touch `Completed` containers.** A
  `Pending` container passes through untouched, and nothing here *creates* a
  reload function - the repository still has to use `listenReloadable()`.

`containerUpdate` runs its block on every emission, so build the flow once in
a ViewModel field, not inside a composable.

## 7. DI and scoping

### Repositories are `@Singleton`

```kotlin
@Singleton
class ProductRepository @Inject constructor(
    private val local: ProductsLocalDataSource,
    private val remote: ProductsRemoteDataSource,
) { /* as in section 3 */ }
```

The cache lives **in the subject instance**, which lives in the repository
instance. An unscoped `@Inject constructor` repository gets a new instance per
injection point, so:

- every screen builds its own subject and runs its own load;
- a mutation's `updateIfSuccess` patches one screen's copy and the other
  screens never learn about it;
- `cacheTimeoutMillis` measures nothing, because a fresh subject has nothing
  cached to time out.

This is the failure mode where "the library doesn't cache" turns out to be a
missing `@Singleton`. If a repository is genuinely per-feature, scope it to
that feature's component - the requirement is *one instance per set of
observers that must agree*, not literally application scope.

Data sources are stateless and need no scope. ViewModels are scoped by the
framework.

### One app-wide `SubjectFactory`

Bind a single `SubjectFactory` and inject it wherever a repository builds a
subject. It centralises the cache timeout and the `LoaderDecorator`, and it
gives tests a swap point.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSubjectFactory(): SubjectFactory {
        return DefaultSubjectFactory(cacheTimeoutMillis = 30_000L)
    }
}
```

`30_000L` is a deliberate departure from `DEFAULT_CACHE_TIMEOUT_MILLIS`
(`1000L`). One second is long enough to survive a recomposition but not a
screen rotation or a back-and-forward navigation; thirty seconds keeps the
data alive across normal navigation, which is almost always what an app
wants.

The repository then takes the factory instead of calling
`LazyFlowSubject.create` directly:

```kotlin
@Singleton
class FactoryProductRepository @Inject constructor(
    subjectFactory: SubjectFactory,
    private val remote: ProductsRemoteDataSource,
) {
    private val subject = subjectFactory.createSubject { emit(remote.getProducts()) }

    fun getProducts(): ListContainerFlow<Product> = subject.listenReloadable()
}
```

`createSubject` / `createCache` accept per-call overrides
(`cacheTimeoutMillis`, `loadConfig`, `metadata`, ...) where one repository
needs to differ; `null` means "inherit the factory's value". They have **no**
`loaderDecorator` parameter - the decorator is a property of the factory.

### Cross-cutting logic: one `LoaderDecorator` for every subject and cache

A `LoaderDecorator` wraps the loader of every subject and cache the factory
builds. Session validation is the standard use: check the token once, in one
place, and have every load in the app fail with the same exception when it is
missing.

```kotlin
class AuthException : Exception("Not authenticated")

interface SessionManager {
    val tokenFlow: Flow<String>
}

fun authDecorator(sessionManager: SessionManager): LoaderDecorator = LoaderDecorator { originLoader ->
    val token: String = dependsOnFlow("auth-token") { sessionManager.tokenFlow }
    if (token.isBlank()) completeWithFailure(AuthException())
    originLoader()
}
```

Install it once:

```kotlin
// REPLACES the provideSubjectFactory above, in the same @Module. A module
// cannot declare two @Provides for SubjectFactory - that is a Dagger
// duplicate-binding error.
@Provides
@Singleton
fun provideSubjectFactory(sessionManager: SessionManager): SubjectFactory {
    return DefaultSubjectFactory(
        cacheTimeoutMillis = 30_000L,
        loaderDecorator = authDecorator(sessionManager),
    )
}
```

Now every repository built through the factory refuses to load without a
token, and every screen's `fold(onError = ...)` sees `AuthException` - with a
working `::reload` that will re-check the token.

Three details that decide whether this works:

- **`completeWithFailure(exception)`, not `throw`.** It returns `Nothing`,
  unwinds before `originLoader()` runs, and delivers the error to collectors
  *regardless of the silent-error policy*. A plain `throw` under
  `LoadConfig.SilentLoadingAndError` would leave stale content on screen and
  the user stuck.
- **`dependsOnFlow`, not a one-off read.** Because the decorator declares the
  token as a dependency, a new token re-runs every decorated loader
  automatically - sign-in refreshes the whole app with no further wiring. For
  sign-out, `completeWithCacheCleanUp()` drops the cached values instead of
  showing an error over them.
- **Pick a key that cannot collide.** Decorator keys share one namespace with
  the decorated loader's `dependsOnFlow` keys; the *first* call for a key
  wins and later lambdas are never invoked. Prefix decorator keys
  (`"auth-token"`, not `"token"`).

### Two factories: opting out of the decorator with a qualifier

Some loads must not go through the auth decorator - the sign-in call itself,
a public config endpoint, anything that runs before there is a token. Rather
than dropping the decorator or teaching it about exceptions, provide a
**second** `SubjectFactory` behind a Dagger qualifier:

```kotlin
@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class NoAuthQualifier
```

```kotlin
// In the same @Module as provideSubjectFactory. A qualified @Provides is a
// distinct binding, so this is not the duplicate-binding error you would get
// from a second unqualified SubjectFactory.
@Provides
@Singleton
@NoAuthQualifier
fun provideNoAuthSubjectFactory(): SubjectFactory {
    return DefaultSubjectFactory(cacheTimeoutMillis = 30_000L)
}
```

Then annotate only the injection points that must skip the decorator:

```kotlin
@Singleton
class PublicProductRepository @Inject constructor(
    @NoAuthQualifier subjectFactory: SubjectFactory,
    private val remote: ProductsRemoteDataSource,
) {
    private val subject = subjectFactory.createSubject { emit(remote.getProducts()) }

    fun getProducts(): ListContainerFlow<Product> = subject.listenReloadable()
}
```

Keep the **unqualified** binding the decorated one, so that forgetting the
annotation fails closed - the load goes through the auth check - rather than
silently opting a repository out of session validation.

The same trick applies to any other setting that only the factory can carry -
in practice that means a different `LoaderDecorator`, since it is the one
`createSubject` / `createCache` parameter with no per-call override (see
above). Everything else the factory sets - `cacheTimeoutMillis`,
`coroutineScopeFactory`, and the rest - already has a per-call override on
`createSubject` / `createCache`, so a second qualified factory is worth it
only when the *decorator* itself needs to differ.

`SubjectFactory` also has a companion object that *is* a `SubjectFactory`,
delegating to a swappable global instance (`SubjectFactory.setFactory(...)` /
`resetFactory()`). Use it as a default parameter value in code that cannot be
injected; prefer real DI everywhere else.

## 8. Working in an existing project

Before writing container code in a codebase you did not start, run this
checklist. Every item is a place where a well-formed pattern from this file
is the wrong thing to add.

1. **Find where the subjects live.**
   `grep -rn "LazyFlowSubject\|LazyCache\|SubjectFactory" src/`.
   If repositories build subjects directly, match that; if they inject a
   `SubjectFactory`, inject it too - mixing the two means the app-wide
   `LoaderDecorator` and cache timeout apply to some loads and not others,
   which is worse than either choice consistently.

2. **Match the existing reload convention before introducing `listenReloadable`.**
   If the project already threads `reload()` through ViewModels (section 6's
   anti-pattern), do not half-convert one screen. Either leave it and follow
   the convention, or convert a whole feature and say so. A codebase where
   half the retry buttons call `::reload` and half call `viewModel::reload` is
   the worst outcome. Also check what `listen()` calls exist: switching one to
   `listenReloadable()` changes the containers every existing collector sees.

3. **Check for `-Xcontext-parameters` in the module you are editing** -
   `build.gradle.kts`, `kotlin { compilerOptions { freeCompilerArgs } }`.
   Without it, `toReducer` / `toContainerReducer` / `containerToReducer` /
   `stateIn` / `shareIn` will not resolve in their short form: pass `scope`
   and `started` explicitly instead. The `combineToReducer` family works
   either way. Check the *module you are editing*, not the library module -
   they are configured separately.

4. **Look for a `ReducerOwner` base class** before writing `scope`/`started`
   by hand: `grep -rn "ReducerOwner" src/`. Most projects have exactly one
   (`AbstractViewModel` or similar) and every ViewModel extends it.

5. **Mirror the state-class conventions.** Is `State` nested inside the
   ViewModel or top-level? Is the container a field (shape A) or the wrapper
   (shape B)? Does the project use the public-interface/private-impl split
   from [`reducers.md`](reducers.md) section 8, or `@Immutable` data classes,
   or `kotlinx.collections.immutable` lists? Copy the nearest existing
   ViewModel's shape rather than importing this file's.

6. **Check repository scoping before assuming the cache works.** A
   `@Singleton`-less repository (section 7) means every symptom you are about
   to debug has one cause. Confirm it first.

7. **Check the `SubjectFactory`'s configured cache timeout** before tuning a
   single subject. If the app-wide factory already sets 30 seconds, a
   per-repository `cacheTimeoutMillis` is an override to justify, not a
   default to add.

8. **Match the project's data-source and mapping conventions before adding a
   new data source.** Two things have to be settled once, project-wide, and
   followed everywhere (section 2): whether data sources return plain values
   or `Container<T>` directly, and which layer maps DTOs/entities to domain
   models (the data source itself, or the repository). Grep an existing data
   source for both before writing a new one - a mix of either is worse than
   either choice made consistently.
