package com.elveum.store.demo.errors

import com.uandcode.hilt.autobind.AutoBinds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@AutoBinds
class ErrorFlagStore @Inject constructor() : ErrorFlagProvider, ErrorFlagRepository {

    private val errorFlagFlow = MutableStateFlow(false)
    private val keepContentOnErrorFlow = MutableStateFlow<Boolean>(false)

    override fun isErrorFlagEnabled(): Boolean {
        return errorFlagFlow.value
    }

    override fun isKeepContentOnErrorFlagEnabled(): Flow<Boolean> {
        return keepContentOnErrorFlow
    }

    override fun toggleErrorFlag() {
        errorFlagFlow.update { !it }
    }

    override fun getErrorFlag(): StateFlow<Boolean> {
        return errorFlagFlow
    }

    override fun toggleKeepContentOnErrorFlag() {
        keepContentOnErrorFlow.update { !it }
    }

    override fun getKeepContentOnErrorFlag(): StateFlow<Boolean> {
        return keepContentOnErrorFlow
    }
}
