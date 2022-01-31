@file:Suppress("LocalVariableName")

package com.github.lipen.satlib.nexus.eqgates

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.convertBoolVarArray
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.utils.maybeFreeze
import com.github.lipen.satlib.nexus.utils.maybeSetDecision
import com.github.lipen.satlib.nexus.utils.maybeSetFrozen
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.op.iffAnd
import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.solve
import com.github.lipen.satlib.utils.useWith
import com.soywiz.klock.measureTimeWithResult
import mu.KotlinLogging
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

private fun Solver.declareReduction(aig: Aig) {
    val timeStart = timeNow()

    // Constants
    context["aig"] = aig
    val nodes = context("nodes") { aig.layers().flatten().toList() }
    fun id2index(id: Int) = nodes.indexOf(id) + 1
    val V = context("V") { nodes.size }
    val X = context("X") { aig.inputs.size }
    val Y = context("Y") { aig.outputs.size }

    logger.info("V = $V, X = $Y, Y = $Y")

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

    logger.info(
        "Declared $numberOfVariables variables and $numberOfClauses clauses in %.3f s"
            .format(secondsSince(timeStart))
    )
}

private fun Iterable<Boolean>.toBinaryString(): String =
    joinToString("") { if (it) "1" else "0" }

fun searchEqGates(aig: Aig, solverProvider: () -> Solver): List<Pair<Int, Int>> {
    logger.info("Searching for equivalent gates in $aig")
    val timeStart = timeNow()

    solverProvider().useWith {
        declareReduction(aig)

        val nodes: List<Int> = context["nodes"]
        val V: Int = context["V"]
        val X: Int = context["X"]
        val Y: Int = context["Y"]
        val nodeValueVar: BoolVarArray = context["nodeValue"]

        fun id2index(id: Int) = nodes.indexOf(id) + 1

        // Freeze gates (because we use them in assumptions later)
        for (v in (X + 1)..V) {
            maybeFreeze(nodeValueVar[v])
        }

        // Mark gate values variables as non-decision
        for (v in (X + 1)..V) {
            maybeSetDecision(nodeValueVar[v], false)
        }

        val (isSat, timeSolve) = measureTimeWithResult { solve() }
        logger.info("${if (isSat) "SAT" else "UNSAT"} for template CNF in %.3fs".format(timeSolve.seconds))

        if (isSat) {
            val model = getModel()
            val nodeValue = context.convertBoolVarArray("nodeValue", model)

            // println("model = ${model.data.toBinaryString()}")
            // println("nodeValue = ${nodeValue.values.toBinaryString()}")

            println("Searching for equivalent gates...")
            val equivalentPairs: MutableList<Pair<Int, Int>> = mutableListOf()
            val antiEquivalentPairs: MutableList<Pair<Int, Int>> = mutableListOf()

            for (v1 in (X + 1)..V) {
                val timeStartV1 = timeNow()
                // if (backend.isEliminated(nodeValueVar[v1])) {
                //     error("nodeValue[v1=$v1]=${nodeValueVar[v1]} (gate ${aig.node(nodes[v1 - 1])}) is eliminated")
                // }
                for (v2 in (v1 + 1)..V) {
                    val (table, timeSolveTable) = measureTimeWithResult {
                        listOf(
                            Pair(false, false),
                            Pair(false, true),
                            Pair(true, false),
                            Pair(true, true)
                        ).map { (x1, x2) ->
                            val (isSubSat, timeSubSolve) = measureTimeWithResult {
                                solve(nodeValueVar[v1] sign x1, nodeValueVar[v2] sign x2)
                            }
                            // println("${if (isSubSat) "SAT" else "UNSAT"} assuming ($x1, $x2) in %.3fs".format(timeSubSolve.seconds))
                            isSubSat
                        }
                    }
                    if (table.toBinaryString() == "1001") {
                        println(
                            "[%.3fs] Found equivalent gates: $v1 and $v2 in %.3f ms"
                                .format(secondsSince(timeStart), timeSolveTable.milliseconds)
                        )
                        equivalentPairs.add(Pair(v1, v2))
                    }
                    if (table.toBinaryString() == "0110") {
                        println(
                            "[%.3fs] Found anti-equivalent gates: $v1 and $v2 in %.3f ms"
                                .format(secondsSince(timeStart), timeSolveTable.milliseconds)
                        )
                        antiEquivalentPairs.add(Pair(v1, v2))
                    }
                }

                // Unfreeze variable `v1` that we won't use anymore
                maybeSetFrozen(nodeValueVar[v1], false)

                println(
                    "[%.3fs] $v1/$V done in %.3fs".format(
                        secondsSince(timeStart),
                        secondsSince(timeStartV1)
                    )
                )
            }

            println("Done searching for equivalent gates")
            println("Total equivalent pairs (EQ+XOR): ${equivalentPairs.size} + ${antiEquivalentPairs.size} = ${equivalentPairs.size + antiEquivalentPairs.size} of total ${aig.andGates.size} ANDs")

            return equivalentPairs
        } else {
            error("Unexpected UNSAT")
        }
    }
}

fun main() {
    val timeStart = timeNow()

    //region [samples]

    // val filename = "data/instances/examples/aag/and.aag"
    // val filename = "data/instances/examples/aag/halfadder.aag"
    // val filename = "data/instances/manual/aag/eq.aag" // 1s, 2+0
    val filename = "data/instances/manual/aag/eq2.aag" // 1s, 3+0
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
    // val filename = "data/instances/ISCAS/aag/c432.aag" // 3s, 110+0
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

    //region [IWLS93 instances]

    // val filename = "data/instances/IWLS93/aag/5xp1.aag" // 3s, 0+0
    // val filename = "data/instances/IWLS93/aag/9sym.aag" // 9s, 0+0
    // val filename = "data/instances/IWLS93/aag/9symml.aag"
    // val filename = "data/instances/IWLS93/aag/alu2.aag" // 120s, 100+8
    // val filename = "data/instances/IWLS93/aag/alu4.aag" // [large]
    // val filename = "data/instances/IWLS93/aag/apex1.aag" // with B
    // val filename = "data/instances/IWLS93/aag/apex2.aag"
    // val filename = "data/instances/IWLS93/aag/apex3.aag"
    // val filename = "data/instances/IWLS93/aag/apex4.aag"
    // val filename = "data/instances/IWLS93/aag/apex5.aag"
    // val filename = "data/instances/IWLS93/aag/apex6.aag"
    // val filename = "data/instances/IWLS93/aag/apex7.aag"
    // val filename = "data/instances/IWLS93/aag/b1.aag" // 1s, 3+0
    // val filename = "data/instances/IWLS93/aag/b9.aag" // 3s, 3+1
    // val filename = "data/instances/IWLS93/aag/b12.aag" // 1217s, 96+75
    // val filename = "data/instances/IWLS93/aag/bigkey.aag" // with B
    // val filename = "data/instances/IWLS93/aag/c8.aag"
    // val filename = "data/instances/IWLS93/aag/C17.aag"
    // val filename = "data/instances/IWLS93/aag/C432.aag"
    // val filename = "data/instances/IWLS93/aag/C499.aag"
    // val filename = "data/instances/IWLS93/aag/C880.aag"
    // val filename = "data/instances/IWLS93/aag/C1355.aag" // 10s, 0+0
    // val filename = "data/instances/IWLS93/aag/C1908.aag"
    // val filename = "data/instances/IWLS93/aag/C2670.aag"
    // val filename = "data/instances/IWLS93/aag/C3540.aag"
    // val filename = "data/instances/IWLS93/aag/C5315.aag"
    // val filename = "data/instances/IWLS93/aag/C6288.aag"
    // val filename = "data/instances/IWLS93/aag/C7552.aag"
    // val filename = "data/instances/IWLS93/aag/cc.aag" // 4s, 1+0
    // val filename = "data/instances/IWLS93/aag/cht.aag"
    // val filename = "data/instances/IWLS93/aag/clip.aag"
    // val filename = "data/instances/IWLS93/aag/clma.aag"
    // val filename = "data/instances/IWLS93/aag/cm138a.aag"
    // val filename = "data/instances/IWLS93/aag/cm150a.aag"
    // val filename = "data/instances/IWLS93/aag/cm151a.aag"
    // val filename = "data/instances/IWLS93/aag/cm152a.aag"
    // val filename = "data/instances/IWLS93/aag/cm162a.aag"
    // val filename = "data/instances/IWLS93/aag/cm163a.aag"
    // val filename = "data/instances/IWLS93/aag/cm42a.aag"
    // val filename = "data/instances/IWLS93/aag/cm82a.aag"
    // val filename = "data/instances/IWLS93/aag/cm85a.aag"
    // val filename = "data/instances/IWLS93/aag/cmb.aag"
    // val filename = "data/instances/IWLS93/aag/comp.aag"
    // val filename = "data/instances/IWLS93/aag/con1.aag"
    // val filename = "data/instances/IWLS93/aag/cordic.aag"
    // val filename = "data/instances/IWLS93/aag/count.aag"
    // val filename = "data/instances/IWLS93/aag/cps.aag"
    // val filename = "data/instances/IWLS93/aag/cu.aag"
    // val filename = "data/instances/IWLS93/aag/daio.aag"
    // val filename = "data/instances/IWLS93/aag/dalu.aag"
    // val filename = "data/instances/IWLS93/aag/decod.aag"
    // val filename = "data/instances/IWLS93/aag/des.aag"
    // val filename = "data/instances/IWLS93/aag/dsip.aag"
    // val filename = "data/instances/IWLS93/aag/duke2.aag"
    // val filename = "data/instances/IWLS93/aag/e64.aag"
    // val filename = "data/instances/IWLS93/aag/ex4p.aag"
    // val filename = "data/instances/IWLS93/aag/ex5p.aag"
    // val filename = "data/instances/IWLS93/aag/example2.aag"
    // val filename = "data/instances/IWLS93/aag/f51m.aag"
    // val filename = "data/instances/IWLS93/aag/frg1.aag"
    // val filename = "data/instances/IWLS93/aag/frg2.aag"
    // val filename = "data/instances/IWLS93/aag/i1.aag"
    // val filename = "data/instances/IWLS93/aag/i10.aag"
    // val filename = "data/instances/IWLS93/aag/i2.aag"
    // val filename = "data/instances/IWLS93/aag/i3.aag"
    // val filename = "data/instances/IWLS93/aag/i4.aag"
    // val filename = "data/instances/IWLS93/aag/i5.aag"
    // val filename = "data/instances/IWLS93/aag/i6.aag"
    // val filename = "data/instances/IWLS93/aag/i7.aag"
    // val filename = "data/instances/IWLS93/aag/i8.aag"
    // val filename = "data/instances/IWLS93/aag/i9.aag"
    // val filename = "data/instances/IWLS93/aag/k2.aag"
    // val filename = "data/instances/IWLS93/aag/lal.aag"
    // val filename = "data/instances/IWLS93/aag/ldd.aag"
    // val filename = "data/instances/IWLS93/aag/majority.aag"
    // val filename = "data/instances/IWLS93/aag/misex1.aag"
    // val filename = "data/instances/IWLS93/aag/misex2.aag"
    // val filename = "data/instances/IWLS93/aag/misex3.aag"
    // val filename = "data/instances/IWLS93/aag/mm30a.aag"
    // val filename = "data/instances/IWLS93/aag/mm4a.aag"
    // val filename = "data/instances/IWLS93/aag/mm9a.aag"
    // val filename = "data/instances/IWLS93/aag/mm9b.aag"
    // val filename = "data/instances/IWLS93/aag/mult16a.aag"
    // val filename = "data/instances/IWLS93/aag/mult16b.aag"
    // val filename = "data/instances/IWLS93/aag/mult32a.aag"
    // val filename = "data/instances/IWLS93/aag/mux.aag"
    // val filename = "data/instances/IWLS93/aag/my_adder.aag"
    // val filename = "data/instances/IWLS93/aag/o64.aag"
    // val filename = "data/instances/IWLS93/aag/pair.aag"
    // val filename = "data/instances/IWLS93/aag/parity.aag"
    // val filename = "data/instances/IWLS93/aag/pcle.aag"
    // val filename = "data/instances/IWLS93/aag/pcler8.aag"
    // val filename = "data/instances/IWLS93/aag/pm1.aag"
    // val filename = "data/instances/IWLS93/aag/rd53.aag"
    // val filename = "data/instances/IWLS93/aag/rd73.aag"
    // val filename = "data/instances/IWLS93/aag/rd84.aag"
    // val filename = "data/instances/IWLS93/aag/rot.aag"
    // val filename = "data/instances/IWLS93/aag/s1196.aag"
    // val filename = "data/instances/IWLS93/aag/s1238.aag"
    // val filename = "data/instances/IWLS93/aag/s1423.aag"
    // val filename = "data/instances/IWLS93/aag/s208.1.aag"
    // val filename = "data/instances/IWLS93/aag/s344.aag"
    // val filename = "data/instances/IWLS93/aag/s349.aag"
    // val filename = "data/instances/IWLS93/aag/s382.aag"
    // val filename = "data/instances/IWLS93/aag/s38417.aag"
    // val filename = "data/instances/IWLS93/aag/s38584.1.aag"
    // val filename = "data/instances/IWLS93/aag/s400.aag"
    // val filename = "data/instances/IWLS93/aag/s420.1.aag"
    // val filename = "data/instances/IWLS93/aag/s444.aag"
    // val filename = "data/instances/IWLS93/aag/s526.aag"
    // val filename = "data/instances/IWLS93/aag/s526n.aag"
    // val filename = "data/instances/IWLS93/aag/s5378.aag"
    // val filename = "data/instances/IWLS93/aag/s641.aag"
    // val filename = "data/instances/IWLS93/aag/s713.aag"
    // val filename = "data/instances/IWLS93/aag/s838.1.aag"
    // val filename = "data/instances/IWLS93/aag/s838.aag"
    // val filename = "data/instances/IWLS93/aag/s9234.1.aag"
    // val filename = "data/instances/IWLS93/aag/s953.aag"
    // val filename = "data/instances/IWLS93/aag/sao2.aag"
    // val filename = "data/instances/IWLS93/aag/sbc.aag"
    // val filename = "data/instances/IWLS93/aag/sct.aag"
    // val filename = "data/instances/IWLS93/aag/seq.aag"
    // val filename = "data/instances/IWLS93/aag/sqrt8.aag"
    // val filename = "data/instances/IWLS93/aag/sqrt8ml.aag"
    // val filename = "data/instances/IWLS93/aag/squar5.aag"
    // val filename = "data/instances/IWLS93/aag/t481.aag"
    // val filename = "data/instances/IWLS93/aag/table3.aag"
    // val filename = "data/instances/IWLS93/aag/table5.aag"
    // val filename = "data/instances/IWLS93/aag/tcon.aag"
    // val filename = "data/instances/IWLS93/aag/term1.aag"
    // val filename = "data/instances/IWLS93/aag/too_large.aag"
    // val filename = "data/instances/IWLS93/aag/ttt2.aag"
    // val filename = "data/instances/IWLS93/aag/unreg.aag"
    // val filename = "data/instances/IWLS93/aag/vda.aag"
    // val filename = "data/instances/IWLS93/aag/vg2.aag"
    // val filename = "data/instances/IWLS93/aag/x1.aag"
    // val filename = "data/instances/IWLS93/aag/x2.aag"
    // val filename = "data/instances/IWLS93/aag/x3.aag"
    // val filename = "data/instances/IWLS93/aag/x4.aag"
    // val filename = "data/instances/IWLS93/aag/xor5.aag"
    // val filename = "data/instances/IWLS93/aag/z4ml.aag"

    //endregion

    val solverProvider = {
        MiniSatSolver()
        // GlucoseSolver()
        // CryptoMiniSatSolver()
        // CadicalSolver()
    }

    val aig = parseAig(Path(filename))
    // for ((i, layer) in aig.layers().withIndex()) {
    //     log.info("Layer #${i + 1} (${layer.size} nodes): $layer")
    // }

    searchEqGates(aig, solverProvider)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
