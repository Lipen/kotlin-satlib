package examples.ph

import com.github.lipen.satlib.core.DomainVar
import com.github.lipen.satlib.core.IntVarArray
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.eq
import com.github.lipen.satlib.core.newIntVar
import com.github.lipen.satlib.core.newIntVarArray
import com.github.lipen.satlib.op.atLeastOne
import com.github.lipen.satlib.op.atMostOne
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.solve
import com.github.lipen.satlib.utils.useWith
import com.github.lipen.satlib.utils.writeln
import com.soywiz.klock.measureTimeWithResult
import examples.utils.secondsSince
import examples.utils.timeNow
import okio.buffer
import okio.sink
import java.io.File

private inline fun <T> DomainVar<T>.changeDomain(
    newDomain: Iterable<T>,
    init: (T) -> Lit,
): DomainVar<T> =
    DomainVar.new(newDomain) { d ->
        if (d in domain) this eq d else init(d)
    }

private fun Solver.generateIncremental(pigeonHole: IntVarArray, P: Int, H: Int = P - 1): Int {
    require(H > 0)

    val activation: Int = newLiteral()

    for (p in 1..P)
        atLeastOne {
            yield(-activation)
            for (h in 1..H)
                yield(pigeonHole[p] eq h)
        }

    // TODO: some clauses were already declared on the previous iteration,
    // TODO: so, ideally, it would be not to declare them again
    for (h in 1..H)
        atMostOne {
            for (p in 1..P)
                yield(pigeonHole[p] eq h)
        }

    return activation
}

private fun Solver.generateInitialTable(): IntVarArray {
    return newIntVarArray(1, encodeOneHot = false) { emptyList() }
}

private fun Solver.generateNextTable(pigeonHole: IntVarArray): IntVarArray {
    val oldP = pigeonHole.shape[0]
    val newP = oldP + 1
    val newH = newP - 1
    return IntVarArray.new(newP) { (p) ->
        if (p <= oldP) pigeonHole[p].changeDomain(1..newH) { newLiteral() }
        else newIntVar(1..newH, encodeOneHot = false)
    }
}

fun provePigeonholePrincipleIncremental(maxP: Int): Boolean {
    println("Proving pigeonhole principle for maxP = $maxP: Incremental strategy...")
    File("pigeonholeIncremental.csv").sink().buffer().use { csv ->
        csv.writeln("P,time")
        GlobalsPH.solverProvider().useWith {
            var pigeonHole = generateInitialTable()
            for (P in 2..maxP) {
                // println("P = $P")
                pigeonHole = generateNextTable(pigeonHole)
                // for (p in 1..P) {
                //     println("pigeonHole[p = $p] = ${pigeonHole[p].literals}")
                // }
                val activation = generateIncremental(pigeonHole, P)
                // println("assumption = $activation")

                // =====
                // val cnf = File("cnf-incr-$P")
                // dumpDimacs(cnf)
                // cnf.appendText("$activation 0\n")
                // =====

                val (isSat, time) = measureTimeWithResult { solve(activation) }
                csv.writeln("$P,${time.seconds}").flush()
                if (isSat) {
                    println("!!!")
                    println("P = $P: Ooops in %.3fs".format(time.seconds))
                    println("model = ${getModel()}")
                    println("!!!")
                } else {
                    println("P = $P: OK in %.3fs".format(time.seconds))
                }

                addClause(-activation)
            }
        }
    }
    return true
}

private fun main() {
    val timeStart = timeNow()
    val maxP = 10

    provePigeonholePrincipleIncremental(maxP)

    println()
    println("All done in %.3f s!".format(secondsSince(timeStart)))
}
