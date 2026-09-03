# Subjects and Caches Reference

`LazyFlowSubject<T>` and `LazyCache<Arg, T>` are the data-layer building blocks
of `com.elveum:container:3.6.0`. Both turn a suspending *loader function* into
an observable `Flow<Container<T>>` that:

- runs the loader lazily, only when someone actually collects;
- caches the loaded value and shares it between all collectors;
- can be reloaded, updated in place, and decorated with cross-cutting logic.

They differ in one respect only: a subject holds **one** value, a cache holds
**one value per argument**.

See [`container-type.md`](container-type.md) for `Container<T>`, its three
states, `fold`, the extraction functions, and the `ContainerFlow<T>` /
`ListContainerFlow<T>` aliases used throughout this file.

Packages:

| Symbol | Package |
|--------|---------|
| `LazyFlowSubject`, `ValueLoader`, `SimpleValueLoader`, `ContainerConfiguration`, and the subject extensions | `com.elveum.container.subject` |
| `LazyCache`, `CacheValueLoader`, `SimpleCacheValueLoader`, `LazyFlowSubjectFactory`, and the cache extensions | `com.elveum.container.cache` |
| `SubjectFactory`, `DefaultSubjectFactory`, `DEFAULT_CACHE_TIMEOUT_MILLIS`, `DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS` | `com.elveum.container.factory` |
| `LoaderDecorator`, `DecoratedFlowComposer` | `com.elveum.container.subject.transformation` |
| `LoadConfig`, `ReplaceErrorsOnReload`, `ContainerMetadata`, `SourceType`, `LoadTrigger`, `FlowComposer`, `Emitter` | `com.elveum.container` |

> Extension functions such as `listenReloadable`, `reloadAsync`,
> `updateIfSuccess`, `updateWith`, `newAsyncLoad`, `createSimple` are **top-level
> extensions**, not members. They must be imported explicitly - e.g.
> `import com.elveum.container.subject.listenReloadable` for the subject one and
> `import com.elveum.container.cache.listenReloadable` for the cache one. The two
> have the same name in different packages.

## 1. `LazyFlowSubject<T>`

### Creating

```kotlin
// declared inside `LazyFlowSubject.companion object`, so call it as LazyFlowSubject.create(...)
public fun <T> create(
    cacheTimeoutMillis: Long = DEFAULT_CACHE_TIMEOUT_MILLIS,              // 1000L
    reloadDependenciesPeriodMillis: Long = DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS, // 50L
    coroutineScopeFactory: CoroutineScopeFactory = CoroutineScopeFactory,
    transformation: ContainerTransformation<T> = EmptyContainerTransformation(),
    loaderDecorator: LoaderDecorator = LoaderDecorator,
    loadConfig: LoadConfig = LoadConfig.Normal,
    metadata: ContainerMetadata = EmptyMetadata,
    valueLoader: ValueLoader<T>? = null,
): LazyFlowSubject<T>
```

The loader runs with an `Emitter<T>` receiver, so call `emit(value)` inside it -
it can emit more than once (typical local-then-remote pattern):

```kotlin
private class ProfileRepository {

    private val subject = LazyFlowSubject.create<User> {
        emit(User(1, "local"), LocalSourceType)
        emit(User(1, "remote"), RemoteSourceType, isLastValue = true)
    }

    fun listenProfile(): ContainerFlow<User> = subject.listenReloadable()
}
```

`valueLoader` is nullable and defaults to `null`, so `LazyFlowSubject.create()`
with no loader is legal - it produces a subject that only ever holds values you
push into it with `updateWith`.

The loader is also assignable as a value, which matters when another component
builds it for you (paging, for example):

```kotlin
val explicitLoader: ValueLoader<User> = ValueLoader {
    emit(User(1, "explicit"), RemoteSourceType, isLastValue = true)
}
val subject = LazyFlowSubject.create(cacheTimeoutMillis = 5_000L, valueLoader = explicitLoader)
```

> **There is no `LazyFlowSubject.createSimple`.** (`LazyCache.createSimple`
> does exist - see section 4.) For a one-value subject either write
> `create { emit(loadIt()) }`, or use `SubjectFactory.createSimpleSubject`
> (section 5).

> **Writing a plain JVM unit test against a subject/cache built with the
> default `coroutineScopeFactory`?** It crashes on `Dispatchers.Main` -
> see the `CoroutineScopeFactory` trap in
> [references/testing.md](testing.md) section 5.

### `Emitter<T>` - the loader receiver

```kotlin
public interface Emitter<T> : FlowComposer {
    public val metadata: ContainerMetadata
    public val loadTrigger: LoadTrigger get() = metadata.loadTrigger

    public suspend fun emit(value: T, source: SourceType, isLastValue: Boolean = false)
    public suspend fun emit(value: T, metadata: ContainerMetadata = EmptyMetadata, isLastValue: Boolean = false)
}
```

- `emit(value)` - the common case.
- `emit(value, LocalSourceType)` / `emit(value, RemoteSourceType, isLastValue = true)` -
  tag where the value came from. `isLastValue = true` is an optional performance
  hint that no further emission follows.
- `loadTrigger` - *why* this execution happened (section 7).
- `metadata` - the metadata passed into the load call that triggered this run.
- Because `Emitter` extends `FlowComposer`, `dependsOnFlow` /
  `dependsOnContainerFlow` are available inside the loader (section 8).

### Members and extensions

| Call | Kind | Returns | Notes |
|------|------|---------|-------|
| `listen(configuration = ContainerConfiguration())` | member | `StateFlow<Container<T>>` | Collecting it starts the load |
| `listenReloadable(emitReloadFunction = true, emitBackgroundLoads = true)` | ext | `StateFlow<Container<T>>` | `listen` with both flags on |
| `spy(configuration = ContainerConfiguration())` | member | `StateFlow<Container<T>>` | Observes **without** triggering a load; emits `Pending` forever if nothing is collecting `listen()` |
| `currentValue(configuration = ContainerConfiguration())` | member | `Container<T>` | Snapshot of the current state |
| `activeCollectorsCount` | member | `Int` | Collectors currently collecting `listen()` |
| `hasActiveCollectors` | member | `Boolean` | `activeCollectorsCount > 0` |
| `reload(config: LoadConfig? = null, metadata: ContainerMetadata = EmptyMetadata)` | member | `Flow<T>` | Re-runs the *previous* loader |
| `reloadAsync(config = null, metadata = EmptyMetadata)` | ext | `Unit` | Fire-and-forget `reload` |
| `newLoad(config = null, metadata = EmptyMetadata, valueLoader)` | member | `Flow<T>` | Replaces the loader and runs it |
| `newAsyncLoad(config = null, metadata = EmptyMetadata, valueLoader)` | ext | `Unit` | Fire-and-forget `newLoad` |
| `newSimpleLoad(config = null, metadata = EmptyMetadata, valueLoader)` | ext, **suspend** | `T` | One-value loader; suspends until loaded; may throw |
| `newSimpleAsyncLoad(config = null, metadata = EmptyMetadata, valueLoader)` | ext | `Unit` | Fire-and-forget `newSimpleLoad` |
| `updateWith(container: Container<T>)` | member | `Unit` | Pushes a container directly; cancels the current load |
| `updateWith(configuration = ContainerConfiguration()) { old -> new }` | ext, inline | `Unit` | Read-modify-write loop over `compareAndSet` |
| `updateIfSuccess(metadata = EmptyMetadata) { oldValue -> newValue }` | ext, inline | `Unit` | Only applies when the current container is `Success` |
| `compareAndSet(configuration = ContainerConfiguration(), expected, updated)` | member | `Boolean` | Atomic CAS on the current container |
| `whenActive(spyMode = true) { ... }` | member | `LazyFlowSubject<T>` | Chainable; see below |

> **There is no `get()` on `LazyFlowSubject`.** The accessor is
> `currentValue(...)`. (`get(arg)` exists on `LazyCache` - section 4.)
> There is also no `reset()` on a subject; `reset()` is cache-only.

```kotlin
// listening
val plain: StateFlow<Container<User>> = subject.listen()
val reloadable: StateFlow<Container<User>> = subject.listenReloadable()
val configured: StateFlow<Container<User>> = subject.listen(
    configuration = ContainerConfiguration(emitBackgroundLoads = true, emitReloadFunction = true)
)
val spied: StateFlow<Container<User>> = subject.spy()   // does not start the load

// reading the snapshot
val now: Container<User> = subject.currentValue()

// reloading
subject.reloadAsync()                                  // fire-and-forget
subject.reloadAsync(LoadConfig.SilentLoading)          // keep the old value visible
val observed: Flow<User> = subject.reload()            // collect the load result

// pushing values without running the loader
subject.updateWith(successContainer(user))
subject.updateWith { old -> old }                      // read-modify-write
subject.updateIfSuccess { it.copy(name = "renamed") }  // Success-only shorthand
val changed: Boolean = subject.compareAndSet(expected = old, updated = new)

// replacing the loader
val flow: Flow<User> = subject.newLoad { emit(step1()); emit(step2()) }
subject.newAsyncLoad(config = LoadConfig.SilentLoading, metadata = SourceTypeMetadata(RemoteSourceType)) {
    emit(fetchRemote())
}
subject.newSimpleAsyncLoad { fetchOne() }
val value: User = subject.newSimpleLoad { fetchOne() }  // suspend
```

`reload()` re-runs the **previous** loader, so it still works after
`updateWith`. If the subject has never been given a loader, `reload()` returns
an empty flow and does nothing.

### `whenActive`

```kotlin
public fun whenActive(
    spyMode: Boolean = true,
    block: suspend ScopedLazyFlowSubject<T>.() -> Unit,
): LazyFlowSubject<T>

// public interface ScopedLazyFlowSubject<T> : LazyFlowSubject<T>, CoroutineScope
```

Registers a suspending block that is launched when the subject becomes active
(first collector of `listen()`) and cancelled when the subject goes inactive
(last collector leaves *and* the cache timeout expires). The receiver,
`ScopedLazyFlowSubject<T>`, is the subject itself plus a `CoroutineScope`, so
you can call any subject method inside the block and `launch` coroutines from
it. `whenActive` returns the subject, so it chains onto `create`:

```kotlin
private val subject = LazyFlowSubject.create<User> { emit(loadUser()) }
    .whenActive {
        // e.g. observe another flow and push updates into this subject
        listen().collect { container -> log(container) }
    }
```

`spyMode = true` (the default) makes a `listen()` call **inside the block**
behave like `spy()`, so the block itself does not keep the subject alive. Pass
`spyMode = false` if the block should count as a real collector.

## 2. Caching lifecycle

The whole point of a subject is that nothing happens until someone watches.

1. **Lazy start.** The loader is not executed by `create`. It runs when the
   first collector applies a terminal operator (`collect`) to the flow returned
   by `listen()` / `listenReloadable()`. `spy()` collectors do **not** count and
   never start a load - **a `spy()` flow with no `listen()` collector anywhere
   emits `Container.Pending` and stays there indefinitely.** `spy()` is a
   side-channel for code that wants to watch a subject somebody *else* is
   driving (logging, a `whenActive` block, a debug overlay); it is never a
   standalone read path.
2. **Sharing.** Only one load is active at a time. Additional collectors that
   arrive while a value is cached receive the cached value immediately without
   re-running the loader.
3. **Deferred work is queued, not discarded.** A call made while nobody is
   collecting is **not** lost - it is held and applied when the next collector
   arrives. What is deferred is only the *observable* effect:
   - `updateWith(container)` pushes the container even with zero collectors.
     The next collector receives that pushed value, and **no fresh load runs**
     for it. (Verified by a runnable test; do not assume the value is dropped.)
   - `newLoad` / `newAsyncLoad` / `newSimpleAsyncLoad` replace the loader
     immediately; the replacement persists and is the loader that actually runs
     for the next collector. The old loader never runs.
   - What genuinely stays silent is the **flow returned by** `newLoad` /
     `reload` / `newSimpleLoad`: it emits nothing until a collector subscribes
     to `listen()`, and it is cancelled when the last collector leaves. So
     `newSimpleLoad`, which suspends on that flow, will not return until a
     collector exists.
   - During the cache-timeout window (collectors gone, timeout not yet expired)
     all of these behave completely normally - the load machinery is still
     running.
4. **Timeout on the way out.** When the last collector stops, a timer starts -
   `cacheTimeoutMillis`, default `DEFAULT_CACHE_TIMEOUT_MILLIS` = `1000L` (1
   second). If a new collector arrives before it expires, the timer is
   cancelled and the cached value is handed straight to the new collector.
5. **Cache cleared.** If the timer expires with zero collectors, the load scope
   is cancelled, flow dependencies are shut down, and the cached value is
   dropped. The next collector triggers a fresh load, and that execution sees
   `loadTrigger == LoadTrigger.CacheExpired`.

Tune it per instance:

```kotlin
LazyFlowSubject.create(cacheTimeoutMillis = 60_000L) { emit(loadData()) }
LazyCache.create<Long, User>(cacheTimeoutMillis = 60_000L) { id -> emit(loadUser(id)) }
```

or globally via `DefaultSubjectFactory(cacheTimeoutMillis = ...)` (section 5).

For a cache, the timer is **per argument**: the entry for `id = 1` expires on
its own schedule, independently of `id = 2`.

## 3. `ContainerConfiguration` and `LoadConfig`

These two are often confused. `ContainerConfiguration` is about **what metadata
the emitted containers carry**; `LoadConfig` is about **how a load transitions
between states**.

### `ContainerConfiguration`

```kotlin
public data class ContainerConfiguration(
    val emitBackgroundLoads: Boolean = false,
    val emitReloadFunction: Boolean = false,
)
```

- `emitReloadFunction = true` - every emitted `Container.Completed` carries a
  `reloadFunction`, so UI can call `container.reload()` for a retry button
  without holding a reference to the subject.
- `emitBackgroundLoads = true` - while a *silent* load is running, emitted
  containers carry `backgroundLoadState = BackgroundLoadState.Loading` (and
  `BackgroundLoadState.Error` after a silent failure), which is how you drive a
  pull-to-refresh indicator over stale-but-visible content.

Both default to `false`, so a bare `listen()` emits containers with no reload
function and `BackgroundLoadState.Idle`. `listenReloadable()` is exactly
`listen(ContainerConfiguration(emitBackgroundLoads = true, emitReloadFunction = true))`
and is what you normally want from a repository.

The same configuration argument is accepted by `currentValue`, `spy`,
`compareAndSet`, and the cache's `get` / `listen`.

### `LoadConfig`

```kotlin
public data class LoadConfig internal constructor(
    val isSilentLoadingEnabled: Boolean,
    val isSilentErrorsEnabled: Boolean,
    val replaceErrorsOnReload: Boolean,
)
```

The constructor is `internal`; use the three presets:

| Preset | While loading | On failure |
|--------|---------------|------------|
| `LoadConfig.Normal` (default) | container goes back to `Container.Pending` | replaced by `Container.Error` |
| `LoadConfig.SilentLoading` | the current value stays visible, tagged `BackgroundLoadState.Loading` | replaced by `Container.Error` |
| `LoadConfig.SilentLoadingAndError` | the current value stays visible, tagged `BackgroundLoadState.Loading` | the current value stays visible, tagged `BackgroundLoadState.Error(e)` |

`ReplaceErrorsOnReload` is a `data object` flag attached with `+`:

```kotlin
val config = LoadConfig.SilentLoading + ReplaceErrorsOnReload
```

With it, a reload stays silent only while the current container is a *value*; a
current **error** is replaced by `Container.Pending` instead of being kept on
screen. Attaching it to a non-silent config (`LoadConfig.Normal`) is a no-op -
`plus` returns the receiver unchanged.

`LoadConfig` is accepted wherever a load can be started:
`create(loadConfig = ...)`, `reload(config)` / `reloadAsync(config)`,
`newLoad(config)` / `newAsyncLoad(config)` / `newSimpleLoad(config)` /
`newSimpleAsyncLoad(config)`, `LazyCache.reload(arg, config)`, and
`FlowComposer.Config(loadConfig)` (section 8).

> Note the parameter names: `create` takes **`loadConfig`**, while `reload` /
> `newLoad` and friends take **`config`**.

Passing `config = null` (the default on reload/newLoad) means "keep using the
config the subject is already working with".

### `reload(config)` applies to one load; `newLoad(config)` changes the default

`reload(config)` / `reloadAsync(config)` apply that config to **that load
only**. The subject's own configuration - the one it was created with, or the
one a later `newLoad` established - is untouched, so the next `reload()` with
no config behaves normally again:

```kotlin
subject.reloadAsync(LoadConfig.SilentLoading)  // silent, just this once
subject.reloadAsync()                          // back to the subject's config
```

`newLoad(config, loader)` is the opposite: it replaces the loader **and**
stores the config as the subject's new default, which every later
`config = null` load inherits - including the load rebuilt after a cache
expiry.

```kotlin
subject.newAsyncLoad(config = LoadConfig.SilentLoading, EmptyMetadata, loader)
subject.reloadAsync()                          // still silent - the default moved
```

Passing a config to `reload` is exactly equivalent to attaching
`LoadConfigOneShotMetadata` (section 7) yourself; the parameter is the
shorthand. On a `LazyCache` the same applies per argument, since each entry is
its own subject.

## 4. `LazyCache<Arg, T>`

`LazyCache` is a subject **keyed by an argument**. Internally it holds one
`LazyFlowSubject<T>` per distinct `Arg`, each with its own load, its own cached
value, and its own cache timeout.

**Reach for a cache instead of a subject when there is one entry per
identifier and different screens observe different entries independently** -
e.g. a user profile by user id, a product detail by product id, a comment
thread by post id. Use a plain subject when there is exactly one logical value
(the signed-in user's profile, the app-wide settings, a single list).

### Creating

```kotlin
public typealias CacheValueLoader<Arg, T> = suspend Emitter<T>.(Arg) -> Unit
public typealias SimpleCacheValueLoader<Arg, T> = suspend (Arg) -> T

// full form (companion member) - same options as LazyFlowSubject.create, plus the Arg parameter
public fun <Arg, T> create(
    cacheTimeoutMillis: Long = DEFAULT_CACHE_TIMEOUT_MILLIS,
    reloadDependenciesPeriodMillis: Long = DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS,
    coroutineScopeFactory: CoroutineScopeFactory = CoroutineScopeFactory,
    transformation: ContainerTransformation<T> = EmptyContainerTransformation(),
    loaderDecorator: LoaderDecorator = LoaderDecorator,
    loadConfig: LoadConfig = LoadConfig.Normal,
    metadata: ContainerMetadata = EmptyMetadata,
    valueLoader: CacheValueLoader<Arg, T>,
): LazyCache<Arg, T>

// one-value form (extension, import com.elveum.container.cache.createSimple)
public fun <Arg, T> LazyCache.Companion.createSimple(
    cacheTimeoutMillis: Long = DEFAULT_CACHE_TIMEOUT_MILLIS,
    valueLoader: SimpleCacheValueLoader<Arg, T>,
): LazyCache<Arg, T>

// per-argument subject construction (companion member)
public fun <Arg, T> createFromFactory(
    cacheTimeoutMillis: Long = DEFAULT_CACHE_TIMEOUT_MILLIS,
    coroutineScopeFactory: CoroutineScopeFactory = CoroutineScopeFactory,
    loaderDecorator: LoaderDecorator = LoaderDecorator,
    transformation: ContainerTransformation<T> = EmptyContainerTransformation(),
    reloadDependenciesPeriodMillis: Long = DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS,
    loadConfig: LoadConfig = LoadConfig.Normal,
    metadata: ContainerMetadata = EmptyMetadata,
    factory: LazyFlowSubjectFactory<Arg, T>,
): LazyCache<Arg, T>
```

> `createSimple` takes **only** `cacheTimeoutMillis` and the loader - it has no
> `loaderDecorator`, `loadConfig`, `metadata` or `transformation` parameter.
> Use `create { arg -> emit(...) }` when you need any of those.

```kotlin
// full loader: Emitter<T> receiver, Arg parameter
private val cache = LazyCache.create<Long, User> { id ->
    emit(User(id, "local-$id"), LocalSourceType)
    emit(User(id, "remote-$id"), RemoteSourceType, isLastValue = true)
}

// simple loader: no Emitter, just return the value
private val simpleCache: LazyCache<Long, User> =
    LazyCache.createSimple { id -> loadUser(id) }

// per-argument subject, built through the creation scope
private val factoryCache: LazyCache<Long, User> = LazyCache.createFromFactory { arg: Long ->
    newInstance(cacheTimeoutMillis = 5_000L, loadConfig = LoadConfig.SilentLoading) {
        emit(loadUser(arg))
    }
}
```

`LazyFlowSubjectFactory<Arg, T>` is a `fun interface` whose single method runs
with a `LazyFlowSubjectCreationScope<T>` receiver, exposing:

```kotlin
public fun newInstance(
    cacheTimeoutMillis: Long? = null,
    reloadDependenciesPeriodMillis: Long? = null,
    coroutineScopeFactory: CoroutineScopeFactory? = null,
    transformation: ContainerTransformation<T>? = null,
    loaderDecorator: LoaderDecorator? = null,
    loadConfig: LoadConfig? = null,
    metadata: ContainerMetadata? = null,
    valueLoader: ValueLoader<T>,
): LazyFlowSubject<T>
```

Every parameter is nullable and `null` means "inherit from the cache", so
`newInstance { emit(...) }` gives you a subject configured exactly like the
cache that owns it.

### Members and extensions

| Call | Kind | Returns | Notes |
|------|------|---------|-------|
| `listen(arg, configuration = ContainerConfiguration())` | member | `StateFlow<Container<T>>` | Collecting starts the load for `arg` |
| `listenReloadable(arg, emitReloadFunction = true, emitBackgroundLoads = true)` | ext | `StateFlow<Container<T>>` | `listen` with both flags on |
| `get(arg, configuration = ContainerConfiguration())` | member | `Container<T>` | Snapshot for `arg` |
| `getActiveCollectorsCount(arg)` | member | `Int` | Collectors on this `arg` |
| `hasActiveCollectors(arg)` | member | `Boolean` | `getActiveCollectorsCount(arg) > 0` |
| `spyOnArgs()` | member | `StateFlow<Set<Arg>>` | The set of args currently held in the cache |
| `reload(arg, config = null, metadata = EmptyMetadata)` | member | `Flow<T>` | Re-runs the loader for `arg` |
| `reloadAsync(arg, config = null, metadata = EmptyMetadata)` | ext | `Unit` | Fire-and-forget `reload` |
| `updateWith(arg, container: Container<T>)` | member | `Unit` | Pushes a container for `arg` |
| `updateWith(arg) { old -> new }` | ext, inline | `Unit` | Read-modify-write; skipped when unchanged |
| `updateIfSuccess(arg, sourceType: SourceType? = null) { oldValue -> newValue }` | ext, inline | `Unit` | Only applies when the current container for `arg` is `Success` |
| `reset()` | member | `Unit` | Drops every cache entry that has **no** active collectors |
| `whenActive { ... }` | member | `LazyCache<Arg, T>` | Chainable; see below |

```kotlin
private class UserRepository {

    private val cache = LazyCache.create<Long, User> { id -> emit(loadUser(id)) }

    fun listenUser(id: Long): ContainerFlow<User> = cache.listenReloadable(id)

    fun currentUser(id: Long): Container<User> = cache.get(id)

    fun reload(id: Long) = cache.reloadAsync(id)

    fun reloadSilently(id: Long) = cache.reloadAsync(id, LoadConfig.SilentLoading)

    fun rename(id: Long, newName: String) = cache.updateIfSuccess(id) { it.copy(name = newName) }

    fun renameFromRemote(id: Long, name: String) =
        cache.updateIfSuccess(id, RemoteSourceType) { it.copy(name = name) }

    fun push(id: Long, user: User) = cache.updateWith(id, successContainer(user))

    fun observedIds(): StateFlow<Set<Long>> = cache.spyOnArgs()

    fun dropUnobserved() = cache.reset()
}
```

### Differences from `LazyFlowSubject` - read this before writing cache code

These are the places where the cache API deliberately does **not** mirror the
subject API. Getting them wrong is the most common failure mode:

| Subject | Cache | Note |
|---------|-------|------|
| `currentValue(configuration)` | `get(arg, configuration)` | The cache has **no** `currentValue`; the subject has **no** `get()` |
| `spy(configuration)` | *(none)* | There is no per-argument `spy`. `spyOnArgs()` observes the *set of keys*, not a value |
| `activeCollectorsCount` (property) | `getActiveCollectorsCount(arg)` (function) | |
| `hasActiveCollectors` (property) | `hasActiveCollectors(arg)` (function) | |
| `compareAndSet(...)` | *(none)* | No CAS on the cache |
| `updateIfSuccess(metadata = ...) { }` | `updateIfSuccess(arg, sourceType = ...) { }` | Second parameter differs: `ContainerMetadata` vs `SourceType?` |
| `newLoad` / `newAsyncLoad` / `newSimpleLoad` / `newSimpleAsyncLoad` | *(none)* | You cannot replace a cache's loader; only `reload` |
| `whenActive(spyMode = true) { }` | `whenActive { }` | The cache version has **no** `spyMode` parameter |
| *(none)* | `reset()` | Cache-only; takes no arguments |

### Cache behaviour details

- **Entries are created by collection, not by construction.** A cache entry for
  `arg` comes into existence when someone starts collecting `listen(arg)`.
  Before that (and after the entry expires):
  - `get(arg)` returns `Container.Pending`,
  - `reload(arg)` returns an empty flow and does nothing,
  - `updateWith(arg, ...)` is silently ignored - the next collector runs a
    fresh load instead.

  This is the one place where the cache genuinely differs from a subject:
  `LazyFlowSubject.updateWith` with no collectors *does* survive and is handed
  to the next collector (section 2, item 3), because the subject always exists.
  A cache entry does not exist until someone collects it, so there is nothing
  to push into.
- **`spyOnArgs()`** emits the set of args that currently have a live cache
  entry. Entries linger for `cacheTimeoutMillis` after their last collector
  leaves, so a key can still appear there briefly with zero collectors.
- **`reset()`** removes exactly those entries whose collector count is `0`
  (i.e. it drops unobserved cached values without disturbing anything on
  screen). Observed entries are untouched.
- **`whenActive { }`** is registered once for the *whole cache*, not per
  argument. The block starts when the cache's total collector count across all
  args goes from 0 to 1, and is cancelled when it drops back to 0. Its
  receiver is `ScopedLazyCache<Arg, T>` - the cache itself plus a
  `CoroutineScope`:

  ```kotlin
  public fun whenActive(block: suspend ScopedLazyCache<Arg, T>.() -> Unit): LazyCache<Arg, T>

  // public interface ScopedLazyCache<Arg, T> : LazyCache<Arg, T>, CoroutineScope
  ```

```kotlin
private val cache = LazyCache.create<Long, User> { id -> emit(loadUser(id)) }
    .whenActive {
        spyOnArgs().collect { args -> prefetch(args) }
    }
```

## 5. `SubjectFactory` and `DefaultSubjectFactory`

Prefer injecting a `SubjectFactory` over calling `LazyFlowSubject.create` /
`LazyCache.create` directly: it centralises the cache timeout and the
`LoaderDecorator`, and it lets tests swap in a fake.

```kotlin
public interface SubjectFactory {

    public fun <T> createSubject(
        cacheTimeoutMillis: Long? = null,
        reloadDependenciesPeriodMillis: Long? = null,
        coroutineScopeFactory: CoroutineScopeFactory? = null,
        transformation: ContainerTransformation<T>? = null,
        loadConfig: LoadConfig = LoadConfig.Normal,
        metadata: ContainerMetadata = EmptyMetadata,
        valueLoader: ValueLoader<T>,
    ): LazyFlowSubject<T>

    public fun <Arg, T> createCache(
        cacheTimeoutMillis: Long? = null,
        reloadDependenciesPeriodMillis: Long? = null,
        coroutineScopeFactory: CoroutineScopeFactory? = null,
        transformation: ContainerTransformation<T>? = null,
        loadConfig: LoadConfig = LoadConfig.Normal,
        metadata: ContainerMetadata = EmptyMetadata,
        valueLoader: CacheValueLoader<Arg, T>,
    ): LazyCache<Arg, T>

    public fun <Arg, T> createCacheFromFactory(
        cacheTimeoutMillis: Long? = null,
        coroutineScopeFactory: CoroutineScopeFactory? = null,
        factory: LazyFlowSubjectFactory<Arg, T>,
    ): LazyCache<Arg, T>

    public companion object : SubjectFactory { /* delegates to the current global instance */ }
}
```

The `Long?` / nullable parameters mean "inherit the factory's own value".

> `createSubject` / `createCache` have **no `loaderDecorator` parameter**. The
> decorator is configured once, on the factory instance.

### `DefaultSubjectFactory`

```kotlin
public open class DefaultSubjectFactory(
    cacheTimeoutMillis: Long = DEFAULT_CACHE_TIMEOUT_MILLIS,                       // 1000L
    reloadDependenciesPeriodMillis: Long = DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS, // 50L
    coroutineScopeFactory: CoroutineScopeFactory = CoroutineScopeFactory,
    // (a transformation-factory parameter also exists; leave it at its default)
    loaderDecorator: LoaderDecorator = LoaderDecorator,
) : SubjectFactory
```

```kotlin
val factory: SubjectFactory = DefaultSubjectFactory(
    cacheTimeoutMillis = 30_000L,
    loaderDecorator = sessionDecorator,
)
```

`DEFAULT_CACHE_TIMEOUT_MILLIS` is `1000L`;
`DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS` is `50L`. Both are public
top-level constants in `com.elveum.container.factory`.

### Using it in a repository

```kotlin
class ProductRepository(
    private val subjectFactory: SubjectFactory = SubjectFactory,   // companion = global default
) {
    private val subject = subjectFactory.createSubject { emit(loadProducts()) }

    fun listen(): ContainerFlow<List<Product>> = subject.listenReloadable()
}
```

The `SubjectFactory` companion object implements `SubjectFactory` itself and
delegates to a swappable global instance:

```kotlin
SubjectFactory.setFactory(FakeSubjectFactory())  // e.g. in a test @Before
SubjectFactory.resetFactory()                    // restores DefaultSubjectFactory()
```

### Convenience extensions

All in `com.elveum.container.factory`:

```kotlin
// one-value subject - the closest thing to a "createSimple" for subjects
val subject: LazyFlowSubject<String> =
    subjectFactory.createSimpleSubject(sourceType = RemoteSourceType) { fetchString() }

// one-value cache
val cache: LazyCache<Long, User> =
    subjectFactory.createSimpleCache(sourceType = RemoteSourceType) { id -> fetchUser(id) }

// flows, without holding the subject yourself
val flow: Flow<Container<String>> = subjectFactory.createFlow { emit(fetchData()) }
val simpleFlow: Flow<Container<String>> =
    subjectFactory.createSimpleFlow(sourceType = RemoteSourceType) { fetchString() }
val reloadableFlow: Flow<Container<String>> = subjectFactory.createReloadableFlow { emit(fetchData()) }
```

`sourceType` defaults to `UnknownSourceType` in all three `createSimple*`
functions, and the value is emitted with `isLastValue = true`.

> `createFlow`, `createSimpleFlow` and `createReloadableFlow` return
> **`Flow<Container<T>>`**, not `StateFlow`. Declare the variable as `Flow`.

## 6. `LoaderDecorator`

A `LoaderDecorator` wraps **every** loader function of a subject or a cache, so
cross-cutting logic (session checks, logging, error mapping) lives in one place.

```kotlin
public fun interface LoaderDecorator {
    public suspend fun DecoratedFlowComposer.decorate(originLoader: suspend () -> Unit)

    public companion object : LoaderDecorator  // the identity decorator - just calls originLoader()
}
```

Written as a lambda, the parameter is `originLoader` and `this` is a
`DecoratedFlowComposer`:

```kotlin
val sessionDecorator = LoaderDecorator { originLoader ->
    val token: String = dependsOnFlow("session-token") { sessionManager.tokenFlow }
    if (token.isBlank()) throw NoSessionException()
    originLoader()
}
```

Two rules:

- The implementation **must** call `originLoader()`, unless it throws or
  terminates the load explicitly. If it does none of those, nothing is emitted
  and the load fails with an `IllegalStateException`.
- The decorator's dependency keys share a namespace with the decorated loader's
  keys, so pick keys that cannot clash (section 8).

### Installing it

```kotlin
// a single subject
LazyFlowSubject.create(loaderDecorator = sessionDecorator) { emit(loadData()) }

// a cache - the decorator applies to the subject of every argument
LazyCache.create<Long, User>(loaderDecorator = sessionDecorator) { id -> emit(loadUser(id)) }

// every subject and cache produced by the factory
DefaultSubjectFactory(loaderDecorator = sessionDecorator)
```

The last form is the usual one: bind a single `DefaultSubjectFactory` in DI and
every repository built through it inherits the decorator.

### `DecoratedFlowComposer` - terminating a load

`DecoratedFlowComposer` extends `FlowComposer` (so `dependsOnFlow` /
`dependsOnContainerFlow` work inside a decorator) and adds two terminators:

```kotlin
public interface DecoratedFlowComposer : FlowComposer {
    public fun completeWithFailure(exception: Exception): Nothing
    public fun completeWithCacheCleanUp(): Nothing
}
```

- `completeWithFailure(e)` - finish the load with an error container. The
  exception reaches collectors **regardless of the silent-error policy**, unlike
  a plain `throw`, which under `LoadConfig.SilentLoadingAndError` would leave
  the stale value on screen.
- `completeWithCacheCleanUp()` - finish the load with a pending container,
  dropping any cached value. Use it when stale data must not remain visible
  (sign-out).

Both return `Nothing`: they unwind the decorator body immediately, so
`originLoader()` is not executed if it has not run yet. Calling them after
`originLoader()` discards whatever the loader emitted.

```kotlin
val sessionDecorator = LoaderDecorator { originLoader ->
    val session = dependsOnFlow("session") { sessionManager.sessionFlow }
    when (session) {
        is Session.SignedOut -> completeWithCacheCleanUp()          // wipe cache, show loading
        is Session.Expired   -> completeWithFailure(SessionExpiredException())  // always visible
        is Session.Active    -> originLoader()
    }
}
```

Because the decorator can declare flow dependencies, a new session token
emitted by `sessionManager.sessionFlow` automatically re-runs every decorated
loader.

## 7. Metadata

### `ContainerMetadata`

`ContainerMetadata` is an immutable bag attached to `Container.Success` and
`Container.Error` (`container.metadata`). Combine instances with `+`; when two
instances of the same type are combined, **the later one wins**:

```kotlin
val meta: ContainerMetadata =
    SourceTypeMetadata(RemoteSourceType) + ReloadFunctionMetadata { _, _ -> subject.reloadAsync() }

val combined = SourceTypeMetadata(LocalSourceType) + SourceTypeMetadata(RemoteSourceType)
// -> only RemoteSourceType survives
```

`EmptyMetadata` is the neutral `data object`.

Read a specific type with the reified `get()` extension
(`import com.elveum.container.get`):

```kotlin
val sourceMeta: SourceTypeMetadata? = container.metadata.get<SourceTypeMetadata>()
val ts: Long? = container.metadata.get<TimestampMetadata>()?.timestamp
```

Built-in metadata types (all in `com.elveum.container`):

| Type | Carries |
|------|---------|
| `SourceTypeMetadata(sourceType: SourceType)` | Where the value came from |
| `BackgroundLoadMetadata(backgroundLoadState: BackgroundLoadState)` | Silent-load progress |
| `ReloadFunctionMetadata(reloadFunction: ReloadFunction)` | How to reload this container |
| `LoadTriggerMetadata(loadTrigger: LoadTrigger)` | Why the loader ran (`Hidden`) |
| `LoadConfigOneShotMetadata(loadConfig: LoadConfig)` | A `LoadConfig` for one single load (`OneShot`, `Hidden`) |
| `IsReloadDependenciesMetadata(isReloadDependencies: Boolean)` | Internal dependency-reload flag (`Hidden`) |

> The background-load metadata class is named **`BackgroundLoadMetadata`**.
> There is no `BackgroundLoadStateMetadata` - that name appears in some prose
> but does not exist as a declaration.

### Convenient accessors

`Container` itself exposes `sourceType` and `backgroundLoadState` as members:

```kotlin
val source: SourceType = container.sourceType
val bg: BackgroundLoadState = container.backgroundLoadState
```

The reload function is **not** a `Container` member. It is an extension on
`ContainerMetadata`, so go through `.metadata`:

```kotlin
val reloadFn: ReloadFunction = container.metadata.reloadFunction   // NOT container.reloadFunction
```

`sourceType`, `backgroundLoadState`, `reloadFunction`, `loadTrigger` and
`isReloadDependencies` all exist as extension properties on `ContainerMetadata`
too, each with a safe fallback (`UnknownSourceType`, `BackgroundLoadState.Idle`,
`EmptyReloadFunction`, `LoadTrigger.NewLoad`, `false`).

Inside `fold` / `map` / `transform`, the `ContainerMapperScope` receiver
exposes `metadata`, `sourceType`, `backgroundLoadState`, `reloadFunction` and
`reload(...)` directly:

```kotlin
container.fold(
    onPending = { "loading" },
    onError = { e -> e.message.orEmpty() },
    onSuccess = { value ->
        if (backgroundLoadState == BackgroundLoadState.Loading) "refreshing ${value.name}"
        else value.name
    },
)
```

### `SourceType`

`SourceType` is a marker interface with five built-in `data object` values:

| Value | Meaning |
|-------|---------|
| `LocalSourceType` | Loaded from a local/on-device source |
| `RemoteSourceType` | Fetched from the network |
| `ImmediateSourceType` | Set directly, not via a loader |
| `FakeSourceType` | Provided by a test double |
| `UnknownSourceType` | Not known (the default) |

Emit with a source type from inside a loader:

```kotlin
LazyFlowSubject.create<List<Article>> {
    emit(localSource.fetch(), LocalSourceType)
    emit(remoteSource.fetch(), RemoteSourceType, isLastValue = true)
}
```

You can define your own by implementing `SourceType`.

### `ReloadFunctionMetadata`

```kotlin
public typealias ReloadFunction = (config: LoadConfig?, metadata: ContainerMetadata) -> Unit
```

Note the **two** parameters - a lambda literal must accept both:

```kotlin
val meta = ReloadFunctionMetadata { config, metadata -> subject.reloadAsync(config, metadata) }
val ignoring = ReloadFunctionMetadata { _, _ -> subject.reloadAsync() }
```

`EmptyReloadFunction` is the no-op default returned when no reload function is
attached.

The normal way to get a reload function into a container is
`listenReloadable()` (or `emitReloadFunction = true`). UI then calls the
`Container` member:

```kotlin
public abstract fun reload(config: LoadConfig? = null, metadata: ContainerMetadata = EmptyMetadata)
```

```kotlin
container.reload()
container.reload(LoadConfig.SilentLoading)
container.reload(LoadConfig.SilentLoading, PushRefreshMetadata)
```

Attaching one manually:

```kotlin
val withReload: Container<User> =
    successContainer(user) + ReloadFunctionMetadata { _, _ -> subject.reloadAsync() }
```

### `BackgroundLoadState`

```kotlin
public sealed class BackgroundLoadState {
    public data object Idle : BackgroundLoadState()
    public data object Loading : BackgroundLoadState()
    public data class Error(val exception: Exception) : BackgroundLoadState()
}
```

Populated only when `emitBackgroundLoads = true` and the load is silent. Drives
"content is visible but stale" UI:

```kotlin
container.fold(
    onPending = { LoadingScreen() },
    onError = { e -> ErrorScreen(e) },
    onSuccess = { value ->
        if (backgroundLoadState == BackgroundLoadState.Loading) RefreshIndicator()
        Content(value)
    },
)
```

There is also `container.isDataLoading()`, which is
`isPending() || backgroundLoadState == BackgroundLoadState.Loading`.

### `LoadTrigger`

An enum, read inside the loader via the `Emitter` receiver:

| Value | Meaning |
|-------|---------|
| `LoadTrigger.NewLoad` | Loader assigned by `create { }` or `newLoad` / `newAsyncLoad` |
| `LoadTrigger.Reload` | Re-triggered by `reload()` / `reloadAsync()` (including a dependency change) |
| `LoadTrigger.CacheExpired` | The cache timed out; a new collector triggered a fresh load |

```kotlin
private val subject = LazyFlowSubject.create<List<Product>> {
    if (loadTrigger != LoadTrigger.Reload) {
        val local = localSource.get()
        if (local != null) emit(local, LocalSourceType)
    }
    emit(remoteSource.get(), RemoteSourceType, isLastValue = true)
}
```

### Custom metadata

Implement `ContainerMetadata`:

```kotlin
data class TimestampMetadata(val timestamp: Long) : ContainerMetadata

val container = successContainer(value, TimestampMetadata(System.currentTimeMillis()))
val ts: Long? = container.metadata.get<TimestampMetadata>()?.timestamp
```

Two marker interfaces modify how it behaves:

- **`ContainerMetadata.Hidden`** - the value is not visible to downstream
  collectors, but it is still threaded through internally and readable from
  inside the loader function (`Emitter.metadata`). Use it for signals the data
  layer needs but the UI must not see.
- **`ContainerMetadata.OneShot`** - the value belongs to the single load request
  it was attached to. It is emitted with that load's container and survives
  re-emission from the in-memory cache, but is **not** carried into any *new*
  load (a later reload, query change, dependency update, or post-expiry reload
  produces containers without it). Override `isOneShot` to return `false` to
  disable the behaviour per instance.

```kotlin
data object PushRefreshMetadata : ContainerMetadata, ContainerMetadata.OneShot

subject.reloadAsync(metadata = PushRefreshMetadata)  // this container carries it...
subject.reloadAsync()                                // ...the next one does not
```

Metadata can be passed to `create(metadata = ...)`, `reload` / `reloadAsync`,
`newLoad` / `newAsyncLoad` / `newSimpleLoad` / `newSimpleAsyncLoad`,
`LazyCache.reload(arg, config, metadata)`, and `Container.reload(config, metadata)`.

`LoadConfigOneShotMetadata` is the built-in `OneShot` type carrying a
`LoadConfig`: it makes exactly one load use that config, letting a code path
that can pass metadata but no explicit config override the config once.

```kotlin
subject.reloadAsync(metadata = LoadConfigOneShotMetadata(LoadConfig.SilentLoading))  // silent
subject.reloadAsync()                                                                // back to normal
```

## 8. Flow dependencies

Loader functions (and decorators) can subscribe to external flows. When a
subscribed flow emits a new value, the loader function is **re-executed
automatically**. The API is `FlowComposer`, which `Emitter` and
`DecoratedFlowComposer` both extend:

```kotlin
public interface FlowComposer {
    public suspend fun <R> dependsOnFlow(key: Any, vararg keys: Any, flow: () -> Flow<R>): R
    public suspend fun <R> dependsOnContainerFlow(key: Any, vararg keys: Any, flow: () -> Flow<Container<R>>): R

    public data class Config(
        val loadConfig: LoadConfig? = null,
        val reloadDependencies: Boolean = false,
    )
}
```

Both return the *unwrapped* current value of the dependency, so the loader body
reads like straight-line code.

### `dependsOnContainerFlow`

For a `Flow<Container<T>>` dependency. If it emits `Container.Error`, the
current load fails with that same exception. If it emits `Container.Pending`,
the load waits.

```kotlin
private val itemsSubject = LazyFlowSubject.create<List<Item>> {
    val currentUser: User = dependsOnContainerFlow("getCurrentUser") {
        sessionProvider.getCurrentUserFlow()
    }
    emit(remoteSource.getItems(currentUser), RemoteSourceType)
}
```

### `dependsOnFlow`

For a plain `Flow<T>` dependency:

```kotlin
val filter: StarFilter = dependsOnFlow("filter") { filterFlow }
```

Both work identically inside a `LazyCache` loader and inside a
`LoaderDecorator`.

### Key stability

Every call needs a stable key (or key + arguments) uniquely identifying the flow
instance; keys are used to cache subscribed flows across re-executions of the
loader.

```kotlin
// simple key
val user: User = dependsOnContainerFlow("getCurrentUser") { sessionProvider.getCurrentUserFlow() }

// key + argument - required when the argument selects a different flow
val byId: User = dependsOnContainerFlow("getUserById", userId) { userRepository.getUserById(userId) }
```

Calling with the **same key twice within one loader execution** ignores the
second lambda and returns the first call's cached result:

```kotlin
val a: String = dependsOnContainerFlow("key") { getFlow1() }
val b: String = dependsOnContainerFlow("key") { getFlow2() }  // getFlow2() is never called; b == a
```

This is also why a decorator must choose keys that cannot collide with the keys
used by the loaders it wraps - they share one namespace.

### Reload configuration

A dependency-triggered reload behaves like an ordinary reload: it uses the load
config the subject is currently working with, so by default the container goes
back to `Container.Pending` while the loader re-runs.

Pass a `FlowComposer.Config` as one of the keys to configure the reloads caused
by that one dependency:

```kotlin
private val starsSubject = LazyFlowSubject.create<List<Star>> {
    val config = FlowComposer.Config(LoadConfig.SilentLoading)
    val filter: StarFilter = dependsOnFlow("filter", config) { filterFlow }
    emit(starsDataSource.fetchStars(filter))
}
```

| Property | Default | Meaning |
|----------|---------|---------|
| `loadConfig` | `null` | Config applied to reloads triggered by this dependency. `null` keeps the subject's current config |
| `reloadDependencies` | `false` | When `true`, every flow dependency of the loader is asked to reload itself (via the `reloadFunction` on its last container) before the loader re-runs |

The config is part of the dependency key, so keep it a stable value (a `val` or
a constant), exactly like the other keys.

Explicit `reload()` / `reloadAsync()` calls always reload the dependencies,
regardless of this setting.

`reloadDependenciesPeriodMillis` (default
`DEFAULT_RELOAD_DEPENDENCIES_PERIOD_MILLIS` = `50L`) controls how often
dependencies are polled for reload triggers; it is a constructor parameter of
`create`, `LazyCache.create`, `SubjectFactory.createSubject` / `createCache`,
and `DefaultSubjectFactory`.
