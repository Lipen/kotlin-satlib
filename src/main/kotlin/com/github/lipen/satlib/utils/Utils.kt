package com.github.lipen.satlib.utils

@Suppress("FunctionName")
internal fun <T> Iterable<T>.toList_(): List<T> = when (this) {
    is List<T> -> this
    else -> toList()
}

internal fun <T> Iterable<T>.pairs(): Sequence<Pair<T, T>> = sequence {
    val pool = this@pairs.toList_()
    for (i in pool.indices) {
        for (j in pool.indices) {
            if (i < j) yield(Pair(pool[i], pool[j]))
        }
    }
}
