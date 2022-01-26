package nexus.eqsimp

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.op.iffAnd
import com.github.lipen.satlib.solver.GlucoseSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.useWith
import com.soywiz.klock.measureTimeWithResult
import examples.bf.toInt
import examples.utils.secondsSince
import examples.utils.timeNow
import mu.KotlinLogging
import nexus.aig.Aig
import nexus.aig.AigAndGate
import nexus.aig.parseAig
import nexus.aig.toposort

private val log = KotlinLogging.logger {}

@Suppress("LocalVariableName")
private fun Solver.declareBase(aig: Aig) {
    context["aig"] = aig
    val V = context("V") { aig.size }
    check(aig.mapping.keys == (1..V).toSet())
    val Y = context("Y") { aig.outputs.size }

    println("aig = $aig")
    println("V = $V")
    println("Y = $Y")

    val nodeValue = context("nodeValue") {
        newBoolVarArray(V)
    }
    val outputValue = context("outputValue") {
        newBoolVarArray(Y) { (y) ->
            val out = aig.outputs[y - 1]
            nodeValue[out.id] sign !out.negated
        }
    }

    comment("AND gate semantics")
    for (v in 1..V) {
        val gate = aig.node(v)
        if (gate is AigAndGate) {
            iffAnd(
                nodeValue[v],
                nodeValue[gate.left.id] sign !gate.left.negated,
                nodeValue[gate.right.id] sign !gate.right.negated,
            )
        }
    }
}

private fun Iterable<Boolean>.toBinaryString(): String =
    joinToString("") { it.toInt().toString() }

fun main() {
    val timeStart = timeNow()

    // val filename = "data/and.aag"
    // val filename = "data/halfadder.aag"
    // val filename = "data/eq.aag"
    // val filename = "data/eq2.aag"
    // val filename = "data/BubbleSort_4_3.aag"
    // val filename = "data/BubbleSort_6_4.aag"
    // val filename = "data/BubbleSort_7_4.aag"
    val filename = "C:\\Dropbox\\Documents\\PhD\\sat\\instances\\ISCAS\\aag/c1908.aag"
    val aig = parseAig(filename)
    log.info("Result: $aig")

    val deps = aig.dependency_graph()
    for ((i, layer) in toposort(deps).withIndex()) {
        log.info("Layer #${i + 1} (${layer.size} nodes): $layer")
    }

    GlucoseSolver().useWith {
        declareBase(aig)

        val (isSat, timeSolve) = measureTimeWithResult { solve() }
        println("${if (isSat) "SAT" else "UNSAT"} in %.3fs".format(timeSolve.seconds))

        if (isSat) {
            // val model = getModel()
            // val nodeValue = context.convertBoolVarArray("nodeValue", model)
            // val outputValue = context.convertBoolVarArray("outputValue", model)
            //
            // println("model = ${model.data.toBinaryString()}")
            // println("nodeValue = ${nodeValue.values.toBinaryString()}")
            // println("outputValue = ${outputValue.values.toBinaryString()}")

            val V: Int = context["V"]
            val nodeValueVar: BoolVarArray = context["nodeValue"]

            println("Searching for equivalent gates...")
            val equivalentPairs: MutableList<Pair<Int, Int>> = mutableListOf()
            val antiEquivalentPairs: MutableList<Pair<Int, Int>> = mutableListOf()
            for (v1 in (aig.inputs.size + 1)..V)
                for (v2 in (v1 + 1)..V) {
                    val table = listOf(
                        Pair(false, false),
                        Pair(false, true),
                        Pair(true, false),
                        Pair(true, true)
                    ).map { (x1, x2) ->
                        val assumptions = listOf(
                            nodeValueVar[v1] sign x1,
                            nodeValueVar[v2] sign x2,
                        )
                        val (isSubSat, timeSubSolve) = measureTimeWithResult { solve(assumptions) }
                        // println("${if (isSubSat) "SAT" else "UNSAT"} assuming ($x1, $x2) in %.3fs".format(timeSubSolve.seconds))
                        isSubSat
                    }
                    // println("(v1,v2)=($v1,$v2) :: ${table.toBinaryString()}")
                    if (table.toBinaryString() == "1001") {
                        println(" -- Found equivalent gates: $v1 and $v2")
                        equivalentPairs.add(Pair(v1, v2))
                    }
                    if (table.toBinaryString() == "0110") {
                        println(" -- Found anti-equivalent gates: $v1 and $v2")
                        antiEquivalentPairs.add(Pair(v1, v2))
                    }
                }
            println("Done searching for equivalent gates")
            println("Total equivalent pairs (EQ+XOR): ${equivalentPairs.size} + ${antiEquivalentPairs.size} = ${equivalentPairs.size + antiEquivalentPairs.size}")
        } else {
            error("Unexpected UNSAT")
        }
    }

    log.info("All done in %.3f s".format(secondsSince(timeStart)))
}
