# Container Store - Architecture Patterns

Layer-by-layer rules and code patterns for integrating the Store library
into an Android app: composable screens → view-models exposing
`StateFlow` → repositories → data sources. See
[api.md](api.md) for the full imports reference and core types.

## Data Sources

Data sources are Retrofit interfaces, Room DAOs, DataStore wrappers etc.
Prefer wrapping raw Retrofit/DAO types into more abstract sources (e.g.
`ProductsRemoteDataSource` that uses a Retrofit interface internally), so
repositories never see transport types.

A data source can **optionally implement a Store contract**, so it can be
passed directly to a builder's `build(contract)` overload. Put the
DTO→domain mapping inside the contract implementation - then the
repository is free from mapping logic:

```kotlin
import com.elveum.store.contracts.SimpleSuspendingContract

class ArticlesDataSource(
    private val articlesApi: ArticlesApi,   // Retrofit interface
    private val articlesDao: ArticlesDao,   // Room DAO with suspend functions
) : SimpleSuspendingContract<List<Article>> {

    override suspend fun fetch(): List<Article> {
        return articlesApi.getArticles().map { dto -> dto.toDomain() }
    }

    override suspend fun saveToLocalStorage(data: List<Article>) {
        articlesDao.replaceAll(data.map { it.toEntity() })
    }

    override suspend fun loadFromLocalStorage(): List<Article>? {
        return articlesDao.getAll()
            ?.takeIf { it.isNotEmpty() }
            ?.map { entity -> entity.toDomain() }
    }
}
```

Contract selection is mechanical - it is determined by the builder
configuration (store type × `withQuery` × local storage mode):

| Builder configuration | Contract to implement |
|-----------------------|-----------------------|
| `simpleStoreBuilder` | `SimpleContract<T>` |
| `simpleStoreBuilder` + `addSuspendingLocalStorage` | `SimpleSuspendingContract<T>` |
| `simpleStoreBuilder` + `addReactiveLocalStorage` | `SimpleReactiveContract<T>` |
| `simpleStoreBuilder` + `withQuery` | `SimpleQueryContract<Q, T>` |
| `simpleStoreBuilder` + `withQuery` + suspending/reactive storage | `SimpleQuerySuspendingContract` / `SimpleQueryReactiveContract` |
| `simpleStoreBuilder` + `disableFetcher` | `SimpleReactiveNoFetcherContract<T>` |
| `simpleStoreBuilder` + `disableFetcher` + `withQuery` | `SimpleQueryReactiveNoFetcherContract<Q, T>` |
| `simpleStoreBuilder` + `withKeys` | `SimpleKeyedContract<Key, T>` |
| `simpleStoreBuilder` + `withKeys` + suspending/reactive storage | `SimpleKeyedSuspendingContract` / `SimpleKeyedReactiveContract` |
| `simpleStoreBuilder` + `withKeys` + `disableFetcher` | `SimpleKeyedReactiveNoFetcherContract<Key, T>` |
| `simpleStoreBuilder` + `withKeys` + `withQuery` | `SimpleKeyedQueryContract<Key, Q, T>` (suspending/reactive/no-fetcher variants: `SimpleKeyedQuerySuspendingContract` / `SimpleKeyedQueryReactiveContract` / `SimpleKeyedQueryReactiveNoFetcherContract`) |
| `pagedStoreBuilder` | `PagedContract<PageKey, T>` |
| `pagedStoreBuilder` + `addSuspendingLocalStorage` | `PagedSuspendingContract<PageKey, T>` |
| `pagedStoreBuilder` + `withQuery` | `PagedQueryContract<Q, PageKey, T>` |
| `pagedStoreBuilder` + `withQuery` + suspending storage | `PagedQuerySuspendingContract<Q, PageKey, T>` |
| `pagedStoreBuilder` + `withKeys` | `PagedKeyedContract<Key, PageKey, T>` (suspending: `PagedKeyedSuspendingContract` - `fetch`, `saveToLocalStorage`, `loadFromLocalStorage`) |
| `pagedStoreBuilder` + `withKeys` + `withQuery` | `PagedKeyedQueryContract<Key, Q, PageKey, T>` (suspending: `PagedKeyedQuerySuspendingContract`) |

Contracts are optional - `build` also accepts plain lambdas (`onFetch`,
`onSaveToStorage`, `onLoadFromStorage` / `onObserveStorage`). Use lambdas
for trivial sources, contracts when mapping or multiple callbacks are
involved.

## Repositories

Repositories **create and hold Store instances**. If the project has a
domain layer with repository interfaces, keep that approach (interface in
the domain/api module, implementation in the data/impl module); otherwise
a plain class is fine.

### Creating stores

```kotlin
import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.seconds

class ArticlesRepository(
    storeFactory: StoreFactory,            // injected; see DI section
    articlesDataSource: ArticlesDataSource,
) {

    private val store = storeFactory.simpleStoreBuilder<List<Article>>()
        .setInMemoryCacheTimeout(30.seconds)   // optional; default 5s
        .setCoroutineContext(Dispatchers.IO)   // optional;
        .addSuspendingLocalStorage()           // optional; see contracts table
        .build(articlesDataSource)             // contract or lambdas

    fun getArticles(): Flow<StoreResult<List<Article>>> = store.observe()
}
```

Keyed store (per-entity details):

```kotlin
import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow

class ProductDetailsRepository(
    storeFactory: StoreFactory,
    private val productsDataSource: ProductsDataSource,
) {

    private val store = storeFactory.simpleStoreBuilder<ProductDetails>()
        .withKeys<Long>()
        .build(onFetch = productsDataSource::fetchProductById)

    fun getProductById(id: Long): Flow<StoreResult<ProductDetails>> = store.observe(id)

    // Optional wrapper: the UI can also reload this id directly via
    // result.invalidate() on the result observed for it (see Composable Screens).
    fun reloadProduct(id: Long) = store.invalidateAsync(id)
}
```

Paged store (`onFetch` returns `PagedList(items, nextKey)`; `nextKey =
null` means no more pages; `itemId` is used to deduplicate items across
pages):

```kotlin
import com.elveum.store.StoreFactory
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.paged.PagedList
import kotlinx.coroutines.flow.Flow

class FeedRepository(
    storeFactory: StoreFactory,
    private val feedDataSource: FeedDataSource,
) {

    private val store = storeFactory.pagedStoreBuilder<Int, Post>(
        initialKey = 0,
        itemId = Post::id,
    )
        .setFetchDistance(20)  // optional; default 20 items before the list end
        .setLoadRequest(LoadRequest.Silent)  // reloads keep existing content (pull-to-refresh)
        .build(
            onFetch = { pageKey ->
                val page = feedDataSource.fetchPage(pageKey)
                PagedList(items = page.posts, nextKey = page.nextKey)
            }
        )

    fun getFeed(): Flow<StoreResult<List<Post>>> = store.observe()
    fun onItemRendered(index: Int) = store.onItemRendered(index)

    // OPTIONAL: prefer reloading straight from the UI via result.invalidate()
    // (see "Reloading data" under Composable Screens). Keep these wrappers only
    // when the reload needs extra logic, when the call site has no StoreResult,
    // or to match an existing project convention.
    // invalidate/invalidateAsync take NO request; each observer keeps receiving
    // data per the request it subscribed with (here the builder default, Silent).
    fun refresh() = store.invalidateAsync()
    fun tryAgain() = store.invalidateAsync()
}
```

See "Typical Operations" in [api.md](api.md) for observing, refreshing,
updates and queries.

IMPORTANT: Usually there is NO need to set dispatcher via `setCoroutineContext(Dispatchers.IO)`, since
most of modern IO operations (Room database, Retrofit) already operate on non-UI thread.

### Mutations (add / update / remove)

A mutation (create/update/delete) must **always hit the backend**. What else you
must do depends on whether the store has a **reactive** local source attached
(`addReactiveLocalStorage()`, or a `disableFetcher()` store observing a local
`Flow`).

**Case 1 — reactive local source attached.** The store's value is driven by the
local `Flow` (`onObserveStorage`). Write to the backend, then write to the **local
source** (a Room `@Insert`/`@Update`/`@Delete`); the local `Flow` re-emits and the
store updates itself. Do **not** also call `updateIfSuccess` / `updateWith` here -
the next local emission would overwrite it anyway.

```kotlin
class NotesRepository(
    storeFactory: StoreFactory,
    private val notesApi: NotesApi,
    private val notesDao: NotesDao,          // exposes observeNotes(): Flow<List<NoteEntity>>
) {
    private val store = storeFactory.simpleStoreBuilder<List<Note>>()
        .addReactiveLocalStorage()
        .build(
            onFetch = { notesApi.getNotes().map { it.toDomain() } },
            onSaveToStorage = { notes -> notesDao.replaceAll(notes.map { it.toEntity() }) },
            onObserveStorage = { notesDao.observeNotes().map { row -> row.map { it.toDomain() } } },
        )

    fun observeNotes(): Flow<StoreResult<List<Note>>> = store.observe()

    suspend fun addNote(draft: NoteDraft) {
        val created = notesApi.createNote(draft)   // 1) backend
        notesDao.insert(created.toEntity())        // 2) local source -> Flow re-emits -> store updates
    }                                              //    (no store call needed)

    suspend fun deleteNote(id: Long) {
        notesApi.deleteNote(id)                    // backend
        notesDao.deleteById(id)                    // local source -> propagates to the store
    }
}
```

**Case 2 — no reactive local source** (remote-only, or `addSuspendingLocalStorage()`).
Nothing propagates automatically, so update every layer **manually**: backend, then
the suspending storage (if any), then the **store cache** via `updateIfSuccess { }`
(read-modify-write; no-op unless currently `Loaded`) or `updateWith(...)`. The
storage write keeps the next cold load consistent; the store-cache update reflects
the change to current observers immediately.

```kotlin
class NotesRepository(
    storeFactory: StoreFactory,
    private val notesApi: NotesApi,
    private val notesDao: NotesDao,          // suspend save/load only (no Flow)
) {
    private val store = storeFactory.simpleStoreBuilder<List<Note>>()
        .addSuspendingLocalStorage()
        .build(
            onFetch = { notesApi.getNotes().map { it.toDomain() } },
            onSaveToStorage = { notes -> notesDao.replaceAll(notes.map { it.toEntity() }) },
            onLoadFromStorage = { notesDao.getAll()?.map { it.toDomain() } },
        )

    fun observeNotes(): Flow<StoreResult<List<Note>>> = store.observe()

    suspend fun addNote(draft: NoteDraft) {
        val created = notesApi.createNote(draft)             // 1) backend
        notesDao.insert(created.toEntity())                  // 2) suspending storage (next cold load)
        store.updateIfSuccess { it + created.toDomain() }    // 3) store cache (current observers)
    }

    suspend fun deleteNote(id: Long) {
        notesApi.deleteNote(id)                              // backend
        notesDao.deleteById(id)                              // storage
        store.updateIfSuccess { list -> list.filterNot { it.id == id } }  // store cache
    }
}
```

For a **remote-only** store (no local storage at all) it is the same as Case 2
minus the storage write: hit the backend, then `updateIfSuccess { }` /
`updateWith(...)` the store cache.

Notes:

- `updateIfSuccess` / `updateWith` act on the **in-memory cache**, which exists
  only while the store is observed. If the store may currently be unobserved, the
  storage write (step 2) is what makes the change survive; on the next observe the
  store reloads from storage. (With a reactive source, Case 1, the local write both
  persists and propagates - one step.)
- For a snappy UI that also rolls back on failure, wrap Case 2 in
  `optimisticUpdate { old -> emit(newValue); backendCall() }` - emitted values
  auto-revert if the block throws. (With a reactive source, prefer an optimistic
  write to the local source if it supports it, or just accept the Flow round-trip.)
- Keyed store: use the key-scoped variants (`updateIfSuccess(key) { }`,
  `updateWith(key, ...)`, `optimisticUpdate(key) { }`).
- Paged store has **no** reactive local storage, so it is always Case 2: update the
  backend, then patch the paged cache with `updateIfSuccess { }` /
  `optimisticUpdate { }`, or call `invalidate()` to reload from the first page.

### Externally-driven stores (event bus / in-memory state) - no fabricated fetcher

Some stores have **no remote source**: the value is produced inside the app and
changes over time (an in-memory selection, session/auth state, an event-bus-like
stream). Do **not** fake a fetcher to model this - e.g.
`build { awaitCancellation() }` (never returns, so the store stays `Loading`
forever) or `build { Optional.empty() }` (one throwaway value, then updates only
via `updateWith`). Those hacks fight the library and break `Loading`/reload
semantics.

Instead, keep the value in a `MutableStateFlow` (or `MutableSharedFlow`) that the
app owns, and let the store **observe** it via `disableFetcher()`. To change the
value, mutate the flow directly - the store re-emits automatically:

```kotlin
import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SelectedFilterRepository(
    storeFactory: StoreFactory,
) {
    // app-owned source of truth (also can be a flow from Android Data Store, Room,
    // shared preferences, etc.):
    private val selectedFilter = MutableStateFlow(Filter.Default)

    private val store = storeFactory.simpleStoreBuilder<Filter>()
        .disableFetcher()                          // no remote fetch
        .build(onObserve = { selectedFilter })     // () -> Flow<T>

    fun observeFilter(): Flow<StoreResult<Filter>> = store.observe()

    // "update the value" = mutate the external flow directly (no updateWith / fake fetch):
    fun setFilter(filter: Filter) { selectedFilter.value = filter }
}
```

Notes:

- Use a `MutableStateFlow` when there is always a current value (the store becomes
  `Loaded` immediately). Use a `MutableSharedFlow(replay = 1)` for a pure event
  stream with no initial value (the store stays `Loading` until the first emit).
- Keyed variant: `simpleStoreBuilder<T>().withKeys<Key>().disableFetcher().build { key -> flowFor(key) }`,
  holding one `MutableStateFlow` per key.
- This is **not** the same as `updateWith` / `updateIfSuccess`: those act on the
  in-memory cache **only while observed** and are for reflecting a change that
  already happened in a real data source. For an app-owned value that IS the
  source, hold it in a flow and observe it - the flow survives cache release,
  a value pushed with `updateWith` does not.
- If the value must ALSO be fetched remotely once and then react to local pushes,
  that is a real data source: use `addReactiveLocalStorage()` (remote fetch +
  observe a local `Flow`) instead of `disableFetcher()`.

### Reactive default request (runtime-controlled loading policy)

`setLoadRequest` has two overloads: a fixed `setLoadRequest(LoadRequest)` and a
reactive `setLoadRequest(Flow<LoadRequest>)`. Use the reactive overload when the
loading policy must change at runtime - its latest emission becomes the default
request for subsequent loads and new observers, with no store recreation and no
change to any `observe(...)` call site. Typical use cases: automatic
online/offline switching on connectivity changes, or a user-controlled "offline
mode" / "data saver" flag stored in DataStore/preferences and toggled from a
settings screen.

```kotlin
import com.elveum.store.StoreFactory
import com.elveum.store.load.LoadRequest
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArticlesRepository(
    storeFactory: StoreFactory,
    settings: SettingsStore,                // exposes the persisted flag as Flow<Boolean>
    articlesDataSource: ArticlesDataSource,
) {

    private val store = storeFactory.simpleStoreBuilder<List<Article>>()
        .addSuspendingLocalStorage()        // so offlineMode() has cached data to serve
        .setLoadRequest(
            settings.offlineModeEnabled.map { offline ->
                if (offline) LoadRequest.builder().offlineMode().build()
                else LoadRequest.Default
            }
        )
        .build(articlesDataSource)

    fun getArticles(): Flow<StoreResult<List<Article>>> = store.observe()
}
```

Available on every builder (simple, keyed, paged, and their query variants).

### Query stores (search / filter / sort)

A *query* is any runtime parameter that re-triggers fetching (search text,
filter, sort order). `withQuery` comes in two forms - pick by **who owns the
query value**. Full signatures are in [api.md](api.md) ("Queries (withQuery)").

**A. Store owns the query** - `withQuery(initialQuery, debounceMillis)`. The
store keeps the current query and exposes `queryFlow` + `submitQueryAsync(...)`;
the repository forwards those:

```kotlin
import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ImageSearchRepository(
    storeFactory: StoreFactory,
    private val imagesDataSource: ImagesDataSource,
) {
    private val store = storeFactory.simpleStoreBuilder<List<Image>>()
        .withQuery(initialQuery = "", debounceMillis = 300)   // -> SimpleQueryStore<String, List<Image>>
        .build { query -> imagesDataSource.search(query) }    // fetch lambda receives the query

    fun observeImages(): Flow<StoreResult<List<Image>>> = store.observe()
    fun currentQuery(): StateFlow<String> = store.queryFlow   // e.g. to prefill the search field
    fun search(query: String) = store.submitQueryAsync(query) // fire-and-forget re-fetch
}
```

**B. Caller owns the query** - `withQuery { flow }`. The query already lives in an
external `StateFlow`. Pass that flow to the builder: the store follows it and stays
a **plain** `SimpleStore` (no `submitQuery`), so the query is never mirrored in two places:

```kotlin
import com.elveum.store.StoreFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ImageSearchRepository(
    private val storeFactory: StoreFactory,
    private val imagesDataSource: ImagesDataSource,
) {
 
    private val _externalQueryFlow = MutableStateFlow("")
    val externalQueryFlow: StateFlow<String> = _externalQueryFlow
    
    private val store = storeFactory.simpleStoreBuilder<List<Image>>()
        // if external flow is StateFlow -> no need to manually set an initial query - it is derived from StateFlow.value
        .withQuery(debounceMillis = 300) { externalQueryFlow } 
        .build { q -> imagesDataSource.search(q) }
    
    fun submitQuery(query: String) = _externalQueryFlow.update { query }
}
```

If a query is supplied by ViewModel, it is better to scope the store to the ViewModel lifecycle.
But if it is not possible (e.g. the repository must be a singleton), then:

- Create a function providing the store instance
- Optional: use `whenActive` block to react on external events and update data in the store (if needed)
- Ideally, return not a store, but observable `Flow`

```kotlin
class ImageSearchRepository(
    private val storeFactory: StoreFactory,
    private val imagesDataSource: ImagesDataSource,
) {
    
    // any ViewModel can call this function and pass queryFlow
    fun observeImages(queryFlow: StateFlow<String>): Flow<StoreResult<List<Image>>> =
        storeFactory.simpleStoreBuilder<List<Image>>()
            .withQuery(debounceMillis = 300) { queryFlow }
            .build { query -> imagesDataSource.search(query) }
            .whenActive { 
                // OPTIONAL:
                // subscribe to other dependencies that affects the store (if needed)
                // 'this' within whenActive points to the Store itself
            }
            .observe()
}
```

Notes:

- The external-flow overload has two forms: `withQuery(debounceMillis) { stateFlow }`
  (initial query = `stateFlow.value`) and `withQuery(initialQuery, debounceMillis) { flow }`
  for a plain `Flow<Q>` that may not emit synchronously.
- The flow is collected only while the store is observed; the first load uses the
  initial query, each later emission re-fetches (paged: resets to the first page).
- `withQuery` combines with local storage and `disableFetcher()` in **any order**,
  and the `build(...)` lambdas receive the query (`build { q -> ... }`).
- Keyed **per-key** query: `simpleStoreBuilder<T>().withKeys<Key>().withQuery { key -> flowFor(key) }`.
  Reversed, `withQuery { flow }.withKeys<Key>()` **shares one flow across all keys**.

### Custom result metadata (flags beyond the value)

When a data source returns extra flags alongside the data (`hasNextPage`, an
ETag, `lastUpdatedAt`, a "stale" marker), attach them as **custom
`ContainerMetadata`** rather than widening the value type. The store propagates
metadata to the emitted `StoreResult`, and the ViewModel/composable reads it back
with `result.metadata.get<T>()`. See [api.md](api.md) ("Container metadata") for
the full API and `ContainerMetadata.kt` for how to define a metadata type.

```kotlin
import com.elveum.container.ContainerMetadata
import com.elveum.container.get
import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import com.elveum.store.stores.paged.PagedList
import kotlinx.coroutines.flow.Flow

data class HasNextPageMetadata(val hasNextPage: Boolean) : ContainerMetadata

class FeedRepository(
    storeFactory: StoreFactory,
    private val feedDataSource: FeedDataSource,
) {
    private val store = storeFactory.pagedStoreBuilder<Int, Post>(initialKey = 0, itemId = Post::id)
        .build(
            onFetch = { pageKey ->
                val page = feedDataSource.fetchPage(pageKey)
                PagedList(
                    items = page.posts,
                    nextKey = page.nextKey,
                    metadata = HasNextPageMetadata(page.hasNext),   // attach the flag
                )
            }
        )

    fun getFeed(): Flow<StoreResult<List<Post>>> = store.observe()
}

// In the ViewModel / composable, read it back off the rendered result:
val hasNext = (result as? StoreResult.Loaded)?.metadata?.get<HasNextPageMetadata>()?.hasNextPage ?: false
```

Prefer this over the `PagedList(items, nextKey, totalCount = n)` convenience
constructor when the API gives you flags (like `hasNextPage`) rather than a total
count - that convenience constructor merely attaches the built-in
`TotalPagedItemsCountMetadata`; custom metadata works the same way. Combine
multiple entries with `+` (`HasNextPageMetadata(true) + EtagMetadata(tag)`).

Metadata can also be attached to the request that *triggers* a reload, not just
produced in the loader: `invalidate`, `invalidateAsync`, `invalidateAllAsync`,
`submitQuery` and `submitQueryAsync` all take an optional
`metadata: ContainerMetadata` that is merged into the emitted result — useful for
tagging *why* a reload happened (e.g. `store.invalidateAsync(RefreshReason.Push)`).
Mark such a type with `ContainerMetadata.OneShot` when it should apply only to
that one load: it rides along on the emitted result (and stays while the value is
cached) but is dropped from the next reload/query/dependency/post-expiry load.
The reference-free `StoreResult.invalidate(config?, metadata?)` accepts the same
metadata argument, so you can tag a reload straight from the rendered result. See
[api.md](api.md) ("Container metadata") for the full API.

### Relations between stores (entity dependencies)

When data managed by one store depends on another store (e.g. editing an
entity on a details screen must update the master list), use
`whenActive { }` on the dependent store. The block runs while the store
has observers and is cancelled when the cache is released:

```kotlin
import com.elveum.store.StoreFactory
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.Flow

// Contract exposing ONLY what this repository needs from the other one.
// Preferred over a direct repository->repository dependency.
interface CatEvents {
    fun observeCatEvents(): Flow<CatUpdatedEvent>
}

class CatsRepository(
    storeFactory: StoreFactory,
    catsDataSource: CatsDataSource,
    catEvents: CatEvents,                 // implemented by CatDetailsRepository
) {

    private val store = storeFactory.simpleStoreBuilder<List<Cat>>()
        .build(onFetch = catsDataSource::fetchCats)
        .whenActive {
            catEvents.observeCatEvents().collect { event ->
                // read-modify-write the loaded list; no-op if not loaded yet
                updateIfSuccess { cats ->
                    cats.map { if (it.id == event.cat.id) event.cat else it }
                }
            }
        }

    fun getCats() = store.observe()
}
```

Rules:

- **Avoid direct horizontal dependencies between repositories.** Define a
  small interface (like `CatEvents` above) exposing only the necessary
  information/events, implement it in the producing repository, and bind
  it in DI.
- The producing side emits an event (e.g. via `MutableSharedFlow`) after
  a successful update.
- Alternatively, when one screen needs a *list item + per-item data from
  another store*, do not push updates at all - compose flows with
  `storeListFlatMapLatest`. The flow can be composed in repositories,
  view-models, etc. It depends on project / concrete use-case context.

## ViewModels

Rules:

- Expose the final screen state via **one `StateFlow`**.
- Define a `State` class per view-model representing the whole screen
  state.
- If the project has a `ReducerOwner`-implementing base view-model (often
  called `AbstractViewModel`), extend it and use the short reducer
  overloads. **Otherwise do not introduce `ReducerOwner`** - pass
  `scope` / `started` (and `stateIn` arguments) explicitly.

Pick the pattern by this decision tree:

| Situation | Pattern |
|-----------|---------|
| Observes ONE flow (with or without `StoreResult`), adds nothing | `stateIn` directly on the repository flow |
| Observes SEVERAL `StoreResult` flows, adds nothing | `combineStores(...)` then `stateIn` |
| Observes a mix of `StoreResult` flows and plain flows, adds nothing | standard `combine(...)` with the plain flows, then `stateIn` |
| Adds its own screen-local data (progress flags, selections, manually updated fields) | `StoreResultReducer` via `storeResultToReducer` (source emits `StoreResult`) or `toStoreResultReducer` (plain source) |
| Combines SEVERAL `StoreResult` flows AND adds screen-local data | `combineStoresToReducer(...)` (combine + `StoreResultReducer` in one step) |

### Pattern 1 - single flow, no extra data

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FeedViewModel(
    private val feedRepository: FeedRepository,
) : ViewModel() {

    val stateFlow: StateFlow<StoreResult<List<Post>>> = feedRepository
        .getFeed()
        .stateIn(viewModelScope, SharingStarted.Lazily, StoreResult.Loading)

    fun onItemRendered(index: Int) = feedRepository.onItemRendered(index) // for paged data

    // OPTIONAL: a screen can reload directly with result.invalidate() instead
    // (see "Reloading data" under Composable Screens). Keep these for reloads
    // that need extra logic or to match an established project convention.
    fun refresh() = feedRepository.refresh()        // silent (observer uses LoadRequest.Silent)
    fun tryAgain() = feedRepository.tryAgain()      // non-silent
}
```

### Pattern 2 - several StoreResult flows, no extra data

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.store.load.StoreResult
import com.elveum.store.load.combineStores
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProductDetailsViewModel(
    private val productId: Long,
    productDetailsRepository: ProductDetailsRepository,
    cartRepository: CartRepository,
) : ViewModel() {

    data class State(
        val productDetails: ProductDetails,
        val isInCart: Boolean,
    )

    val stateFlow: StateFlow<StoreResult<State>> = combineStores(
        productDetailsRepository.getProductById(productId), // Flow<StoreResult<ProductDetails>>
        cartRepository.isInCart(productId), // Flow<StoreResult<Boolean>>
        ::State,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreResult.Loading)
}
```

The combined result is `Loaded` only when all sources are `Loaded`,
`Failed` if any source failed, `Loading` otherwise.

### Pattern 3 - mixing StoreResult flows with plain flows

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.store.load.StoreResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class StarsViewModel(
    private val starsRepository: StarsRepository,
) : ViewModel() {

    data class State(
        val starsResult: StoreResult<List<Star>>,  // StoreResult kept inside the state
        val filter: StarFilter,                    // plain flow value
    )

    val stateFlow: StateFlow<State> = combine(
        starsRepository.getStars(),      // Flow<StoreResult<List<Star>>>
        starsRepository.getFilter(),     // StateFlow<StarFilter> (e.g. store.queryFlow)
        ::State,
    ).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        State(StoreResult.Loading, StarFilter()),
    )

    fun updateFilter(filter: StarFilter) = starsRepository.updateFilter(filter)
}
```

### Pattern 4 - screen-local data: reducers

`StoreResultReducer<State>` turns a flow into
`StateFlow<StoreResult<State>>` and lets the view-model layer additional
data on top of each loaded value.

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.store.load.StoreResult
import com.elveum.store.reducers.StoreResultReducer
import com.elveum.store.reducers.storeResultToReducer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CartViewModel(
    private val cartRepository: CartRepository,
) : ViewModel() {

    data class State(
        val cart: List<CartProductItem>,           // from the store
        val isClearInProgress: Boolean = false,    // screen-local, updated manually
    )

    private val reducer: StoreResultReducer<State> = cartRepository
        .getFullCart()                              // Flow<StoreResult<List<CartProductItem>>>
        .storeResultToReducer(
            initialState = ::State,                 // (T) -> State, first Loaded value
            nextState = State::copy,                // (State, T) -> State, keeps local fields
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
        )

    val stateFlow: StateFlow<StoreResult<State>> = reducer.stateFlow

    fun clear() {
        viewModelScope.launch {
            try {
                reducer.updateState { it.copy(isClearInProgress = true) }
                cartRepository.clear()
                // also handle errors following the project rules
            } finally {
                reducer.updateState { it.copy(isClearInProgress = false) }
            }
        }
    }
}
```

Key facts:

- `initialState` builds the state from the first loaded value;
  `nextState` merges a re-emitted store value into the existing state, so
  manually-set fields survive store updates (`State::copy` of a data
  class whose **first constructor parameter** is the store value does
  exactly that).
- `reducer.updateState { }` changes the state **only while it is
  `Loaded`**; it is a no-op during `Loading`/`Failed`.
- `Loading` and `Failed` from the source pass through to `stateFlow`
  unchanged.
- For a source flow **without** `StoreResult`, use `toStoreResultReducer`
  with the same parameters - emissions are wrapped into
  `StoreResult.Loaded`, and the initial `stateFlow` value is
  `StoreResult.Loading`.
- If (and only if) the project already has a base view-model implementing
  `com.elveum.container.reducer.ReducerOwner`, the `scope`/`started`
  arguments can be omitted (context-parameter overloads).

## Composable Screens

Collect the view-model's `StateFlow` and render `StoreResult` with a
`when` clause:

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsState
import com.elveum.store.load.StoreResult

@Composable
fun ArticlesScreen(viewModel: ArticlesViewModel) {
    val result by viewModel.stateFlow.collectAsState()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val r = result) {
            StoreResult.Loading -> CircularProgressIndicator()
            is StoreResult.Failed -> Column {
                Text("Failed to load: ${r.exception.message}")
                // Reload the origin store directly from the result - no ViewModel
                // or repository function needed (see below).
                Button(onClick = { r.invalidate() }) { Text("Try again") }
            }
            is StoreResult.Loaded -> ArticlesContent(r.value, viewModel)
        }
    }
}
```

### Reloading data: try-again and pull-to-refresh

**Prefer reloading straight from the result.** A `StoreResult` can reload the
store that produced it via `result.invalidate()` - with no reference to the
store, and no `reload()` / `tryAgain()` / `refresh()` function threaded through
the ViewModel and repository. The composable that renders the result calls
`invalidate()` directly. `invalidate()` is fire-and-forget (safe to call from
an `onClick` / `onRefresh` lambda).

Whether the reload shows a spinner or keeps the current content is decided by
the request the observer subscribed with (via `observe(request)` or the
builder's `setLoadRequest`), **not** by the `invalidate()` call:

- **Try again** (error screen, no content visible) → default request → the
  observer goes through `Loading` again:

```kotlin
is StoreResult.Failed -> Button(onClick = { result.invalidate() }) {
    Text("Try again")
}
```

- **Pull-to-refresh** (content already visible) → subscribe with
  `LoadRequest.Silent` (per-observer `observe(LoadRequest.Silent)` or builder
  `setLoadRequest(LoadRequest.Silent)`) so `invalidate()` keeps the old content;
  the in-flight refresh shows through `result.isBackgroundLoading()`:

```kotlin
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.elveum.store.load.isBackgroundLoading
import com.elveum.store.load.invalidate

is StoreResult.Loaded -> PullToRefreshBox(
    isRefreshing = result.isBackgroundLoading(),
    onRefresh = { result.invalidate() },     // observer uses LoadRequest.Silent
) {
    ArticlesList(result.value)
}
```

**The old way still works.** A `refresh()` / `tryAgain()` function on the
ViewModel delegating to `store.invalidateAsync()` in the repository is
equivalent and remains valid. Reach for it when the reload needs extra logic
beyond a plain invalidate, when no `StoreResult` is available at the call site,
or when the project has already established that convention - in an existing
project, mirror what is already there rather than mixing both styles (see
"Working in an Existing Project"). For a new screen, prefer
`result.invalidate()`.

### Pagination

Paged stores require two things from the UI: report rendered items, and
render the next-page footer.

```kotlin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import com.elveum.container.subject.paging.PageState
import com.elveum.store.load.StoreResult
import com.elveum.store.load.nextPageState

is StoreResult.Loaded -> LazyColumn {
    itemsIndexed(result.value, key = { _, post -> post.id }) { index, post ->
        // tells the store which item is visible; triggers the next-page
        // load when within the fetch distance of the list end
        LaunchedEffect(index) { viewModel.onItemRendered(index) }
        PostCard(post)
    }
    item {
        when (val pageState = result.nextPageState) {
            PageState.Idle -> Unit                            // nothing to load
            PageState.Pending -> CircularProgressIndicator()  // next page loading
            is PageState.Error -> OutlinedButton(onClick = pageState.retry) {
                Text("Retry")                                  // retry ONLY the failed page
            }
        }
    }
}
```

IMPORTANT: use `itemsIndexed` when rendering paged lists, since this function provides 
a ready-to-use index that can be passed to `onItemRendered(index)` callback without
additional computation of the item's current position.

Error-channel rule for paged screens:

- Failed **initial** load → `StoreResult.Failed` → full-screen error with
  a non-silent try-again.
- Failed **next-page** load → list stays `Loaded`; the failure appears
  only in `result.nextPageState` → footer with `pageState.retry`.

## Dependency Injection

If the project has DI configured (Hilt, Koin, Dagger, manual), classes
that construct stores (usually repositories) must receive `StoreFactory`
through their constructor - do not call `StoreFactory.simpleStoreBuilder`
on the companion object directly in such projects, because injection
keeps repositories testable.

Hilt:

```kotlin
import com.elveum.store.StoreFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object StoreFactoryModule {
    @Provides
    fun provideStoreFactory(): StoreFactory = StoreFactory  // companion object IS the default impl
}
```

Koin:

```kotlin
import com.elveum.store.StoreFactory
import org.koin.dsl.module

val storeModule = module {
    single<StoreFactory> { StoreFactory }
}
```

Checklist:

1. Search the project for an existing binding before adding one:
   `grep -rn "StoreFactory" --include=*.kt` - look for a `@Provides` /
   `single` / `factory` declaration.
2. If none exists, add a module following the project's existing DI
   module conventions (location, naming, component/scope).
3. Verify the app compiles and (for Hilt) the kapt/ksp graph builds:
   `./gradlew :app:assembleDebug`.

In projects **without** DI, using the companion object directly
(`StoreFactory.simpleStoreBuilder<T>()`) is acceptable.

## Lifecycle and Scoping

- A class holding a store (usually a repository) must be **scoped**, not
  recreated per use - otherwise the in-memory cache is useless. In most
  cases: `@Singleton` (Hilt) / `single` (Koin).
- A store may be scoped narrower (e.g. created inside a view-model and
  used only by one screen) when the cached data is genuinely
  screen-local. Then its lifetime is the view-model's lifetime; the
  in-memory cache timeout still applies between recompositions.
- Stores need no explicit disposal: the cache is released automatically
  after the last observer unsubscribes plus the in-memory cache timeout.
  `whenActive` blocks are cancelled at the same moment.
- Do not wrap `observe()` flows in extra `shareIn`/`stateIn` at the
  repository level - sharing and caching are already handled by the
  store. Apply `stateIn` once, in the view-model.

## Working in an Existing Project

Before writing any code, inspect the project and mirror its conventions:

1. **Architecture/modules.** If repositories are split into `api`
   (interfaces) and `implementation` modules, put the new repository
   interface in the api module and the class creating stores in the
   implementation module. The `com.elveum:store` dependency goes where
   `StoreResult` is referenced: if the interface exposes
   `Flow<StoreResult<T>>`, the api module needs it as an `api(...)`
   dependency.
2. **Templates.** Open 1-2 existing view-models, repositories and data
   sources and copy their structure: constructor injection style, naming,
   error handling, dispatcher usage, state exposure.
3. **ReducerOwner.** Use the `ReducerOwner`/`AbstractViewModel` reducer
   shorthand only if the project already has it; otherwise pass
   `scope`/`started` explicitly.
4. **Code style.** Match the project's formatting, visibility modifiers,
   and detekt/ktlint rules; run the project's lint tasks after changes.
5. **Verification.** After integration run, at minimum:
   `./gradlew :<module>:compileDebugKotlin` and the project's unit tests
   for the touched modules.
