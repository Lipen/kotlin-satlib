package com.github.lipen.satlib.nexus.utils

import com.soywiz.klock.PerformanceCounter
import com.soywiz.klock.TimeSpan
import kotlin.math.pow

internal fun isEven(i: Int): Boolean = (i and 1) == 0
internal fun isOdd(i: Int): Boolean = (i and 1) != 0

internal fun timeNow(): TimeSpan = PerformanceCounter.reference
internal fun timeSince(timeStart: TimeSpan): TimeSpan = timeNow() - timeStart
internal fun secondsSince(timeStart: TimeSpan): Double = timeSince(timeStart).seconds

internal fun Boolean.toInt(): Int = if (this) 1 else 0

internal fun Iterable<Boolean>.toBinaryString(): String = joinToString("") { if (it) "1" else "0" }

/** Returns the [i]-th bit (1-based, LSB-to-MSB order) of the number. */
internal fun Int.bit(i: Int): Boolean = (this and (1 shl (i - 1))) != 0

internal fun Collection<Double>.mean(): Double = sum() / size
internal fun Collection<Double>.geomean(): Double = reduce(Double::times).pow(1 / size)
