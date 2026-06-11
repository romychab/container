package com.elveum.store.demo.errors

interface ErrorFlagProvider {
    fun isErrorFlagEnabled(): Boolean
    fun isKeepContentOnErrorFlagEnabled(): Boolean
}
