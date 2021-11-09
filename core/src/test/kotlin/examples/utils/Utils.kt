package examples.utils

import com.soywiz.klock.PerformanceCounter
import com.soywiz.klock.TimeSpan

fun timeNow(): TimeSpan = PerformanceCounter.reference

fun timeSince(timeStart: TimeSpan): TimeSpan = timeNow() - timeStart

fun secondsSince(timeStart: TimeSpan): Double = timeSince(timeStart).seconds
