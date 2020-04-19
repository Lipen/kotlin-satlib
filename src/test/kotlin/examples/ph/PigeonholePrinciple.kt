package examples.ph

import com.soywiz.klock.PerformanceCounter
import examples.bf.timeSince

fun main() {
    val maxP = 3
    val timeStart = PerformanceCounter.reference

    provePigeonholePrincipleIterative(maxP)
    provePigeonholePrincipleIncremental(maxP)

    println()
    println("All done in %.3f s".format(timeSince(timeStart).seconds))
}
