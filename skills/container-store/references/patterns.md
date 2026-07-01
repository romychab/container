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
| `keyedStoreBuilder` | `KeyedContract<Key, T>` |
| `keyedStoreBuilder` + suspending/reactive storage | `KeyedSuspendingContract` / `KeyedReactiveContract` |
| `keyedStoreBuilder` + `disableFetcher` | `KeyedReactiveNoFetcherContract<Key, T>` |
| `pagedStoreBuilder` | `PagedContract<PageKey, T>` |
| `pagedStoreBuilder` + `addSuspendingLocalStorage` | `PagedSuspendingContract<PageKey, T>` |
| `pagedStoreBuilder` + `withQuery` | `PagedQueryContract<Q, PageKey, T>` |
| `pagedStoreBuilder` + `withQuery` + suspending storage | `PagedQuerySuspendingContract<Q, PageKey, T>` |

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

    private val store = storeFactory.keyedStoreBuilder<Long, ProductDetails>()
        .build(onFetch = productsDataSource::fetchProductById)

    fun getProductById(id: Long): Flow<StoreResult<ProductDetails>> = store.observe(id)

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
        .build(
            onFetch = { pageKey ->
                val page = feedDataSource.fetchPage(pageKey)
                PagedList(items = page.posts, nextKey = page.nextKey)
            }
        )

    fun getFeed(): Flow<StoreResult<List<Post>>> = store.observe()
    fun onItemRendered(index: Int) = store.onItemRendered(index)
    fun refresh() = store.invalidateAsync(LoadRequest.Silent)
    fun tryAgain() = store.invalidateAsync()
}
```

See "Typical Operations" in [api.md](api.md) for observing, refreshing,
updates and queries.

IMPORTANT: Usually there is NO need to set dispatcher via `setCoroutineContext(Dispatchers.IO)`, since
most of modern IO operations (Room database, Retrofit) already operate on non-UI thread.

### Relations between stores (entity dependencies)

When data managed by one store depends on another store (e.g. editing an
entity on a details screen must update the master list), use
`whenActive { }` on the dependent store. The block runs while the store
has observers and is cancelled when the cache is released:

```kotlin
import com.elveum.store.StoreFactory
import kotlinx.coroutines.flow.Flow
import com.elveum.store.stores.base.update

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
                update { oldList ->
                    oldList.map { if (it.id == event.cat.id) event.cat else it }
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

    fun refresh() = feedRepository.refresh()        // silent
    fun tryAgain() = feedRepository.tryAgain()      // non-silent
    fun onItemRendered(index: Int) = feedRepository.onItemRendered(index) // for paged data
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
                Button(onClick = viewModel::tryAgain) { Text("Try again") } // non-silent reload
            }
            is StoreResult.Loaded -> ArticlesContent(r.value, viewModel)
        }
    }
}
```

### Pull-to-refresh (silent) vs try-again (non-silent)

- **Try again** (error screen button) → `store.invalidateAsync()` -
  observers go through `Loading` again.
- **Pull-to-refresh** → `store.invalidateAsync(LoadRequest.Silent)` - the
  old content stays; the in-flight refresh is visible through
  `result.isBackgroundLoading()`:

```kotlin
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.elveum.store.load.isBackgroundLoading

is StoreResult.Loaded -> PullToRefreshBox(
    isRefreshing = result.isBackgroundLoading(),
    onRefresh = viewModel::refresh,          // invalidateAsync(LoadRequest.Silent)
) {
    ArticlesList(result.value)
}
```

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
