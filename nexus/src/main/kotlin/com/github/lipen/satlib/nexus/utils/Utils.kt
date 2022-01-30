package com.github.lipen.satlib.nexus.utils

import com.soywiz.klock.PerformanceCounter
import com.soywiz.klock.TimeSpan

internal fun isEven(i: Int): Boolean = (i % 2 == 0)
internal fun isOdd(i: Int): Boolean = !isEven(i)

internal fun timeNow(): TimeSpan = PerformanceCounter.reference
internal fun timeSince(timeStart: TimeSpan): TimeSpan = timeNow() - timeStart
internal fun secondsSince(timeStart: TimeSpan): Double = timeSince(timeStart).seconds

internal fun Boolean.toInt(): Int = if (this) 1 else 0

internal fun Iterable<Boolean>.toBinaryString(): String = joinToString("") { if (it) "1" else "0" }