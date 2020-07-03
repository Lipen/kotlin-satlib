package com.github.lipen.satlib.utils

import okio.BufferedSink
import okio.BufferedSource

@Suppress("FunctionName")
fun <T> Iterable<T>.toList_(): List<T> = when (this) {
    is List<T> -> this
    else -> toList()
}

fun <T> Iterable<T>.pairs(): Sequence<Pair<T, T>> = sequence {
    val pool = this@pairs.toList_()
    for (i in pool.indices) {
        for (j in pool.indices) {
            if (i < j) yield(Pair(pool[i], pool[j]))
        }
    }
}

fun BufferedSource.lineSequence(): Sequence<String> =
    sequence<String> { while (true) yield(readUtf8Line() ?: break) }.constrainOnce()

fun BufferedSink.write(s: String): BufferedSink = writeUtf8(s)

fun BufferedSink.writeln(s: String): BufferedSink = write(s).writeByte(10) // 10 is '\n'

inline fun <T : AutoCloseable?, R> T.useWith(block: T.() -> R): R = use(block)

val <T> T.exhaustive: T
    get() = this
