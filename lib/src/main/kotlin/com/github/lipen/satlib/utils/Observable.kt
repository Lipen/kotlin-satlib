package com.github.lipen.satlib.utils

abstract class Observable<T> {
    private val _listeners: MutableList<T> = mutableListOf()

    val listeners: List<T> = _listeners

    fun register(listener: T) {
        _listeners.add(listener)
    }

    fun unregister(listener: T) {
        _listeners.remove(listener)
            .also { check(it) }
    }

    fun clear() {
        _listeners.clear()
    }
}
