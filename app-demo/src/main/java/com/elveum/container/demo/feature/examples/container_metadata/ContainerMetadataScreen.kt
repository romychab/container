package com.elveum.container.demo.feature.examples.container_metadata

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.elveum.container.BackgroundLoadState
import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.LocalSourceType
import com.elveum.container.ReloadFunction
import com.elveum.container.SourceType
import com.elveum.container.backgroundLoadState
import com.elveum.container.demo.ui.components.DemoScaffold
import com.elveum.container.demo.ui.components.Heading
import com.elveum.container.get
import com.elveum.container.reloadFunction
import com.elveum.container.sourceType
import com.elveum.container.successContainer
import com.elveum.container.update
import java.util.UUID

/**
 * Example of custom metadata field:
 */
data class CustomRequestUuidMetadata(
    val customRequestUuid: UUID,
) : ContainerMetadata

/**
 * Helper extension property for easier access of custom metadata value.
 * It is recommended to name the extension property with the same name
 * as the property within the defined data class:
 */
val ContainerMetadata.customRequestUuid: UUID?
    get() = get<CustomRequestUuidMetadata>()?.customRequestUuid

@Composable
fun ContainerMetadataScreen() = DemoScaffold(
    title = "Container Metadata",
    description = """
        Any **Container.Completed** instance (either Success, or Error) can ship additional
        metadata values. The library provides built-in metadata fields such as **sourceType**,
        **backgroundLoadState** and **reload()** function. You can also define your own metadata
        fields by creating a data class that implements the **ContainerMetadata** interface.
    """.trimIndent()
) {

    // By default, metadata is empty:
    val emptyMetadataContainer: Container.Completed<String> = successContainer(
        value = "Hello from the Container!",
    )

    // but you can clone the container and provide metadata values:
    val containerWithMetadata = emptyMetadataContainer.update {
        // set the origin source of data encapsulated into the container:
        sourceType = LocalSourceType
        // set an additional flag indicating that the loading is not finished
        // yet (for example, new values will arrive from the remote data source soon):
        backgroundLoadState = BackgroundLoadState.Loading
        // set optional reload function:
        reloadFunction = {
            /* execute reload here (it is recommended to store reload lambda somewhere
               and reuse the same instance) */
        }
        // custom metadata property:
        metadata += CustomRequestUuidMetadata(UUID.randomUUID())
    }

    // any metadata value can be read from a 'metadata' property:

    Heading("Container SourceType")
    val sourceType: SourceType = containerWithMetadata.sourceType
    Text(sourceType.toString())

    Heading("Container BackgroundLoadState")
    val backgroundLoadState: BackgroundLoadState = containerWithMetadata.backgroundLoadState
    Text(backgroundLoadState.toString())

    Heading("Container ReloadFunction")
    val reloadFunction: ReloadFunction = containerWithMetadata.metadata.reloadFunction
    Text(reloadFunction::class.simpleName ?: "-")

    Heading("Container Custom UUID")
    val uuid: UUID? = containerWithMetadata.metadata.customRequestUuid
    Text("$uuid")

    // Also metadata fields are accessible when using fold() function
    // (Success and Error branches):
    containerWithMetadata.fold(
        onPending = { },
        onSuccess = {
            println("Source Type: ${this.sourceType}")
            println("Background Load State: ${this.backgroundLoadState}")
            println("Custom UUID: ${this.metadata.customRequestUuid}")
        },
        onError = {
            println("Source Type: ${this.sourceType}")
            println("Background Load State: ${this.backgroundLoadState}")
            println("Custom UUID: ${this.metadata.customRequestUuid}")
        }
    )
}
