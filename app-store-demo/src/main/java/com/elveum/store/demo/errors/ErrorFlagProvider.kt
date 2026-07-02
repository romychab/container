package com.elveum.store.demo.errors

import kotlinx.coroutines.flow.Flow

interface ErrorFlagProvider {
    fun isErrorFlagEnabled(): Boolean
    fun isKeepContentOnErrorFlagEnabled(): Flow<Boolean>
}
