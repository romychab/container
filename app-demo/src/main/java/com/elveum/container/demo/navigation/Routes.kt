package com.elveum.container.demo.navigation

import androidx.navigation3.runtime.NavKey
import com.elveum.container.demo.navigation.examples.Example
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data class ExampleRoute(val example: Example) : NavKey
