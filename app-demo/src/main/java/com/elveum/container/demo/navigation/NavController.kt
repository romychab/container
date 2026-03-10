package com.elveum.container.demo.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("NavController is not provided.")
}

class NavController(
    private val backStack: NavBackStack<NavKey>
) {

    fun navigateUp() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

}
