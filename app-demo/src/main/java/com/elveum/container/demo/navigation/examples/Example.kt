package com.elveum.container.demo.navigation.examples

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

@Serializable()
sealed class Example {

    abstract val title: String
    abstract val category: Category
    abstract val description: String

    @Composable
    abstract fun Content()

}
