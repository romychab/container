package com.elveum.container.demo.feature.examples.container_values

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.elveum.container.Container
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.components.Heading
import com.elveum.container.errorContainer
import com.elveum.container.exceptionOrNull
import com.elveum.container.getOrNull
import com.elveum.container.pendingContainer
import com.elveum.container.successContainer

@Composable
fun ContainerValuesScreen() = DemoScaffold(
    title = "Container Values",
    description = """
        Use the **getOrNull()** and **exceptionOrNull()** extension functions to safely extract
        a nullable value or exception from any container. These functions are useful
        when you only need to handle one specific container state.
    """.trimIndent()
) {
    Heading("Container.Success")
    ContainerValues(
        successContainer("Hello from the Container!")
    )

    Heading("Container.Error")
    ContainerValues(
        errorContainer(RuntimeException("Failure from the Container!"))
    )

    Heading("Container.Pending")
    ContainerValues(
        pendingContainer()
    )
}

@Composable
private fun ContainerValues(container: Container<String>) {
    val value: String? = container.getOrNull()
    val exception: Exception? = container.exceptionOrNull()

    Row {
        Text(
            text = "getOrNull(): ",
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
        Text(
            text = "$value",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
        )
    }
    Row {
        Text(
            text = "exceptionOrNull(): ",
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
        Text(
            text = "${exception?.javaClass?.simpleName}",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
        )
    }

}