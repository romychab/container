package com.elveum.container.utils

import com.elveum.container.Container

fun <T> Iterable<Container<T>>.raw() = map { it.raw() }
