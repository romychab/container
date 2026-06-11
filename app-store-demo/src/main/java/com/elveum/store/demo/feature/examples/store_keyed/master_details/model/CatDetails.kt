package com.elveum.store.demo.feature.examples.store_keyed.master_details.model

data class CatDetails(
    val cat: Cat,
    val description: String,
) {
    val id = cat.id
    val name = cat.name
    val imageUrl = cat.imageUrl
}