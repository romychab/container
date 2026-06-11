package com.elveum.store.demo.feature.examples.store_keyed.basic.model

/**
 * Basic item data fetched as a part of the whole list.
 *
 * The [description] is loaded separately and lazily for each item, so it is
 * not a part of this model.
 */
data class Item(
    val id: Long,
    val title: String,
    val imageUrl: String,
)
