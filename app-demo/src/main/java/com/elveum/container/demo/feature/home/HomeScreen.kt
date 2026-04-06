package com.elveum.container.demo.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.elveum.container.demo.navigation.examples.Example
import com.elveum.container.demo.navigation.examples.ExampleRegistry
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.components.Heading

@Composable
fun HomeScreen(onExampleSelected: (Example) -> Unit) {
    DemoScaffold(
        title = "Container Demo",
        hasBackButton = false,
        scrollable = false,
        contentPadding = PaddingValues.Zero,
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            ExampleRegistry.byCategory.forEach { (category, examples) ->
                item(key = category) {
                    Heading(category.title)
                }
                items(examples, key = { it::class.qualifiedName ?: "" }) { example ->
                    ListItem(
                        headlineContent = { Text(example.title) },
                        supportingContent = { Text(example.description) },
                        modifier = Modifier.clickable { onExampleSelected(example) },
                    )
                    if (example != examples.last()) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
