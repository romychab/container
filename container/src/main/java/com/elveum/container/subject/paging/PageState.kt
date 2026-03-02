package com.elveum.container.subject.paging

/**
 * The loading state of the next page.
 */
public sealed class PageState {

    /**
     * The next page is not being loaded.
     */
    public data object Idle : PageState()

    /**
     * The next page is being loaded right now. You can
     * use this state to render a progress indicator at the bottom
     * of your paged list.
     */
    public data object Pending : PageState()

    /**
     * The load of the next page has been failed. Use [retry]
     * function to reload the page again if needed.
     */
    public data class Error(
        val exception: Exception,
        val retry: () -> Unit,
    ) : PageState()

}
