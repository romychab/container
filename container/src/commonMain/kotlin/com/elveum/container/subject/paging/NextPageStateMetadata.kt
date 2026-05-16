package com.elveum.container.subject.paging

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

public data class NextPageStateMetadata(
    val nextPageState: PageState,
) : ContainerMetadata
