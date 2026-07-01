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
import com.elveum.store.stores.keyed.update           // update(key) { } extension for KeyedStore
import com.elveum.store.stores.paged.PagedStore
import com.elveum.store.stores.paged.PagedQueryStore
import com.elveum.store.stores.paged.PagedList        // data class PagedList(items, nextKey, metadata); also PagedList(items, nextKey, totalCount)

// Base store members (extensions / supporting types)
import com.elveum.store.stores.base.update            // update { } extension for SimpleStore/PagedStore
import com.elveum.store.stores.base.OptimisticUpdateScope
import com.elveum.store.stores.base.BaseStore
import com.elveum.store.stores.base.BaseSimpleStore
import com.elveum.store.stores.base.BasePagedStore
import com.elveum.store.stores.base.WithQuery
import com.elveum.store.stores.base.WithStoreLifecycleOwner

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
import com.elveum.store.load.isLoaded                 // smart-cast contract
import com.elveum.store.load.isFailed                 // smart-cast contract
import com.elveum.store.load.isCompleted
import com.elveum.store.load.isForegroundLoading
import com.elveum.store.load.isBackgroundLoading      // true while a silent refresh runs
import com.elveum.store.load.hasAnyLoading
import com.elveum.store.load.nextPageState            // StoreResult<T>.nextPageState: PageState (paged stores)
import com.elveum.store.load.invalidate               // StoreResult<T>.invalidate(): reload the origin store
import com.elveum.store.load.onItemRendered           // StoreResult<T>.onItemRendered(index) (paged stores)

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
import com.elveum.store.contracts.KeyedContract
import com.elveum.store.contracts.KeyedSuspendingContract
import com.elveum.store.contracts.KeyedReactiveContract
import com.elveum.store.contracts.SimpleReactiveNoFetcherContract       // disableFetcher(): observe(): Flow<T>
import com.elveum.store.contracts.SimpleQueryReactiveNoFetcherContract  // disableFetcher() + withQuery: observe(query): Flow<T>
import com.elveum.store.contracts.KeyedReactiveNoFetcherContract        // disableFetcher(): observe(key): Flow<T>
import com.elveum.store.contracts.PagedContract
import com.elveum.store.contracts.PagedSuspendingContract
import com.elveum.store.contracts.PagedQueryContract
import com.elveum.store.contracts.PagedQuerySuspendingContract

// Exceptions
import com.elveum.store.exceptions.NoCachedDataException // emitted by offlineMode() without cache

// From the transitive com.elveum:container dependency
import com.elveum.container.subject.paging.PageState  // Idle / Pending / Error(retry)
import com.elveum.container.subject.paging.totalPagedItemsCount        // ContainerMetadata.totalPagedItemsCount: Int (-1 if unknown)
import com.elveum.container.subject.paging.TotalPagedItemsCountMetadata // attach a total count to a page
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
- The `update` extension exists in two packages: import
  `com.elveum.store.stores.base.update` for simple/paged stores and
  `com.elveum.store.stores.keyed.update` for keyed stores.
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

### LoadRequest

Accepted by `observe`, `invalidate`, `invalidateAsync`, `submitQuery`:

| Request | Effect |
|---------|--------|
| `LoadRequest.Default` | Fetch only if not cached; observers see `Loading` during the load |
| `LoadRequest.Silent` | Keep currently shown content while reloading (pull-to-refresh); progress visible via `isBackgroundLoading()` |
| `LoadRequest.builder()` | Custom: `freshMode()` (skip caches), `offlineMode()` (cache only; emits `NoCachedDataException` if empty), `keepContentOnLoad()`, `keepContentOnLoadAndError()`, then `build()` |

### Store behaviour (all types)

- Lazy: the first observer triggers the fetch; all observers share one
  in-memory cache.
- The cache is released after the last observer unsubscribes plus the
  configured timeout (`setInMemoryCacheTimeout`, default **5 seconds**).
- `setCoroutineContext(Dispatchers.IO)` sets the context for
  fetch/storage callbacks.

## Typical Operations

```kotlin
// Observe (Flow<StoreResult<T>>); lazy, shared, cached:
store.observe()
store.observe(key)                       // keyed store

// Read the latest result synchronously (no collection):
val current: StoreResult<T> = store.get()
val currentForKey: StoreResult<T> = store.get(key)   // keyed store
val value: T? = store.getOrNull()                    // = store.get().getOrNull(); getOrNull(key) for keyed
val error: Exception? = store.failureOrNull()        // failureOrNull(key) for keyed

// Local-only store (no remote fetcher): observe a local reactive Flow only.
// Supported by simple & keyed stores; combines with withQuery.
StoreFactory.simpleStoreBuilder<Settings>()
    .disableFetcher()
    .build(onObserve = dataStore::observeSettings)   // () -> Flow<Settings>
StoreFactory.keyedStoreBuilder<BookId, Book>()
    .disableFetcher()
    .build(onObserve = { id -> dao.observeBook(id) }) // (Key) -> Flow<T>

// Paged store reporting the total item count; read it back from the result:
PagedList(items = page.items, nextKey = page.nextKey, totalCount = page.total)
val total: Int = result.metadata.totalPagedItemsCount  // -1 if not provided

// Await a single completed result (skips Loading), or throw on failure:
suspend fun loadOnce(): T = store.observe().firstGetOrThrow()

// Silent refresh (pull-to-refresh): old content stays visible
store.invalidateAsync(LoadRequest.Silent)

// Non-silent reload ("Try again" after an error): shows Loading
store.invalidateAsync()                  // = LoadRequest.Default
suspend fun refresh() = store.invalidate()  // suspend until done

// Plain cache update AFTER the real data source has been changed:
import com.elveum.store.stores.base.update
suspend fun clear() {
    cartDataSource.clear()
    store.update { emptyList() }
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
import com.elveum.store.stores.keyed.update
store.update(productId) { it.copy(isRead = true) }

// Replace the cached result entirely (any StoreResult, no previous value needed):
store.updateWith(StoreResult.Loaded(value))
store.updateWith(productId, StoreResult.Loaded(value))   // keyed store

// Query stores:
store.queryFlow                          // StateFlow<Q> - current query
store.submitQueryAsync(newQuery)         // re-fetch; default LoadRequest.Silent
suspend fun search(q: Q) = store.submitQuery(q)  // suspends
```
