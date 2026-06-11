package com.elveum.store.demo.feature.examples.store_keyed.shopping.model

import java.util.Locale

fun formatPrice(price: Double): String = String.format(Locale.US, "$%.2f", price)
