package com.elveum.store.demo.errors

import kotlinx.coroutines.flow.StateFlow

interface ErrorFlagRepository {

    fun toggleErrorFlag()
    fun getErrorFlag(): StateFlow<Boolean>

    fun toggleKeepContentOnErrorFlag()
    fun getKeepContentOnErrorFlag(): StateFlow<Boolean>

}
