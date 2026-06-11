package com.elveum.store.demo.navigation.examples

object ExampleRegistry {

    val all: List<Example> = listOf(
        Example.BasicSimpleStore,
        Example.PullToRefreshSimpleStore,
        Example.LocalRemoteSuspendingSimpleStore,
        Example.LocalRemoteReactiveSimpleStore,
        Example.ArgsSimpleStore,
        Example.CombinedSimpleStore,

        Example.BasicKeyedStore,
        Example.MasterDetailsKeyedStore,
        Example.ShoppingCartKeyedStore,

        Example.PaginationBasics,
        Example.PaginationStatuses,
        Example.PaginationArgs,
        Example.PaginationUpdates,
        Example.PaginationLocalRemote,
    )

    val byCategory: Map<Category, List<Example>> = all.groupBy { it.category }

}
