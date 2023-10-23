package examples.ph

import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.jni.CadicalSolver
import examples.utils.secondsSince
import examples.utils.timeNow

object GlobalsPH {
    var solverProvider: () -> Solver = {
        // MiniSatSolver()
        // CryptoMiniSatSolver()
        CadicalSolver()
    }

    init {
        solverProvider()
    }
}

private fun main() {
    val timeStart = timeNow()
    val maxP = 11

    provePigeonholePrincipleIterative(maxP)
    provePigeonholePrincipleIncremental(maxP)

    println()
    println("All done in %.3f s!".format(secondsSince(timeStart)))
}
