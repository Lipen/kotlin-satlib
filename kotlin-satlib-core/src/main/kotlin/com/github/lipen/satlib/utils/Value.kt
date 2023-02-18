package com.github.lipen.satlib.utils

interface Value<out T> {
    val value: T

    companion object {
        // constructor
        operator fun <T> invoke(value: T): Value<T> = object : Value<T> {
            override val value: T = value
            override fun toString(): String = this.value.toString()
        }
    }
}
