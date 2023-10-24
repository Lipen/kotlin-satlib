package examples.utils

import korlibs.time.PerformanceCounter
import korlibs.time.TimeSpan
import kotlin.math.pow

fun timeNow(): TimeSpan = PerformanceCounter.reference

fun timeSince(timeStart: TimeSpan): TimeSpan = timeNow() - timeStart

fun secondsSince(timeStart: TimeSpan): Double = timeSince(timeStart).seconds

fun Int.pow(n: Int): Int =
    if (this == 2) 1 shl n
    else this.toDouble().pow(n).toInt()

fun Long.pow(n: Int): Long =
    if (this == 2L) 1L shl n
    else this.toDouble().pow(n).toLong()

fun String.toBooleanArray(): BooleanArray {
    return BooleanArray(length) { i ->
        when (this[i]) {
            '1' -> true
            '0' -> false
            else -> error("All characters in string '$this' must be '1' or '0'")
        }
    }
}

fun String.toBooleanList(): List<Boolean> {
    return map {
        when (it) {
            '1' -> true
            '0' -> false
            else -> error("All characters in string '$it' must be '1' or '0'")
        }
    }
}

fun BooleanArray.toBinaryString(): String {
    return asIterable().toBinaryString()
}

fun Iterable<Boolean>.toBinaryString(): String {
    return joinToString("") { if (it) "1" else "0" }
}

@JvmName("toBinaryStringNullable")
fun Iterable<Boolean?>.toBinaryString(): String {
    return joinToString("") {
        when (it) {
            true -> "1"
            false -> "0"
            null -> "x"
        }
    }
}
