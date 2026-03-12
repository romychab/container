package com.elveum.container.demo.feature.examples

import androidx.compose.runtime.Composable
import com.elveum.container.demo.navigation.examples.Example

@Composable
fun ExampleScreen(example: Example) {
    example.Content()
}
