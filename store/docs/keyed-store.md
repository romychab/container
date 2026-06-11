# Keyed Store

`KeyedStore<Key, T>` manages multiple values, one per key, like a map of
[simple stores](simple-store.md). Each key-value entry has its own
auto-managed lifecycle determined by its observers. Typical data managed
by a keyed store: product details, friend profiles, movie details - any
entity fetched by a unique identifier.

## Table of Contents

- [Creating a Keyed Store](#creating-a-keyed-store)
- [Per-Key Cache Lifecycle](#per-key-cache-lifecycle)
- [Observing and Reloading](#observing-and-reloading)
- [Local Storage](#local-storage)
- [Updating Cached Data](#updating-cached-data)
- [Combining a List with Keyed Details](#combining-a-list-with-keyed-details)
- [Synchronizing Stores](#synchronizing-stores)
- [Sharing a Store Between Screens](#sharing-a-store-between-screens)
- [API Summary](#api-summary)

## Creating a Keyed Store

The minimal setup requires only an `onFetch` function that receives the
key:

```kotlin
class CatDetailsRepository(
    private val catsDataSource: CatsDataSource,
) {

    private val store = StoreFactory.keyedStoreBuilder<Long, CatDetails>()
        .build(onFetch = catsDataSource::fetchCatDetails)

    fun getCat(id: Long): Flow<StoreResult<CatDetails>> = store.observe(id)
}
```

As with all stores, the builder supports `setInMemoryCacheTimeout` and
`setCoroutineContext`:

```kotlin
private val store = StoreFactory.keyedStoreBuilder<Long, ProductDetails>()
    .setInMemoryCacheTimeout(10.seconds)
    .setCoroutineContext(Dispatchers.IO)
    .build(onFetch = productsDataSource::fetchProductById)
```

## Per-Key Cache Lifecycle

Unlike a traditional map, entries are not kept forever:

- An entry is created and loaded when the first observer subscribes to
  `observe(key)` for that key.
- All observers of the same key share one cached value.
- When the last observer of a key unsubscribes, the entry is kept for the
  configured in-memory cache timeout (**5 seconds** by default) and then
  removed from the cache.

This means each screen observing `observe(productId)` gets a shared,
up-to-date value while it is open, and the memory is reclaimed
automatically after the screens are closed.

## Observing and Reloading

All key-less operations of a simple store have keyed counterparts:

```kotlin
// observe a single entity
fun getProduct(id: Long): Flow<StoreResult<ProductDetails>> {
    return store.observe(id)
}

// force a reload of a single entity
suspend fun refreshProduct(id: Long) {
    store.invalidate(id, LoadRequest.Default)
}

fun refreshProductAsync(id: Long) {
    store.invalidateAsync(id, LoadRequest.Silent)
}
```

`observe` and `invalidate` accept a [LoadRequest](load-requests.md)
controlling how the data is loaded (fresh/offline mode, keeping content
while reloading).

## Local Storage

Keyed stores support the same two local storage modes as simple stores;
each callback additionally receives the key:

```kotlin
// suspending storage (e.g. DAO with suspend functions)
private val store = StoreFactory.keyedStoreBuilder<BookId, Book>()
    .addSuspendingLocalStorage()
    .build(
        onFetch = { bookId -> api.fetchBook(bookId) },
        onSaveToStorage = { bookId, book -> dao.save(bookId, book) },
        onLoadFromStorage = { bookId -> dao.load(bookId) }, // returns T?
    )

// reactive storage (e.g. Room Flow queries)
private val store = StoreFactory.keyedStoreBuilder<BookId, Book>()
    .addReactiveLocalStorage()
    .build(
        onFetch = { bookId -> api.fetchBook(bookId) },
        onSaveToStorage = { bookId, book -> dao.save(bookId, book) },
        onObserveStorage = { bookId -> dao.observe(bookId) }, // returns Flow<T?>
    )
```

With reactive storage, any change of the keyed record in the local storage
is automatically delivered to the observers of that key.

Contract-based `build` overloads are available too: `KeyedContract`,
`KeyedSuspendingContract`, `KeyedReactiveContract`.

## Updating Cached Data

`optimisticUpdate(key)` works like the simple-store version, scoped to one
key. Emitted values are auto-reverted if the block throws:

```kotlin
suspend fun updateCatName(id: Long, name: String) {
    store.optimisticUpdate(id) { oldDetails ->
        emit(oldDetails.copy(cat = oldDetails.cat.copy(name = name)))
        catsDataSource.updateCatName(id, name) // reverts the cache on failure
    }
}
```

The plain `update(key)` extension applies a transformation without
optimistic semantics:

```kotlin
suspend fun markAsRead(bookId: BookId) {
    dao.markAsRead(bookId)
    store.update(bookId) { it.copy(isRead = true) }
}
```

## Combining a List with Keyed Details

A common pattern is a list loaded by a [simple store](simple-store.md)
where every item needs extra data loaded per key. The
`storeListFlatMapLatest` extension observes the keyed store for every item
of the list and merges the results:

```kotlin
class BasicItemsRepository(
    private val dataSource: BasicItemsDataSource,
) {

    private val listStore = StoreFactory.simpleStoreBuilder<List<Item>>()
        .build(onFetch = dataSource::fetchItems)

    private val descriptionStore = StoreFactory.keyedStoreBuilder<Long, String>()
        .build(onFetch = dataSource::fetchDescription)

    fun getItems(): Flow<StoreResult<List<ListItem>>> {
        return listStore
            .observe()
            .storeListFlatMapLatest(
                observer = { item -> descriptionStore.observe(item.id) },
                mapper = { item, descriptionResult ->
                    ListItem(item, descriptionResult.getOrNull())
                },
            )
    }
}
```

The list is emitted as soon as it is loaded; descriptions arrive
individually (`getOrNull()` returns `null` while a description is still
loading) and each arrival re-emits the merged list. See
[Store Results](store-results.md#transformations) for the full set of
transformation extensions.

## Synchronizing Stores

In master-detail flows, an edit on the details screen should be reflected
in the master list. Combine `whenActive` with events to synchronize two
independent stores:

```kotlin
// Details: keyed store, publishes an event after each successful update
@Singleton
class CatDetailsRepository(
    private val catsDataSource: CatsDataSource,
) : CatEvents {

    private val store = StoreFactory.keyedStoreBuilder<Long, CatDetails>()
        .build(onFetch = catsDataSource::fetchCatDetails)

    private val catEvents = MutableSharedFlow<CatUpdatedEvent>()

    override fun observeCatEvents(): Flow<CatUpdatedEvent> = catEvents

    suspend fun updateCatName(id: Long, name: String) {
        store.optimisticUpdate(id) { old ->
            val updated = old.copy(cat = old.cat.copy(name = name))
            emit(updated)
            catsDataSource.updateCatName(id, name)
            catEvents.emit(CatUpdatedEvent(updated.cat))
        }
    }
}

// Master list: applies events to its own cache while it is active
@Singleton
class CatsRepository(
    private val catsDataSource: CatsDataSource,
    private val catEvents: CatEvents,
) {

    private val store = StoreFactory.simpleStoreBuilder<List<Cat>>()
        .build(onFetch = catsDataSource::fetchCats)
        .whenActive {
            catEvents.observeCatEvents().collect { event ->
                optimisticUpdate { oldList ->
                    emit(oldList.map { if (it.id == event.cat.id) event.cat else it })
                }
            }
        }

    fun getCats(): Flow<StoreResult<List<Cat>>> = store.observe()
}
```

The `whenActive` block is started when the list store gets its first
observer and cancelled when the cache is released, so the subscription does
not outlive the cached data.

## Sharing a Store Between Screens

Because all observers of a key share one cached value, a singleton
repository with a keyed store naturally keeps multiple screens consistent.
In the demo's shopping example, the cart repository exposes derived flows
built on top of the same store, and the products / details screens combine
them with their own stores:

```kotlin
@Singleton
class CartRepository(
    private val cartDataSource: CartDataSource,
    private val productDetailsObserver: ProductDetailsObserver,
) {

    private val store = StoreFactory.simpleStoreBuilder<List<CartItem>>()
        .build(onFetch = cartDataSource::fetchCart)

    fun getMinimalCart(): Flow<StoreResult<List<CartItem>>> = store.observe()

    fun isInCart(productId: Long): Flow<StoreResult<Boolean>> {
        return getMinimalCart().storeMap { cart ->
            cart.any { it.productId == productId }
        }
    }

    suspend fun addToCart(product: Product) {
        store.optimisticUpdate { cart ->
            emit(cart + CartItem(product.id, quantity = 1))
            cartDataSource.addToCart(product)
        }
    }
}
```

```kotlin
// Product details screen: details + cart membership in one state
val stateFlow: StateFlow<StoreResult<State>> = combineStores(
    productDetailsRepository.getProductById(productId),
    cartRepository.isInCart(productId),
    ::State,
).stateIn(viewModelScope, SharingStarted.Lazily, StoreResult.Loading)
```

An optimistic `addToCart` on one screen is instantly visible on every
other screen observing the cart, with no manual propagation.

## API Summary

| Member                                                       | Description                                                       |
|--------------------------------------------------------------|-------------------------------------------------------------------|
| `observe(key, request)`                                      | Fetch and observe the value for `key`                             |
| `invalidate(key, request)` / `invalidateAsync(key, request)` | Force a reload of one key                                         |
| `optimisticUpdate(key) { }`                                  | Update one key's cache ahead of the real update, with auto-revert |
| `update(key) { }`                                            | Plain cache update for one key (extension)                        |
| `whenActive { }`                                             | Run a block while the store has observers (any key)               |
