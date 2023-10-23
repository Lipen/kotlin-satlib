@file:Suppress("LocalVariableName")

package examples.bf

import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.jni.CadicalSolver
import examples.utils.secondsSince
import examples.utils.timeNow

object GlobalsBF {
    val solverProvider: () -> Solver = {
        // MiniSatSolver()
        // GlucoseSolver()
        // CryptoMiniSatSolver()
        CadicalSolver()
    }
    var IS_ENCODE_BFS: Boolean = true
    var IS_FORBID_DOUBLE_NEGATION: Boolean = true
    var Pmax: Int = 30
    var timeout: Double = 30.0

    init {
        solverProvider().close()
    }
}

fun main() {
    val timeStart = timeNow()

    // solveAllIterative(X = 3)
    // solveAllIncremental(X = 3)

    // solveAllIterative(X = 4, timeout = 0.8)

    // solveAllIterative(X = 5, timeout = 30.0)
    // solveAllIncremental(X = 5, timeout = 30.0)

    solveAllInterleaved(X = 5, Pmax = 30, timeout = 1200.0)

    // val X = 3
    // val values = "10010110"
    // val tt = values.toTruthTable(X)
    // solveIncrementally(tt)

    // val X = 4
    // // val values = "0000001011110111" // 13
    // // val values = "0000001101100010" // 17
    // val values = "0000000001101000" // 18
    // val tt = values.toTruthTable(X)
    // solveIteratively(tt)
    // solveIncrementally(tt)

    // solveFor(18, tt, timeout = 30.0)
    // solveFor(19, tt, timeout = 30.0)
    // solveFor(20, tt, timeout = 30.0)

    // val X = 3
    // val values = "00100000"
    // val tt = values.toTruthTable(X)
    // solveFor(17, tt, timeout = 30.0)
    // solveIteratively(tt)
    // val X = 3
    // val values = (1..2.bf.pow(X)).map { "01".random() }.joinToString("")
    // val tt = values.toTruthTable(X)
    // solveIteratively(tt)

    // solveRandom(X = 3, n = 10, distribution = "01")
    // solveRandom(X = 4, n = 1000, distribution = "01xxx", timeout = 10.0)
    // solveRandom(X = 5, n = 1500, distribution = "01xx", timeout = 10.0)

    // val X = 4
    // val values = (1..2.bf.pow(X)).map { "01x".random() }.joinToString("")
    // val tt = values.toTruthTable(X)
    // solveIteratively(tt)

    // val X = 3
    // val values = "01101001"
    // val tt = values.toTruthTable(X)
    // solveIteratively(tt, Pmax = 30, timeout = 40.0)

    // val X = 3
    // val values = "00010110"
    // val tt = values.toTruthTable(X)
    // solveIteratively(tt, Pmax = 30, timeout = 10.0)

    // val X = 5
    // val values = "100xxx01xxxxx11x00101x110xxx0xxx"
    // val tt = values.toTruthTable(X)
    // for (P in 30 downTo 20) {
    //     solveFor(P, tt, timeout = 30.0)
    // }
    // solveIteratively(tt)

    println()
    println("All done in %.3f s".format(secondsSince(timeStart)))
}
