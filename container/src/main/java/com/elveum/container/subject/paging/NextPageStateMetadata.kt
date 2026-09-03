package com.elveum.container.subject.paging

import com.elveum.container.Container
import com.elveum.container.ContainerMetadata
import com.elveum.container.get

/**
 * Get the current state of the next page load. This state can be rendered
 * at the bottom of any paged list.
 *
 * @see PageState
 */
public val ContainerMetadata.nextPageState: PageState
    get() = get<NextPageStateMetadata>()?.nextPageState ?: PageState.Idle

/**
 * Get the current state of the next page load directly from the container,
 * without going through [Container.metadata].
 *
 * @see PageState
 */
public val Container<*>.nextPageState: PageState
    get() = metadata.nextPageState

public data class NextPageStateMetadata(
    val nextPageState: PageState,
) : ContainerMetadata
