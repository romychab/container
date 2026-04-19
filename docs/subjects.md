# Subjects

This page covers `LazyFlowSubject` building block for on-demand data loading along
with the metadata system that threads cross-cutting information through containers.

## Table of Contents

- [LazyFlowSubject](#lazyflowsubject)
  - [Basic Usage](#basic-usage)
  - [Listening and Caching Behaviour](#listening-and-caching-behaviour)
  - [Reloading Data](#reloading-data)
  - [Pushing Values Directly](#pushing-values-directly)
  - [Replacing the Loader](#replacing-the-loader)
  - [Load Triggers](#load-triggers)
  - [ContainerConfiguration](#containerconfiguration)
  - [listenReloadable](#listenreloadable)
- [SubjectFactory](#subjectfactory)
  - [Testability](#testability)
  - [Convenience Factory Functions](#convenience-factory-functions)
- [Metadata](#metadata)
  - [ContainerMetadata](#containermetadata)
  - [SourceType](#sourcetype)
  - [ReloadFunctionMetadata](#reloadfunctionmetadata)
  - [BackgroundLoadStateMetadata](#backgroundloadstatemetadata)
  - [LoadTrigger](#loadtrigger)
  - [Custom Metadata](#custom-metadata)
- [Flow Dependencies in Loader Functions](#flow-dependencies-in-loader-functions)
  - [dependsOnContainerFlow](#dependsoncontainerflow)
  - [dependsOnFlow](#dependsonflow)
  - [Key Stability](#key-stability)

## LazyFlowSubject

`LazyFlowSubject<T>` converts a suspend loader function into a
`Flow<Container<T>>`. The loader runs lazily and its result is automatically
cached for subsequent subscribers.

### Basic Usage

```kotlin
class ProductRepository(
    private val localDataSource: ProductsLocalDataSource,
    private val remoteDataSource: ProductsRemoteDataSource,
) {

    private val productsSubject = LazyFlowSubject.create {
        // Step 1: emit local (cached) data immediately if available:
        val local = localDataSource.getProducts()
        if (local != null) emit(local)

        // Step 2: fetch from remote and update the local cache:
        val remote = remoteDataSource.getProducts()
        localDataSource.save(remote)
        emit(remote)
    }

    // ListContainerFlow<T> is an alias for Flow<Container<List<T>>>
    fun listenProducts(): ListContainerFlow<Product> = productsSubject.listen()

    fun reload() = productsSubject.reloadAsync()
}
```

The lambda passed to `LazyFlowSubject.create` is the *loader function*. Inside
it, you have access to the `Emitter<T>` receiver, which provides:

- `emit(value)`: emit a loaded value
- `loadTrigger: LoadTrigger`: why the loader was called (see [Load Triggers](#load-triggers))
- `metadata: ContainerMetadata`: metadata from the triggering call
- `dependsOnFlow { }` / `dependsOnContainerFlow { }`: subscribe to external
  flows (see [Flow Dependencies](#flow-dependencies-in-loader-functions))

### Listening and Caching Behaviour

- The loader starts running the first time `listen()` is called and a
  subscriber begins collecting
- The latest loaded value is cached; new subscribers receive it immediately
  without re-running the loader
- When the last subscriber stops collecting, a timer starts
  (default: **1 second**). If no new subscriber appears before the timer
  expires, the cached value is cleared and the loader will run again on the
  next subscription
- You can customise the timeout:

```kotlin
LazyFlowSubject.create(
    cacheTimeoutMillis = 60_000L, // 60 seconds
) {
    emit(loadData())
}
```

### Reloading Data

Re-run the previous loader without changing it:

```kotlin
// fire-and-forget:
subject.reloadAsync()

// or observe the reload result:
subject.reload().collect { value -> ... }
```

You can reload silently (the existing cached value is kept visible while the
new load runs in the background):

```kotlin
subject.reloadAsync(LoadConfig.SilentLoading)
```

### Pushing Values Directly

Place a value into the subject without running the loader:

```kotlin
subject.updateWith(successContainer("immediate value"))

// update the existing container:
subject.updateWith { oldContainer ->
    oldContainer.map { it + " (updated)" }
}

// shorthand that only runs if the current container is Success:
subject.updateIfSuccess { oldValue ->
    oldValue.copy(isFavorite = true)
}
```

After `updateWith`, calling `reload()` re-runs the *previous* loader function.

### Replacing the Loader

You can replace the loader function at any time:

```kotlin
// assign a new multi-value loader (returns a Flow<T> of the new results):
val flow: Flow<T> = subject.newLoad {
    emit(step1())
    emit(step2())
}

// fire-and-forget variant:
subject.newAsyncLoad { emit(loadData()) }

// single-value loader (suspends until the value is loaded):
val result: T = subject.newSimpleLoad { fetchData() }

// single-value loader (fire-and-forget):
subject.newSimpleAsyncLoad { fetchData() }
```

The `newLoad` / `newSimpleLoad` functions also accept a `loadConfig` argument and
an optional `metadata` argument:

- `loadConfig = LoadConfig.SilentLoading`: the existing cached value stays visible
  while the new load runs in the background. Emitted containers carry
  `backgroundLoadState = BackgroundLoadState.Loading` when `emitBackgroundLoads` is
  enabled (e.g. via `listenReloadable()`).
- `metadata`: arbitrary data attached to this load call; accessible inside the
  loader function via the `metadata` property on the `Emitter` receiver.
  See [Metadata](#metadata) for available types and how to define custom ones.

```kotlin
subject.newAsyncLoad(
    loadConfig = LoadConfig.SilentLoading,
    metadata = SourceTypeMetadata(RemoteSourceType),
) {
    emit(fetchRemote())
}
```

### Load Triggers

Inside the loader function, `loadTrigger` tells you *why* the loader was
called. Use it to skip unnecessary steps, e.g. the local-cache check on
explicit reloads:

```kotlin
private val productsSubject = LazyFlowSubject.create {
    if (loadTrigger != LoadTrigger.Reload) {
        val local = localDataSource.getProducts()
        if (local != null) emit(local)
    }
    val remote = remoteDataSource.getProducts()
    localDataSource.save(remote)
    emit(remote)
}
```

| Value | Meaning |
|-------|---------|
| `LoadTrigger.NewLoad` | Loader was set with `newLoad()` or `create {}` |
| `LoadTrigger.Reload` | Loader was re-triggered by `reload()` / `reloadAsync()` |
| `LoadTrigger.CacheExpired` | Cache timeout elapsed; the next subscriber triggered a fresh load |

### ContainerConfiguration

Pass a `ContainerConfiguration` to `listen()` to control what extra metadata
is attached to emitted containers:

```kotlin
val flow: Flow<Container<List<Product>>> = subject.listen(
    configuration = ContainerConfiguration(
        emitReloadFunction    = true,  // attach a reload function to each container
        emitBackgroundLoads   = true,  // set BackgroundLoadState metadata value to Loading while reloading silently
    )
)
```

- `emitReloadFunction` - each emitted `Container.Success` / `Container.Error`
  carries a `reloadFunction` that, when called, triggers `reloadAsync()` on
  the subject. Useful for UI components that need to offer a "retry" button
  without knowing about the subject directly.
- `emitBackgroundLoads` - while a silent reload is in progress, emitted
  containers have `backgroundLoadState` metadata property. Useful when you want to
  display the current loaded data along with an additional indication that something is being loaded
  right now (for example, PullToRefresh behavior)

### listenReloadable

`listenReloadable()` is a convenience shorthand that enables both flags:

```kotlin
// equivalent to listen(ContainerConfiguration(emitReloadFunction = true, emitBackgroundLoads = true))
fun listenProducts(): Flow<Container<List<Product>>> = subject.listenReloadable()
```

## SubjectFactory

`SubjectFactory` is an interface for creating `LazyFlowSubject` instances.
Use it instead of calling `LazyFlowSubject.create {}` directly.

### Testability

Injecting `SubjectFactory` via DI (e.g. Hilt) makes it straightforward to
replace the real factory with a test double that returns pre-configured or mock
subjects:

```kotlin
class ProductRepository(
    private val subjectFactory: SubjectFactory = SubjectFactory,
) {
    private val subject = subjectFactory.createSubject {
        delay(1000)
        emit("my-item")
    }

    fun listen(): ContainerFlow<String> = subject.listenReloadable()
}
```

In production, bind `DefaultSubjectFactory` with your chosen cache timeout:

```kotlin
@Provides
@Singleton
fun provideSubjectFactory(): SubjectFactory =
    DefaultSubjectFactory(cacheTimeoutMillis = 60_000L)
```

In tests, replace it with any fake implementation of `SubjectFactory` or a mock.

The global default can also be overridden for tests:

```kotlin
SubjectFactory.setFactory(FakeSubjectFactory())
// ... run tests ...
SubjectFactory.resetFactory()
```

### Convenience Factory Functions

`SubjectFactory` provides several extension functions to reduce boilerplate:

```kotlin
// Create a LazyFlowSubject with a simple (single-value) loader:
val subject: LazyFlowSubject<String> = subjectFactory.createSimpleSubject(
    sourceType  = RemoteSourceType,
) { fetchString() }

// Create a StateFlow directly (backed by a LazyFlowSubject internally):
val flow: StateFlow<Container<String>> = subjectFactory.createFlow {
    emit(fetchData())
}

// Create a reloadable StateFlow (emitReloadFunction + emitBackgroundLoads enabled):
val flow: StateFlow<Container<String>> = subjectFactory.createReloadableFlow {
    emit(fetchData())
}
```

## Metadata

### ContainerMetadata

`ContainerMetadata` is an immutable bag attached to `Container.Success` and
`Container.Error`. Multiple metadata instances can be combined:

```kotlin
val meta = SourceTypeMetadata(RemoteSourceType) + ReloadFunctionMetadata { loadItems() }
val container = successContainer("data", meta)
```

When two metadata instances of the same type are combined, the second one
replaces the first:

```kotlin
val combined = SourceTypeMetadata(LocalSourceType) + SourceTypeMetadata(RemoteSourceType)
// result: only RemoteSourceType is kept
```

Access a specific metadata type:

```kotlin
val sourceMeta: SourceTypeMetadata? = container.metadata.get<SourceTypeMetadata>()
```

Or use the shorthand extension properties available on containers:

```kotlin
val source: SourceType  = container.sourceType
val bgLoadState: BackgroundLoadState = container.backgroundLoadState
val reloadFn = container.reloadFunction
```

### SourceType

`SourceType` communicates where the data came from. The built-in values are:

| Value | Meaning |
|-------|---------|
| `LocalSourceType` | Loaded from a local/on-device data source |
| `RemoteSourceType` | Fetched from a remote/network data source |
| `ImmediateSourceType` | Set directly, not via a loader |
| `FakeSourceType` | Provided by a test double or fake implementation |
| `UnknownSourceType` | Source is not known |

Emit with a source type inside the loader:

```kotlin
LazyFlowSubject.create {
    emit(localDataSource.get(), LocalSourceType)
    // isLastValue arg is optional, but it can improve performance a bit:
    emit(remoteDataSource.get(), RemoteSourceType, isLastValue = true)
}
```

Set a source type on an existing container:

```kotlin
val container = successContainer("data", SourceTypeMetadata(RemoteSourceType))

// or update metadata on an existing container:
val updated = container.update { sourceType = RemoteSourceType }
```

Read the source type:

```kotlin
val success: Container.Success<String> = ...
println(success.sourceType)  // RemoteSourceType
```

### ReloadFunctionMetadata

Attaching a reload function to a container lets UI components trigger a
reload without needing a direct reference to the subject / view-model, or any
other components:

```kotlin
// The listenReloadable() shorthand does this automatically:
fun listenProducts() = productsSubject.listenReloadable()

// Or attach manually on an existing container:
val container = successContainer("data") + ReloadFunctionMetadata { loadMyData() }

// Or override the reload function in a flow:
val flow = source.containerUpdate {
    val originalReload = reloadFunction
    reloadFunction = {
        println("Reloading...")
        originalReload(it) // call the original reload function if needed
    }
}
```

Calling the reload function from UI code:

```kotlin
val container: Container<String> = ...
container.fold(
    onError   = { ex -> Button(onClick = { container.reload() }) { Text("Retry") } },
    onSuccess = { value -> /* ... */ },
)
```

### BackgroundLoadStateMetadata

When a silent reload is in progress and `emitBackgroundLoads = true` is set,
emitted containers carry `backgroundLoadState = Loading` metadata value. UI can use this to
show an indicator (e.g. pull-to-refresh) while still displaying the stale data:

```kotlin
container.fold(
    onSuccess = { value ->
        if (backgroundLoadState == BackgroundLoadState.Loading) ShowRefreshIndicator()
        ShowContent(value)
    },
)
```

### LoadTrigger

Available inside the loader function via `loadTrigger: LoadTrigger`:

```kotlin
LazyFlowSubject.create {
    when (loadTrigger) {
        LoadTrigger.NewLoad      -> { /* first-ever load or newLoad() called */ }
        LoadTrigger.Reload       -> { /* explicit reload() call */ }
        LoadTrigger.CacheExpired -> { /* fresh load after cache timed out */ }
    }
}
```

### Custom Metadata

You can define your own metadata types by implementing `ContainerMetadata`:

```kotlin
data class TimestampMetadata(val timestamp: Long) : ContainerMetadata

// attach:
val container = successContainer("data", TimestampMetadata(System.currentTimeMillis()))

// read:
val ts: Long? = container.metadata.get<TimestampMetadata>()?.timestamp
```

Implement `ContainerMetadata.Hidden` to prevent the metadata from being seen
by downstream collectors (it is still passed through internally and visible from
the loader function):

```kotlin
data class TimestampMetadata(val timestamp: Long) : ContainerMetadata, ContainerMetadata.Hidden
```

## Flow Dependencies in Loader Functions

Starting from v2.0.0-beta13, loader functions can subscribe to external Kotlin
flows. When the subscribed flow emits a new value, the loader function is
automatically re-executed.

### dependsOnContainerFlow

Use `dependsOnContainerFlow` to depend on a `Flow<Container<T>>`. If the
dependent flow emits `Container.Error`, the current load is failed with the
same exception. If it emits `Container.Pending`, the load waits:

```kotlin
interface SessionProvider {
    fun getCurrentUserFlow(): Flow<Container<User>>
}

private val itemsSubject = LazyFlowSubject.create {
    // The loader re-runs whenever the current user changes:
    val currentUser: User = dependsOnContainerFlow("getCurrentUser") {
        sessionProvider.getCurrentUserFlow()
    }
    val items = remoteDataSource.getItems(currentUser)
    emit(items, RemoteSourceType)
}
```

### dependsOnFlow

For plain `Flow<T>` dependencies (not wrapped in `Container`):

```kotlin
val currentUser: User = dependsOnFlow("getUser") {
    sessionProvider.getUserFlow()
}
```

### Key Stability

Every `dependsOnFlow` / `dependsOnContainerFlow` call must be given a stable
key (or key + arguments) that uniquely identifies the flow instance. The keys
are used to cache the subscribed flows across re-executions of the loader:

```kotlin
// simple key:
val user: User = dependsOnContainerFlow("getCurrentUser") {
    sessionProvider.getCurrentUserFlow()
}

// key with arguments (important if the argument changes the flow):
val userId: String = sessionProvider.getCurrentUserId()
val user: User = dependsOnContainerFlow("getUserById", userId) {
    userRepository.getUserById(userId)
}
```

If you call `dependsOnFlow` or `dependsOnContainerFlow` with the same key
twice within one loader execution, the second call is ignored and returns the
cached result from the first call:

```kotlin
val a: String = dependsOnContainerFlow("key") { getFlow1() }
val b: String = dependsOnContainerFlow("key") { getFlow2() } // getFlow2 is ignored
// a == b, both refer to the results from the first call
```
