# Container Store - Public API Reference

All public symbols, grouped by package. Use these exact imports; do not
guess others.

## Imports

```kotlin
// Factory - entry point for creating stores
import com.elveum.store.StoreFactory

// Store interfaces (returned by builders; useful for explicit field types)
import com.elveum.store.stores.simple.SimpleStore
import com.elveum.store.stores.simple.SimpleQueryStore
import com.elveum.store.stores.keyed.KeyedStore
import com.elveum.store.stores.keyed.KeyedQueryStore  // withKeys() + withQuery(); per-key query API
import com.elveum.store.stores.keyed.PagedKeyedStore  // pagedStoreBuilder(...).withKeys(); per-key pagination
import com.elveum.store.stores.keyed.PagedKeyedQueryStore // pagedStoreBuilder(...).withKeys().withQuery()
import com.elveum.store.stores.paged.PagedStore
import com.elveum.store.stores.paged.PagedQueryStore
import com.elveum.store.stores.paged.PagedList        // data class PagedList(items, nextKey, metadata); also PagedList(items, nextKey, totalCount)

// Base store members (extensions / supporting types)
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.base.BaseStore
import com.elveum.store.stores.base.BaseSimpleStore
import com.elveum.store.stores.base.BasePagedStore
import com.elveum.store.stores.base.WithQuery
import com.elveum.store.stores.base.WithStoreLifecycleOwner // base of every store; provides whenActive { } (chain after build)

// Results and load requests
import com.elveum.store.load.StoreResult              // sealed: Loading / Loaded / Failed / Completed
import com.elveum.store.load.LoadRequest              // LoadRequest.Default, LoadRequest.Silent, LoadRequest.builder()
import com.elveum.store.load.LoadRequestSource        // enum: Default / Fresh / Offline
import com.elveum.store.load.LoadRequestBuilder

// StoreResult extensions
import com.elveum.store.load.map                      // StoreResult<T>.map { }
import com.elveum.store.load.storeMap                 // Flow<StoreResult<T>>.storeMap { }
import com.elveum.store.load.storeFlatMapResultLatest // inner flow emits StoreResult
import com.elveum.store.load.storeFlatMapLatest       // inner flow emits plain values
import com.elveum.store.load.storeListFlatMapLatest   // list + per-item keyed observation
import com.elveum.store.load.combineStores            // combine 2..5 (or List of) StoreResult flows
import com.elveum.store.load.firstGetOrThrow          // suspend: await first completed result or throw
import com.elveum.store.load.firstOptionalGetOrThrow  // suspend: await + unwrap Optional<T> or throw
import com.elveum.store.load.toOptional               // T?.toOptional(): Optional<T> (null -> empty)
import com.elveum.store.load.getOptionalValueOrNull   // StoreResult<Optional<T>> / Optional-store -> T?
import com.elveum.store.load.isOptionalEmpty          // Optional-store: true only when loaded & empty
import com.elveum.store.load.getOrNull                // StoreResult<T>.getOrNull(): T?  AND  BaseStore<T>.getOrNull() / KeyedStore.getOrNull(key)
import com.elveum.store.load.failureOrNull            // StoreResult<T>.failureOrNull(): Exception?  AND  BaseStore/KeyedStore variants
import com.elveum.store.load.filterLoaded             // Flow<StoreResult<T>>.filterLoaded(): Flow<T> (keep only loaded values)
import com.elveum.store.load.toStoreResult            // Container<T>.toStoreResult(): StoreResult<T> (interop)
import com.elveum.store.load.toContainer              // StoreResult<T>.toContainer(): Container<T> (interop)
import com.elveum.store.load.withMetadataFrom         // StoreResult<T>.withMetadataFrom(origin): merge metadata
import com.elveum.store.load.raw                       // StoreResult<T>.raw(): strip ALL metadata (for testing/equality)
import com.elveum.store.load.isLoaded                 // smart-cast contract
import com.elveum.store.load.isFailed                 // smart-cast contract
import com.elveum.store.load.isCompleted
import com.elveum.store.load.isForegroundLoading
import com.elveum.store.load.isBackgroundLoading      // true while a silent refresh runs
import com.elveum.store.load.hasAnyLoading
import com.elveum.store.load.nextPageState            // StoreResult<T>.nextPageState: PageState (paged stores)
import com.elveum.store.load.invalidate               // StoreResult<T>.invalidate(): reload the origin store
import com.elveum.store.load.onItemRendered           // StoreResult<T>.onItemRendered(index) (paged stores)
import com.elveum.store.load.totalPagedItemsCount     // StoreResult<T>.totalPagedItemsCount: Int (-1 if unknown); shortcut for result.metadata.totalPagedItemsCount

// Reducers (view-model layer)
import com.elveum.store.reducers.StoreResultReducer
import com.elveum.store.reducers.storeResultToReducer // Flow<StoreResult<T>> -> StoreResultReducer
import com.elveum.store.reducers.toStoreResultReducer // Flow<T>              -> StoreResultReducer
import com.elveum.store.reducers.combineStoresToReducer // combine 2..5 (or List of) StoreResult flows -> StoreResultReducer

// Contracts (optional; implemented by data sources instead of build(...) lambdas)
import com.elveum.store.contracts.SimpleContract
import com.elveum.store.contracts.SimpleSuspendingContract
import com.elveum.store.contracts.SimpleReactiveContract
import com.elveum.store.contracts.SimpleQueryContract
import com.elveum.store.contracts.SimpleQuerySuspendingContract
import com.elveum.store.contracts.SimpleQueryReactiveContract
import com.elveum.store.contracts.SimpleKeyedContract                   // withKeys(): fetch(key)
import com.elveum.store.contracts.SimpleKeyedSuspendingContract         // withKeys() + suspending storage: fetch(key), loadFromLocalStorage(key), saveToLocalStorage(key, data)
import com.elveum.store.contracts.SimpleKeyedReactiveContract           // withKeys() + reactive storage: fetch(key), observeLocalStorage(key), saveToLocalStorage(key, data)
import com.elveum.store.contracts.SimpleReactiveNoFetcherContract       // disableFetcher(): observe(): Flow<T>
import com.elveum.store.contracts.SimpleQueryReactiveNoFetcherContract  // disableFetcher() + withQuery: observe(query): Flow<T>
import com.elveum.store.contracts.SimpleKeyedReactiveNoFetcherContract  // withKeys() + disableFetcher(): observe(key): Flow<T>
// Keyed query contracts (withKeys() + withQuery())
import com.elveum.store.contracts.SimpleKeyedQueryContract              // fetch(key, query)
import com.elveum.store.contracts.SimpleKeyedQuerySuspendingContract
import com.elveum.store.contracts.SimpleKeyedQueryReactiveContract
import com.elveum.store.contracts.SimpleKeyedQueryReactiveNoFetcherContract
import com.elveum.store.contracts.PagedContract
import com.elveum.store.contracts.PagedSuspendingContract
import com.elveum.store.contracts.PagedQueryContract
import com.elveum.store.contracts.PagedQuerySuspendingContract
// Paged keyed contracts (pagedStoreBuilder(...).withKeys())
import com.elveum.store.contracts.PagedKeyedContract                    // fetch(key, pageKey)
import com.elveum.store.contracts.PagedKeyedQueryContract               // fetch(key, query, pageKey)
import com.elveum.store.contracts.PagedKeyedSuspendingContract          // fetch, saveToLocalStorage, loadFromLocalStorage
import com.elveum.store.contracts.PagedKeyedQuerySuspendingContract

// Exceptions
import com.elveum.store.exceptions.NoCachedDataException // emitted by offlineMode() without cache

// From the transitive com.elveum:container dependency
import com.elveum.container.subject.paging.PageState  // Idle / Pending / Error(retry)
import com.elveum.container.subject.paging.totalPagedItemsCount        // ContainerMetadata.totalPagedItemsCount: Int (-1 if unknown)
import com.elveum.container.subject.paging.TotalPagedItemsCountMetadata // attach a total count to a page
import com.elveum.container.ContainerMetadata         // marker interface for attachable metadata; subclass it for custom flags
import com.elveum.container.get                        // metadata.get<MyMetadata>(): MyMetadata? - read one entry by type
import com.elveum.container.EmptyMetadata              // the no-op metadata (PagedList / emit default)
import com.elveum.container.SourceType                // metadata: where the value came from
import com.elveum.container.LocalSourceType
import com.elveum.container.RemoteSourceType
import com.elveum.container.BackgroundLoadState
import com.elveum.container.reducer.ReducerOwner      // optional base interface for view-models
```

Notes:

- **Stores do not support nullable value types.** Every store generic is
  declared as `T : Any` (`SimpleStore<T : Any>`, `KeyedStore<Key : Any, T : Any>`,
  `PagedStore<T : Any>`, etc.), so `SimpleStore<User?>` will not compile. To
  represent an optional/absent value, use `java.util.Optional<T>` as the value
  type (e.g. `SimpleStore<Optional<User>>`) and unwrap it with
  `getOptionalValueOrNull()` / `firstOptionalGetOrThrow()`.
- There is **no** reactive local storage for paged stores
  (`PagedReactiveContract` does not exist; paged builders have no
  `addReactiveLocalStorage()`). It is planned for future versions.
- The `update { }` extension was **renamed to `updateIfSuccess`**
  (`store.updateIfSuccess { old -> new }`; keyed: `store.updateIfSuccess(key) { old -> new }`).
  It is a read-modify-write helper that transforms the currently loaded value and writes it
  back, running **only when the current value is `Loaded`** (no-op otherwise) - the rename
  makes that condition explicit. Use `updateWith(StoreResult.Loaded(new))` for an
  unconditional overwrite. `optimisticUpdate { }` and `updateWith(...)` are unchanged.
- `optimisticUpdate`, `updateIfSuccess`, `submitQuery` and `submitQueryAsync` operate on the
  in-memory cache, which exists **only while the store (or key) has an active observer**.
  Calling them for an unobserved store/key is a silent no-op.
- Keyed stores are created by calling `.withKeys<Key>()` on a simple or paged
  builder (there is no `keyedStoreBuilder`). `withKeys()` preserves the
  configuration applied before it (cache timeout, local storage, query, etc.).
- The zero-arg-context overloads of `storeResultToReducer` /
  `toStoreResultReducer` use Kotlin context parameters
  (`context(owner: ReducerOwner)`); they require the
  `-Xcontext-parameters` compiler flag. The overloads with explicit
  `scope` / `started` parameters work without the flag.

## Core Types

### StoreResult

Every store emits `Flow<StoreResult<T>>` with three states:

```kotlin
StoreResult.Loading                     // no cached data yet; load in progress
StoreResult.Loaded(value, metadata)     // success; value: T
StoreResult.Failed(exception, metadata) // load failed; exception: Exception
// Loaded and Failed share the supertype StoreResult.Completed<T>
```

Useful members on every result: `getOrNull()`, `failureOrNull()`,
`isLoaded()`, `isFailed()`, `isBackgroundLoading()`, `sourceType`,
`backgroundLoadState`, and (paged stores only) `nextPageState`.
`getOrNull()` / `failureOrNull()` also exist directly on the store
(`store.getOrNull()`, `store.getOrNull(key)`) as synchronous shortcuts for
`store.get().getOrNull()`. `Flow<StoreResult<T>>.filterLoaded()` reduces a
result flow to a `Flow<T>` of loaded values only.

A result can also act on the store that emitted it (no store reference
needed): `result.invalidate()` reloads the origin store, and
`result.onItemRendered(index)` reports a rendered item to a paged store
(may trigger the next-page load).

For `Flow<StoreResult<T>>` one-shot reads use `firstGetOrThrow()` (suspend;
awaits the first `Completed` result, returns the value or throws the
failure). For one-shot reads of `Optional`-valued stores: `firstOptionalGetOrThrow()`
(awaits + unwraps, throws on empty/failure).

For `Optional`-valued stores (the way to model "value may be absent", since
store generics are `T : Any`) there are extra helpers:

```kotlin
// userStore: SimpleStore<Optional<User>> (KeyedStore variants take a key arg)

// wrap a nullable fetch result into Optional (null -> Optional.empty()):
StoreFactory.simpleStoreBuilder<Optional<User>>().build { api.findUser().toOptional() }

// extract from a StoreResult:
val user: User? = userStore.get().getOptionalValueOrNull()

// or read the latest value straight off the store:
val sameUser: User? = userStore.getOptionalValueOrNull()          // get(key) for keyed

// true only when the latest result is a Loaded *empty* Optional
// (false while loading, on failure, or when a value is present):
if (userStore.isOptionalEmpty()) showEmptyState()                 // isOptionalEmpty(key) for keyed
```

### Container metadata (custom flags on a result)

Every `StoreResult` carries a `ContainerMetadata` bag, readable as
`result.metadata`. The library ships typed entries (source type, background-load
state, total paged items count, ...) and you can attach **your own**. Reach for
this when the data source reports extra flags alongside the value — e.g.
`hasNextPage`, an ETag, a server `lastUpdated`, or a "stale" marker — instead of
widening the value type `T`.

Define a metadata type (see `container/.../ContainerMetadata.kt` for the pattern —
any `data class`/`data object` implementing the marker interface):

```kotlin
import com.elveum.container.ContainerMetadata
import import com.elveum.container.get

data class PagingFlagsMetadata(
    val hasNextPage: Boolean,
) : ContainerMetadata

// Optional typed accessor, mirroring how the library exposes totalPagedItemsCount:
val ContainerMetadata.hasNextPage: Boolean
    get() = get<PagingFlagsMetadata>()?.hasNextPage ?: false
```

Attach it when producing the value:

```kotlin
// Paged store — via the PagedList metadata argument (defaults to EmptyMetadata):
PagedList(items = page.items, nextKey = page.nextKey, metadata = PagingFlagsMetadata(page.hasNext))
// Custom paged loader (buildCustom): emitPage(items, metadata = PagingFlagsMetadata(...))
// Custom simple/keyed loader (buildCustom): emit(value, metadata = PagingFlagsMetadata(...))
```

Read it back from any result:

```kotlin
val flags = result.metadata.get<PagingFlagsMetadata>()   // PagingFlagsMetadata? (null if absent)
if (result.metadata.hasNextPage) { /* ... */ }           // via the typed accessor
```

**Stripping metadata for tests** — because metadata rides along on every
`StoreResult`, two results with the same value/exception but different metadata
are **not** equal. When asserting in tests, call `result.raw()` to drop all
metadata and compare only the value/exception:

```kotlin
assertEquals(StoreResult.Loaded(expectedUser), store.get().raw())
```

`raw()` returns `Loading` unchanged, and `Loaded`/`Failed` with `EmptyMetadata`.

Combine several entries with `+` (a same-typed entry on the right replaces the
one on the left): `PagingFlagsMetadata(true) + EtagMetadata(tag)`. Metadata is
propagated from the loader/`PagedList` to the emitted `StoreResult`, so it is the
clean channel for out-of-band flags. The built-in `totalPagedItemsCount` shortcut
(and `defaultMetadata(...)`) are implemented exactly this way — `get<T>()` plus a
default.

### LoadRequest

A `LoadRequest` is supplied in only two places: `observe(request? = null)`
(per-observer) and the builder's `setLoadRequest(...)` (the default).
`invalidate` / `invalidateAsync` / `submitQuery` / `submitQueryAsync` do
**not** take a request - they just trigger a reload, and each observer keeps
receiving data according to the request IT subscribed with (a store/key may
have several observers, each with its own request: fresh, offline,
keep-content, ...). `observe` takes a *nullable* `LoadRequest`; passing `null`
(or omitting it) falls back to the store's configured default request instead
of always meaning `LoadRequest.Default`:

| Request | Effect |
|---------|--------|
| `LoadRequest.Default` | Fetch only if not cached; observers see `Loading` during the load |
| `LoadRequest.Silent` | Keep currently shown content while reloading, both on invalidate **and** on query change (pull-to-refresh); progress visible via `isBackgroundLoading()`. `= builder().keepContentOnLoad().keepContentOnQuery().build()` |
| `LoadRequest.builder()` | Custom: `freshMode()` (skip caches), `offlineMode()` (cache only; emits `NoCachedDataException` if empty), then `build()`. Keep-content is set **per reload trigger**: `keepContentOnLoad` / `keepContentOnLoadAndError` for invalidation (`invalidate`/`result.invalidate()`), `keepContentOnQuery` / `keepContentOnQueryAndError` for query changes (`submitQuery`/query flow). Each defaults to showing `Loading` if its option is not set, so `keepContentOnLoad()` alone reloads silently but still shows `Loading` on a query change. Each option takes `replaceErrorsOnReload`/`replaceErrorsOnQuery = true`; pass `false` to also keep a current error visible while reloading. Each family may be set at most once (enforced by the fluent builder type) | 

### Store behaviour (all types)

- Lazy: the first observer triggers the fetch; all observers share one
  in-memory cache.
- The cache is released after the last observer unsubscribes plus the
  configured timeout (`setInMemoryCacheTimeout`, default **5 seconds**).
- `setCoroutineContext(Dispatchers.IO)` sets the context for
  fetch/storage callbacks.
- `setLoadRequest(loadRequest: LoadRequest)` sets the default `LoadRequest`
  used whenever `observe` is called without an explicit request (default:
  `LoadRequest.Default`).
- `setLoadRequest(flow: Flow<LoadRequest>)` - reactive overload: the flow's
  latest emission becomes the current default request, so the loading policy can
  change at runtime without recreating the store or touching `observe(...)` call
  sites. Use it to switch offline/online automatically when connectivity drops,
  or to honor a user-controlled "offline mode" / "data saver" flag stored in
  DataStore/preferences: map the flag `Flow<Boolean>` into a `LoadRequest`
  (`if (offline) LoadRequest.builder().offlineMode().build() else LoadRequest.Default`).
  See patterns.md ("Reactive default request").
- `whenActive(block: suspend Store.() -> Unit): Store` (chained after `build`) —
  runs `block` **while the store is active** (from the first observer until the
  cache is released) and cancels it when the store goes inactive. `this` inside
  the block is the store itself, so the block can call `updateIfSuccess { }`,
  `updateWith(...)`, `submitQueryAsync(...)`, etc. Use it to wire a store to
  **external events / other stores** — e.g. subscribe to another store's update
  events and patch this store's loaded value. Returns the same store, so the call
  chains inline. See patterns.md ("Relations between stores"). Keyed variant
  receives `KeyedStore<Key, T>` as `this` (use `updateIfSuccess(key) { }`).

## Queries (withQuery)

`withQuery` parameterizes a store by a runtime value (search text, filter, sort
order) that re-triggers fetching whenever it changes. There are **two families**,
chosen by whether the store or the caller owns the query value. Both accept an
optional `debounceMillis` (delay after a query change before reloading; default
`0`), and both make the `build(...)` fetch/storage lambdas receive the query `Q`
(`build { query -> ... }`, keyed `build { key, query -> ... }`).

### 1. Imperative query — the store owns the query

The store holds the current query and exposes an API to change it. Returns a
*query-aware* store (`SimpleQueryStore` / `KeyedQueryStore` / `PagedQueryStore` /
`PagedKeyedQueryStore`).

Signature (present on every non-external builder: simple, suspending, reactive,
no-fetcher, keyed, paged):

```kotlin
fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long = 0): <QueryBuilder>
// simpleStoreBuilder<T>()                    -> SimpleQueryBuilder<Q, T>       -> SimpleQueryStore<Q, T>
// simpleStoreBuilder<T>().withKeys<Key>()    -> SimpleKeyedQueryBuilder<Key,Q,T> -> KeyedQueryStore<Key,Q,T>
// pagedStoreBuilder(...)                     -> PagedQueryBuilder<Q,PageKey,T> -> PagedQueryStore<Q, T>
```

- `initialQuery` — query used for the first load (keyed: the seed for every key).

Query API on the resulting store:

```kotlin
store.queryFlow                     // StateFlow<Q> — current query
store.submitQueryAsync(newQuery)    // fire-and-forget re-fetch
store.submitQuery(newQuery)         // suspend variant; suspends until applied
// keyed store — each key holds its OWN query:
keyedStore.observeQueryFlow(key)    // StateFlow<Q> for that key
keyedStore.submitQueryAsync(key, newQuery)
```

`submitQuery` / `submitQueryAsync` act on the in-memory cache and are a **no-op
when the store/key has no active observer** (observe first).

### 2. External query flow — the caller owns the query

The query comes from a `Flow`/`StateFlow` you already have (e.g. a ViewModel's
`searchText: StateFlow<String>`). The store simply follows it. Returns a **plain**
store (`SimpleStore` / `KeyedStore` / `PagedStore` / `PagedKeyedStore`) with **no**
`queryFlow` / `submitQuery` — the external flow is the single source of truth.

Two overloads on every builder that supports `withQuery`:

```kotlin
// primary — explicit initial query; accepts any cold/hot Flow<Q>:
fun <Q : Any> withQuery(
    initialQuery: Q,
    debounceMillis: Long = 0,
    queryFlow: () -> Flow<Q>,
): <ExternalQueryBuilder>

// convenience — StateFlow<Q>; initial query is taken from queryFlow().value:
fun <Q : Any> withQuery(
    debounceMillis: Long = 0,
    queryFlow: () -> StateFlow<Q>,
): <ExternalQueryBuilder>

// keyed builders — the lambda receives the Key, so each key follows its own flow:
fun <Q : Any> withQuery(initialQuery: Q, debounceMillis: Long = 0, queryFlow: (Key) -> Flow<Q>): ...
fun <Q : Any> withQuery(debounceMillis: Long = 0, queryFlow: (Key) -> StateFlow<Q>): ...
```

Behaviour:

- First load uses `initialQuery` (or `stateFlow.value`); each later emission
  re-fetches (paged: resets to the first page).
- The flow is collected **only while the store/key is active** (has an observer,
  or within the cache-timeout window); emissions while inactive do nothing.
- `build(...)` still returns the plain (non-query) store; contracts are the same
  `...QueryContract` types as the imperative form.

### Order independence

`withQuery` may be chained **before or after** `addSuspendingLocalStorage()` /
`addReactiveLocalStorage()` / `disableFetcher()` / `withKeys()` — both orders
build the same store:

```kotlin
simpleStoreBuilder<T>().addSuspendingLocalStorage().withQuery { flow }   // OK
simpleStoreBuilder<T>().withQuery { flow }.addSuspendingLocalStorage()   // same store
```

With keys, the ORDER picks the semantic:

```kotlin
// ONE flow shared across all keys (every active key reloads on each emission):
simpleStoreBuilder<T>().withQuery { sharedFlow }.withKeys<Key>()
// PER-KEY flow (each key follows its own stream):
simpleStoreBuilder<T>().withKeys<Key>().withQuery { key -> flowFor(key) }
```

## Typical Operations

```kotlin
// Observe (Flow<StoreResult<T>>); lazy, shared, cached:
store.observe()                          // observe(request: LoadRequest? = null)
store.observe(LoadRequest.Silent)        // per-observer request; null -> builder default
store.observe(key)                       // keyed store; observe(key, request: LoadRequest? = null)

// Read the latest result synchronously (no collection):
val current: StoreResult<T> = store.get()
val currentForKey: StoreResult<T> = store.get(key)   // keyed store
val value: T? = store.getOrNull()                    // = store.get().getOrNull(); getOrNull(key) for keyed
val error: Exception? = store.failureOrNull()        // failureOrNull(key) for keyed

// Local-only store (no remote fetcher): observe a local reactive Flow only.
// Supported by simple & keyed stores; combines with withQuery. Also the right
// way to model an app-owned / event-bus value: observe a MutableStateFlow and
// mutate it to update (do NOT fake a fetcher like build { awaitCancellation() }
// or build { Optional.empty() } — see patterns.md "Externally-driven stores").
StoreFactory.simpleStoreBuilder<Settings>()
    .disableFetcher()
    .build(onObserve = dataStore::observeSettings)   // () -> Flow<Settings>
StoreFactory.simpleStoreBuilder<Book>()
    .withKeys<BookId>()
    .disableFetcher()
    .build(onObserve = { id -> dao.observeBook(id) }) // (Key) -> Flow<T>

// Custom loader (no local storage) that emits values MANUALLY - use it only in rare cases
// that can't be covered by other API provided by the library.
StoreFactory.simpleStoreBuilder<Article>()
    .buildCustom { /* this: Emitter<Article> */ emit(value1); emit(value2); emit(value3) }
StoreFactory.simpleStoreBuilder<Book>().withKeys<BookId>()
    .buildCustom { id -> emit(dao.peek(id)); emit(api.fetch(id)) }   // keyed: block receives the key
// paged builder: block receives the page key and gets a PageEmitter receiver:
StoreFactory.pagedStoreBuilder<Int, Post>(initialKey = 0, itemId = Post::id)
    .buildCustom { pageKey -> emitPage(items); emitNextKey(nextKey) }

// Paged store reporting the total item count; read it back from the result:
PagedList(items = page.items, nextKey = page.nextKey, totalCount = page.total)
val total: Int = result.totalPagedItemsCount  // -1 if not provided; = result.metadata.totalPagedItemsCount

// Await a single completed result (skips Loading), or throw on failure:
suspend fun loadOnce(): T = store.observe().firstGetOrThrow()

// Reload the store. From the UI, PREFER reloading straight from the rendered
// result - no store/ViewModel/repository reference needed:
result.invalidate()                      // reloads the origin store (fire-and-forget)
// invalidate/invalidateAsync (on the store) are the reference-based equivalents,
// useful when no StoreResult is at hand. All of them take NO request; each
// observer keeps receiving data per the request it subscribed with (via
// observe(...) or the builder default). For a silent refresh (pull-to-refresh)
// subscribe with LoadRequest.Silent - observe(LoadRequest.Silent) or builder
// setLoadRequest(LoadRequest.Silent) - so old content stays visible:
store.invalidateAsync()                  // fire-and-forget reload
suspend fun refresh() = store.invalidate()  // suspend until done

// Keys currently active (have an observer, or within the cache-timeout window):
val active: StateFlow<Set<Key>> = keyedStore.activeKeys

// Read-modify-write the loaded value AFTER the real data source changed
// (updateIfSuccess = the old update { } extension, renamed; runs only if Loaded).
// NOTE: only needed WITHOUT a reactive local source. With addReactiveLocalStorage()
// (or a disableFetcher() store observing a Flow), write to the local source instead
// and let its Flow propagate - do NOT also call updateIfSuccess/updateWith. See
// patterns.md "Mutations (add / update / remove)".
suspend fun renameProfile(newName: String) {
    dataSource.renameProfile(newName)
    store.updateIfSuccess { it.copy(name = newName) }
}

// Unconditional overwrite regardless of current state:
suspend fun clear() {
    cartDataSource.clear()
    store.updateWith(StoreResult.Loaded(emptyList()))
}

// Optimistic update: emit the expected value first, then do the real
// update; emitted values are AUTO-REVERTED if the block throws:
suspend fun toggleLike(post: Post) {
    store.optimisticUpdate { oldList ->
        val updated = post.copy(isLiked = !post.isLiked)
        emit(oldList.map { if (it.id == updated.id) updated else it })
        feedDataSource.toggleLike(updated)   // failure -> cache reverted
    }
}

// Keyed variants take the key first:
store.optimisticUpdate(productId) { old -> emit(old.copy(title = title)); api.rename(productId, title) }
// Keyed read-modify-write (runs only if that key is currently Loaded):
store.updateIfSuccess(productId) { it.copy(isRead = true) }

// Replace the cached result entirely (any StoreResult, no previous value needed):
store.updateWith(StoreResult.Loaded(value))
store.updateWith(productId, StoreResult.Loaded(value))   // keyed store

// Query stores - see the "Queries (withQuery)" section above for the full
// signatures and the two families (imperative vs external flow). Quick calls:

// Imperative form (store owns the query):
store.queryFlow                          // StateFlow<Q> - current query
store.submitQueryAsync(newQuery)         // re-fetch
keyedQueryStore.observeQueryFlow(key)    // StateFlow<Q> for that key (per-key query)
keyedQueryStore.submitQueryAsync(key, newQuery)

// External flow form (caller owns the query; result is a PLAIN store, no submitQuery):
val store = StoreFactory.simpleStoreBuilder<List<Image>>()
    .withQuery(debounceMillis = 500) { searchQuery }   // StateFlow<Q>: initial query = searchQuery.value
    .build { query -> api.fetch(query) }               // -> SimpleStore<List<Image>>

// whenActive { } - run a coroutine while the store is observed (cancelled on cache
// release); chain it after build(). Use it to connect the store to external events
// or other stores. `this` is the store, so update helpers are callable directly:
val catsStore = StoreFactory.simpleStoreBuilder<List<Cat>>()
    .build(onFetch = catsDataSource::fetchCats)
    .whenActive {                              // this: SimpleStore<List<Cat>>
        catEvents.observeCatEvents().collect { event ->
            updateIfSuccess { cats ->          // no-op unless currently Loaded
                cats.map { if (it.id == event.cat.id) event.cat else it }
            }
        }
    }
```
