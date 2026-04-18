package com.elveum.container.subject.paging.internal

internal class PageKeyProcessor<Key, T>(
    private val pageResultsEmitter: PageResultsEmitter<Key, T>,
    private val store: PageLoaderRecordStore<Key, T>
) {

    @Volatile
    private var isImmediateLaunchScheduled = false

    suspend fun continueKey(key: Key) {
        store.onKeyContinued(key)
        pageResultsEmitter.emitResults()
    }

    fun completeKey(key: Key) {
        store.onKeyCompleted(key)
    }

    suspend fun failKey(key: Key, exception: Exception) {
        store.onKeyFailed(key, exception)
        pageResultsEmitter.emitResults()
    }

    fun scheduleImmediateLaunch() {
        isImmediateLaunchScheduled = true
    }

    fun resetScheduledImmediateLaunch() {
        isImmediateLaunchScheduled = false
    }

    fun isImmediateLaunchScheduled(): Boolean {
        return isImmediateLaunchScheduled
    }

}
