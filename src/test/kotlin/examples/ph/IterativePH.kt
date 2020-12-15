package examples.ph

import com.github.lipen.satlib.core.IntVarArray
import com.github.lipen.satlib.core.newIntVarArray
import com.github.lipen.satlib.op.atLeastOne
import com.github.lipen.satlib.op.atMostOne
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.useWith
import com.github.lipen.satlib.utils.writeln
import com.soywiz.klock.measureTimeWithResult
import okio.buffer
import okio.sink
import java.io.File

private fun Solver.generateIterative(P: Int, H: Int = P - 1): IntVarArray {
    val pigeonHole = newIntVarArray(P) { 1..H }

    for (p in 1..P)
        atLeastOne { // may be simplified to `atLeastOne(pigeonHole[p].literals`
            for (h in 1..H)
                yield(pigeonHole[p] eq h)
        }

    for (h in 1..H)
        atMostOne {
            for (p in 1..P)
                yield(pigeonHole[p] eq h)
        }

    return pigeonHole
}

fun provePigeonholePrincipleIterative(maxP: Int): Boolean {
    println("Proving pigeonhole principle for maxP = $maxP: Iterative strategy...")
    File("pigeonholeIterative.csv").sink().buffer().use { csv ->
        csv.writeln("P,time")
        for (P in 2..maxP) {
            GlobalsPH.solverProvider().useWith {
                // println("P = $P")
                generateIterative(P)

                // =====
                // val cnf = File("cnf-iter-$P")
                // dumpDimacs(cnf)
                // =====

                val (isSat, time) = measureTimeWithResult { solve() }
                if (isSat) {
                    println("P = $P: Ooops")
                } else {
                    println("P = $P: OK in %.3f s".format(time.seconds))
                }
                csv.writeln("$P,${time.seconds}").flush()
            }
        }
    }
    return true
}

private fun main() {
    val maxP = 10
    provePigeonholePrincipleIterative(maxP)
}
