package com.github.lipen.satlib.utils

import okio.BufferedSink
import okio.BufferedSource

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

internal fun BufferedSource.lineSequence(): Sequence<String> =
    sequence<String> { while (true) yield(readUtf8Line() ?: break) }.constrainOnce()

internal fun BufferedSink.write(s: String): BufferedSink = writeUtf8(s)

internal fun BufferedSink.writeln(s: String): BufferedSink = write(s).writeByte(10) // 10 is '\n'

internal inline fun <T : AutoCloseable?, R> T.useWith(block: T.() -> R): R = use(block)

val <T> T.exhaustive: T
    get() = this
