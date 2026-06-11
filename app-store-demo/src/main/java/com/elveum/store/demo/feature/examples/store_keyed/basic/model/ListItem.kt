package com.elveum.store.demo.feature.examples.store_keyed.basic.model

/**
 * Merged model exposed by the repository.
 *
 * It combines the basic [Item] data (loaded as a part of the whole list) with
 * the [description] loaded separately for each item. The [description] is
 * `null` while the item details are still loading and becomes non-null as soon
 * as they arrive.
 */
data class ListItem(
    val item: Item,
    val description: String?,
) {
    val id: Long = item.id
    val title: String = item.title
    val imageUrl: String = item.imageUrl
}

