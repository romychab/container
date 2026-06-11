package com.elveum.store.stores.base

public interface WithStoreLifecycleOwner<Store> {

    /**
     * Register a [block] of code which is executed when the store becomes active (while at
     * least one observer start listening the store, and until cache is released).
     *
     * The [block] is automatically canceled whenever the store transitions into inactive state.
     *
     * You this call to setup relations between entities managed by different stores. For example,
     * you can subscribe to changes or events from other store and update this store when updates
     * arrive.
     */
    public fun whenActive(block: suspend Store.() -> Unit): Store

}
