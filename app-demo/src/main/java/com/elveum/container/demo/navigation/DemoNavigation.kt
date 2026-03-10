package com.elveum.container.demo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.elveum.container.demo.feature.examples.ExampleScreen
import com.elveum.container.demo.feature.home.HomeScreen

@Composable
fun DemoNavigation() {
    val backStack = rememberNavBackStack(HomeRoute)
    val navController = remember { NavController(backStack) }
    CompositionLocalProvider(
        LocalNavController provides navController
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<HomeRoute> {
                    HomeScreen(onExampleSelected = { example -> backStack.add(ExampleRoute(example)) })
                }
                entry<ExampleRoute> { route ->
                    ExampleScreen(example = route.example)
                }
            },
        )
    }
}
