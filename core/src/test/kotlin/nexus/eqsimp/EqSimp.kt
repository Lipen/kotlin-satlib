package nexus.eqsimp

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.convertBoolVarArray
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.op.iffAnd
import com.github.lipen.satlib.solver.GlucoseSolver
import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.useWith
import com.soywiz.klock.measureTimeWithResult
import examples.bf.toInt
import examples.utils.secondsSince
import examples.utils.timeNow
import mu.KotlinLogging
import nexus.aig.Aig
import nexus.aig.parseAig

private val log = KotlinLogging.logger {}

@Suppress("LocalVariableName")
private fun Solver.declareBase(aig: Aig) {
    val nodes = aig.layers().flatten().toList()
    fun id2index(id: Int) = nodes.indexOf(id) + 1

    // Constants
    context["aig"] = aig
    context["nodes"] = nodes
    val V = context("V") { nodes.size }
    val X = context("X") { aig.inputs.size }
    val Y = context("Y") { aig.outputs.size }

    log.info("V = $V, X = $Y, Y = $Y")

    // Variables
    val nodeValue = context("nodeValue") {
        newBoolVarArray(V)
    }

    // Constraints

    comment("AND gate semantics")
    for (v in (X + 1)..V) {
        val gate = aig.andGate(nodes[v - 1])
        check(id2index(gate.id) == v)
        iffAnd(
            nodeValue[v],
            nodeValue[id2index(gate.left.id)] sign !gate.left.negated,
            nodeValue[id2index(gate.right.id)] sign !gate.right.negated,
        )
    }

    log.info("Declared $numberOfVariables variables and $numberOfClauses clauses")
}

private fun Iterable<Boolean>.toBinaryString(): String =
    joinToString("") { it.toInt().toString() }

fun main() {
    val timeStart = timeNow()

    //region [samples]

    // val filename = "data/instances/examples/aag/and.aag"
    // val filename = "data/instances/examples/aag/halfadder.aag"
    // val filename = "data/instances/manual/aag/eq.aag" // 1s, 2+0
    // val filename = "data/instances/manual/aag/eq2.aag" // 1s, 3+0
    // val filename = "data/instances/BubbleSort/aag/BubbleSort_4_3.aag" // 2s, 36+0
    // val filename = "data/instances/BubbleSort/aag/BubbleSort_5_4.aag" // 17s, 180+0
    // val filename = "data/instances/BubbleSort/aag/BubbleSort_6_4.aag" // 52s, 270+0
    // val filename = "data/instances/BubbleSort/aag/BubbleSort_7_4.aag" // 130s, 378+0

    //endregion

    // region [instances with B]

    // val filename = "data/instances/ISCAS/aag/prolog.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1196.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1238.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1269.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s13207.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s13207.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1423.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1488.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1494.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s1512.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s15850.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s15850.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s208.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s27.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s298.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s3271.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s3330.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s3384.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s344.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s349.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s35932.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s382.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s38417.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s38584.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s38584.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s386.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s400.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s420.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s444.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s4863.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s499.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s510.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s526.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s526n.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s5378.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s635.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s641.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s6669.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s713.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s820.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s832.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s838.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s9234.1.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s9234.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s938.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s953.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s967.aag" // with B
    // val filename = "data/instances/ISCAS/aag/s991.aag" // with B

    // endregion

    //region [invalid instances]

    // val filename = "data/instances/ISCAS/aag/c2670.aag" // with 0 output

    //endregion

    //region [valid ISCAS instances]

    // val filename = "data/instances/ISCAS/aag/c17.aag" // 1s, 0+0
    val filename = "data/instances/ISCAS/aag/c432.aag" // 3s, 110+0
    // val filename = "data/instances/ISCAS/aag/c499.aag" // 5s, 0+0
    // val filename = "data/instances/ISCAS/aag/c880.aag" // 3s, 0+0
    // val filename = "data/instances/ISCAS/aag/c1355.aag" // 8s, 0+0
    // val filename = "data/instances/ISCAS/aag/c1908.aag" // 5s, 1+1
    // val filename = "data/instances/ISCAS/aag/c3540.aag" // [long] 116s, 4+3
    // val filename = "data/instances/ISCAS/aag/c5315.aag" // [long] 256s, 21+0
    // val filename = "data/instances/ISCAS/aag/c6288.aag" // [long] 1992s, 0+2
    // val filename = "data/instances/ISCAS/aag/c7552.aag" // [long] ?, ?+?

    //endregion

    //region [EPFL instances]

    // val filename = "data/instances/EPFL/arithmetic/adder/aag/adder.aag" // 9s, 0+0
    // val filename = "data/instances/EPFL/arithmetic/bar/aag/bar.aag" // ~10ks, 0+0
    // val filename = "data/instances/EPFL/arithmetic/divisor/aag/div.aag" // ?
    // val filename = "data/instances/EPFL/arithmetic/hypotenuse/aag/hyp.aag" // ?
    // val filename = "data/instances/EPFL/arithmetic/log2/aag/log2.aag" // ?
    // val filename = "data/instances/EPFL/arithmetic/max/aag/max.aag" // ?
    // val filename = "data/instances/EPFL/arithmetic/multiplier/aag/multiplier.aag" // ?
    // val filename = "data/instances/EPFL/arithmetic/sin/aag/sin.aag" // ?
    // val filename = "data/instances/EPFL/arithmetic/sqrt/aag/sqrt.aag" // ?
    // val filename = "data/instances/EPFL/arithmetic/square/aag/square.aag" // ?

    //endregion

    val aig = parseAig(filename)
    log.info("aig = $aig")

    // for ((i, layer) in aig.layers().withIndex()) {
    //     log.info("Layer #${i + 1} (${layer.size} nodes): $layer")
    // }

    // CryptoMiniSatSolver().useWith {
    // GlucoseSolver().useWith {
    MiniSatSolver().useWith {
        // CadicalSolver().useWith {
        declareBase(aig)

        val nodes: List<Int> = context["nodes"]
        val V: Int = context["V"]
        val X: Int = context["X"]
        val Y: Int = context["Y"]
        val nodeValueVar: BoolVarArray = context["nodeValue"]

        fun id2index(id: Int) = nodes.indexOf(id) + 1

        // Freeze gates (because we use them in assumptions later)
        for (v in (X + 1)..V) {
            backend.freeze(nodeValueVar[v])
        }

        // Mark gate values variables as non-decision
        for (v in (X + 1)..V) {
            backend.setDecision(nodeValueVar[v], false)
        }

        val (isSat, timeSolve) = measureTimeWithResult { solve() }
        println("${if (isSat) "SAT" else "UNSAT"} in %.3fs".format(timeSolve.seconds))

        if (isSat) {
            val model = getModel()
            val nodeValue = context.convertBoolVarArray("nodeValue", model)

            // println("model = ${model.data.toBinaryString()}")
            // println("nodeValue = ${nodeValue.values.toBinaryString()}")

            println("Searching for equivalent gates...")
            val equivalentPairs: MutableList<Pair<Int, Int>> = mutableListOf()
            val antiEquivalentPairs: MutableList<Pair<Int, Int>> = mutableListOf()

            for (v1 in (X + 1)..V) {
                val timeStartFirst = timeNow()
                if (backend.isEliminated(nodeValueVar[v1])) {
                    error("nodeValue[v1=$v1]=${nodeValueVar[v1]} (gate ${aig.node(nodes[v1 - 1])}) is eliminated")
                }
                for (v2 in (v1 + 1)..V) {
                    val (table, timeSolveTable) = measureTimeWithResult {
                        listOf(
                            Pair(false, false),
                            Pair(false, true),
                            Pair(true, false),
                            Pair(true, true)
                        ).map { (x1, x2) ->
                            val (isSubSat, timeSubSolve) = measureTimeWithResult {
                                solve(
                                    assumptions = listOf(
                                        nodeValueVar[v1] sign x1,
                                        nodeValueVar[v2] sign x2,
                                    )
                                )
                            }
                            // println("${if (isSubSat) "SAT" else "UNSAT"} assuming ($x1, $x2) in %.3fs".format(timeSubSolve.seconds))
                            isSubSat
                        }
                    }
                    // println("v=($v1,$v2) :: ${table.toBinaryString()}")
                    if (table.toBinaryString() == "1001") {
                        println(" -- Found equivalent gates: $v1 and $v2 in %.3f ms".format(timeSolveTable.milliseconds))
                        equivalentPairs.add(Pair(v1, v2))
                    }
                    if (table.toBinaryString() == "0110") {
                        println(" -- Found anti-equivalent gates: $v1 and $v2 in %.3f ms".format(timeSolveTable.milliseconds))
                        antiEquivalentPairs.add(Pair(v1, v2))
                    }
                }

                // Unfreeze variable `v1` that we won't use anymore
                backend.setFrozen(nodeValueVar[v1], false)

                println(
                    "$v1/$V done in %.3fs after %.3fs".format(
                        secondsSince(timeStartFirst),
                        secondsSince(timeStart)
                    )
                )
            }
            println("Done searching for equivalent gates")
            println("Total equivalent pairs (EQ+XOR): ${equivalentPairs.size} + ${antiEquivalentPairs.size} = ${equivalentPairs.size + antiEquivalentPairs.size} of total ${aig.ands.size} ANDs")
        } else {
            error("Unexpected UNSAT")
        }
    }

    log.info("All done in %.3f s".format(secondsSince(timeStart)))
}
