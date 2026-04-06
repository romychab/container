package com.elveum.container.demo.errors

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorFlagStore @Inject constructor() : ErrorFlagProvider, ErrorFlagRepository {

    private val errorFlagFlow = MutableStateFlow(false)

    override fun isErrorFlagEnabled(): Boolean {
        return errorFlagFlow.value
    }

    override fun toggle() {
        errorFlagFlow.update { !it }
    }

    override fun getErrorFlag(): StateFlow<Boolean> {
        return errorFlagFlow
    }


}
