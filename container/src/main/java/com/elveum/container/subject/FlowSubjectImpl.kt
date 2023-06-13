package com.elveum.container.subject

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FlowSubjectImpl<T> : FlowSubject<T> {

    private var state: State<T> = State.NotInitialized
    private val listeners = mutableSetOf<Listener<T>>()

    override fun onNext(value: T) = synchronized(this) {
        if (state is State.Closed) return
        state = State.WithValue(value)
        listeners.forEach { it.onNext(value) }
    }

    override fun onError(e: Throwable) = synchronized(this) {
        if (state is State.Closed) return
        state = State.Closed(e, null)
        listeners.forEach { it.onComplete(e) }
    }

    override fun onComplete() = synchronized(this) {
        val state = this.state
        if (state is State.Closed) return
        val value = if (state is State.WithValue) {
            state.value
        } else {
            null
        }
        this.state = State.Closed(null, value)
        listeners.forEach {
            it.onComplete()
        }
    }

    override fun flow(): Flow<T> {
        return callbackFlow {
            val listener = object : Listener<T> {
                override fun onNext(value: T) {
                    trySend(value)
                }

                override fun onComplete(e: Throwable?) {
                    channel.close(e)
                }
            }
            add(listener)
            awaitClose {
                remove(listener)
            }
        }
    }

    private fun add(listener: Listener<T>) = synchronized(this) {
        val state = this.state
        if (state is State.Closed) {
            if (state.value != null) listener.onNext(state.value)
            listener.onComplete(state.exception)
        } else {
            listeners.add(listener)
            if (state is State.WithValue) {
                listener.onNext(state.value)
            }
        }
    }

    private fun remove(listener: Listener<T>) = synchronized(this) {
        listeners.remove(listener)
    }

    private interface Listener<T> {
        fun onNext(value: T)
        fun onComplete(e: Throwable? = null)
    }

    private sealed class State<out T> {
        object NotInitialized : State<Nothing>()
        class WithValue<T>(val value: T) : State<T>()
        class Closed<T>(val exception: Throwable?, val value: T?): State<T>()
    }
}