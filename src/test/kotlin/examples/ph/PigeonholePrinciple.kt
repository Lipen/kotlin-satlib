package examples.ph

import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver
import com.soywiz.klock.PerformanceCounter
import examples.bf.timeSince

object GlobalsPH {
    var solverProvider: () -> Solver = {
        MiniSatSolver()
        // CryptoMiniSatSolver()
        // CadicalSolver()
    }

    init {
        solverProvider()
    }
}

fun main() {
    val maxP = 11
    val timeStart = PerformanceCounter.reference

    provePigeonholePrincipleIterative(maxP)
    provePigeonholePrincipleIncremental(maxP)

    println()
    println("All done in %.3f s".format(timeSince(timeStart).seconds))
}
