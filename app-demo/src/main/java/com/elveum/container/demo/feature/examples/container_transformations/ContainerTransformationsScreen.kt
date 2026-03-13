package com.elveum.container.demo.feature.examples.container_transformations

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elveum.container.Container
import com.elveum.container.RemoteSourceType
import com.elveum.container.SourceTypeMetadata
import com.elveum.container.catch
import com.elveum.container.catchAll
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.components.Heading
import com.elveum.container.errorContainer
import com.elveum.container.map
import com.elveum.container.mapException
import com.elveum.container.pendingContainer
import com.elveum.container.recover
import com.elveum.container.successContainer
import com.elveum.container.transform
import com.elveum.container.update
import java.io.IOException

class OriginException : RuntimeException()

@Composable
fun ContainerTransformationsScreen() = DemoScaffold(
    title = "Container Transformations",
    description = """
        Containers can be transformed without unwrapping the encapsulated value by using functions
        such as **map, mapException, catchAll, catch, recover, transform, fold, update, filterMetadata**.
    """.trimIndent()
) {
    val pendingContainer: Container<String> = pendingContainer()
    val successContainer: Container<String> = successContainer("Origin")
    val errorContainer: Container<String> = errorContainer(OriginException())

    Heading("Origin containers")
    ContainerShowcase(pendingContainer, successContainer, errorContainer)

    Heading("map() function")
    Text("This function can safely convert any success value encapsulated in the container. " +
            "The map() function does not affect Error or Pending containers.")
    ContainerShowcase(
        pendingContainer = pendingContainer.map { value -> value.uppercase() },
        errorContainer = errorContainer.map { value -> value.uppercase() },
        successContainer = successContainer.map { value -> value.uppercase() },
    )

    Heading("mapException() function")
    Text("The mapException() function converts an exception of type T into another exception type. " +
            "Useful when you need to wrap or map external exceptions into custom in-app exceptions.")
    ContainerShowcase(
        pendingContainer = pendingContainer.mapException(RuntimeException::class) { ex ->
            IOException(ex)
        },
        errorContainer = errorContainer.mapException(RuntimeException::class) { ex ->
            IOException(ex)
        },
        successContainer = successContainer.mapException(RuntimeException::class) { ex ->
            IOException(ex)
        },
    )

    Heading("catchAll() function")
    Text("The catchAll() function converts an Error container with any exception into a new container. " +
            "Only Error containers are affected by this function.")
    ContainerShowcase(
        pendingContainer = pendingContainer.catchAll { ex ->
            successContainer("Caught ${ex.javaClass.simpleName}")
        },
        errorContainer = errorContainer.catchAll { ex ->
            successContainer("Caught ${ex.javaClass.simpleName}")
        },
        successContainer = successContainer.catchAll { ex ->
            successContainer("Caught ${ex.javaClass.simpleName}")
        },
    )

    Heading("catch() function")
    Text("The catch() function converts an Error container with the specified exception type " +
            "into a new container. Only Error containers are affected by this function.")
    ContainerShowcase(
        pendingContainer = pendingContainer.catch(RuntimeException::class) { ex ->
            successContainer("Caught ${ex.javaClass.simpleName}")
        },
        errorContainer = errorContainer.catch(RuntimeException::class) { ex ->
            successContainer("Caught ${ex.javaClass.simpleName}")
        },
        successContainer = successContainer.catch(RuntimeException::class) { ex ->
            successContainer("Caught ${ex.javaClass.simpleName}")
        },
    )

    Heading("recover() function")
    Text("The recover() function converts an Error container with the specified exception type into a Success container.")
    ContainerShowcase(
        pendingContainer = pendingContainer.recover(RuntimeException::class) { ex ->
            "Recovered from\n${ex.javaClass.simpleName}"
        },
        errorContainer = errorContainer.recover(RuntimeException::class) { ex ->
            "Recovered from\n${ex.javaClass.simpleName}"
        },
        successContainer = successContainer.recover(RuntimeException::class) { ex ->
            "Recovered from\n${ex.javaClass.simpleName}"
        },
    )

    Heading("transform() function")
    Text("The transform() function can be used to write your own extensions. It converts a " +
            "Success or Error container into another container. The Pending container is not affected.")
    ContainerShowcase(
        pendingContainer = pendingContainer.transform(
            onError = { e -> successContainer("Success from\n${e.javaClass.simpleName}") },
            onSuccess = { value -> successContainer(value.uppercase()) }
        ),
        errorContainer = errorContainer.transform(
            onError = { e -> successContainer("Success from\n${e.javaClass.simpleName}") },
            onSuccess = { value -> successContainer(value.uppercase()) }
        ),
        successContainer = successContainer.transform(
            onError = { e -> successContainer("Success from\n${e.javaClass.simpleName}") },
            onSuccess = { value -> successContainer(value.uppercase()) }
        ),
    )

    Heading("fold() function")
    Text("The fold() function can not only be used for rendering containers, but also for low-level conversion " +
            "of container instances into other containers or even into other types. See the source of ContainerTransformationsScreen for more details.")

    val value: String = successContainer.fold(
        onPending = { "folded pending" },
        onError = { "folded error" },
        onSuccess = { "folded success" }
    )
    println("Folded a container into String: $value")

    Heading("update() function")
    Text("The update() function does not affect values or exceptions encapsulated in the container. It only " +
            "updates metadata values attached to the container. See the source of ContainerTransformationsScreen for more details.")

    val containerWithMetadata: Container<String> = successContainer.update {
        sourceType = RemoteSourceType
    }
    println("SourceType after update(): ${containerWithMetadata.sourceType}")

    Heading("filterMetadata")
    Text("The filterMetadata() function can exclude metadata values from the container. Encapsulated " +
            "values or exceptions are not affected. See the source of ContainerTransformationsScreen for more details.")
    val containerWithClearedMetadata: Container<String> = containerWithMetadata
        .filterMetadata { metadata ->
            metadata !is SourceTypeMetadata
        }
    println("SourceType after filterMetadata(): ${containerWithClearedMetadata.sourceType}")
}

@Composable
private fun ContainerShowcase(
    pendingContainer: Container<String>,
    successContainer: Container<String>,
    errorContainer: Container<String>,
) {
    Row {
        Text(
            text = "Pending",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Success",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Error",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
    }
    Row {
        ContainerContent(pendingContainer)
        ContainerContent(successContainer)
        ContainerContent(errorContainer)
    }
}

@Composable
private fun RowScope.ContainerContent(container: Container<String>) {
    container.fold(
        onPending = {
            CircularProgressIndicator(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize()
                    .size(24.dp)
            )
        },
        onSuccess = { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize()
            )
        },
        onError = { exception ->
            Text(
                text = "${exception.javaClass.simpleName}",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize()
            )
        }
    )
}