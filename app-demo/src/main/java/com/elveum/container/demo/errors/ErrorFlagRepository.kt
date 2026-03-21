package com.elveum.container.demo.errors

import kotlinx.coroutines.flow.StateFlow

interface ErrorFlagRepository {
    fun toggle()
    fun getErrorFlag(): StateFlow<Boolean>
}
