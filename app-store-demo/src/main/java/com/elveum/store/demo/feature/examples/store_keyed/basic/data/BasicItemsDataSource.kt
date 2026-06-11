package com.elveum.store.demo.feature.examples.store_keyed.basic.data

import com.elveum.store.demo.feature.examples.store_keyed.basic.model.Item
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Single source of truth for the basic example.
 *
 * - [fetchItems] returns the whole list of items with the basic data only.
 * - [fetchDescription] returns the additional description for a single item by
 *   its ID with a random delay, so descriptions for different items arrive at
 *   different moments.
 */
@Singleton
class BasicItemsDataSource @Inject constructor(
    private val faker: Faker,
    private val random: Random,
) {

    private val items = MutableList(20) {
        StoredItem(
            item = Item(
                id = it + 1L,
                title = faker.cat().name(),
                imageUrl = ItemImages[it % ItemImages.size],
            ),
            description = faker.lorem().sentence(8, 5),
        )
    }

    suspend fun fetchItems(): List<Item> {
        delay(2000)
        return items.map { it.item }
    }

    suspend fun fetchDescription(id: Long): String {
        delay(random.nextLong(500, 3000))
        return items.first { it.item.id == id }.description
    }

    private class StoredItem(
        val item: Item,
        val description: String,
    )
}

private val ItemImages = listOf(
    "https://images.unsplash.com/photo-1615454299901-de13b71ecaae?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODAyNw&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1604916287784-c324202b3205?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODAzMQ&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1596854372407-baba7fef6e51?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODA0Nw&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1608032364895-0da67af36cd2?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODA0Mw&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1571988840298-3b5301d5109b?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODA0Ng&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1494256997604-768d1f608cac?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODEyMQ&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/flagged/photo-1557427161-4701a0fa2f42?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODEyNQ&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1640384974326-3e72680e0fb3?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODI0MA&ixlib=rb-1.2.1&q=80&w=1080",
    "https://images.unsplash.com/photo-1623876159473-5e79be88f7ac?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=MnwxfDB8MXxyYW5kb218MHx8Y2F0fHx8fHx8MTY2MjQ3ODIzMw&ixlib=rb-1.2.1&q=80&w=1080"
)
