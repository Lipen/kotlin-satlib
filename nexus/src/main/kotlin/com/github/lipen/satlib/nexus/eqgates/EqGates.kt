@file:Suppress("LocalVariableName", "FunctionName", "DuplicatedCode")

package com.github.lipen.satlib.nexus.eqgates

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.encoding.encodeAig1
import com.github.lipen.satlib.nexus.utils.declare
import com.github.lipen.satlib.nexus.utils.iffXor2
import com.github.lipen.satlib.nexus.utils.maybeFreeze
import com.github.lipen.satlib.nexus.utils.maybeMelt
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.op.iffIff
import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.solve
import com.github.lipen.satlib.utils.useWith
import com.soywiz.klock.measureTimeWithResult
import mu.KotlinLogging
import kotlin.io.path.Path
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

internal fun Solver.`check gates equivalence using conjugated table`(
    lit1: Lit,
    lit2: Lit,
): Boolean {
    // EQ table:
    //   00 SAT
    //   01 UNSAT
    //   10 UNSAT
    //   11 SAT

    // // solve(0x) -> SAT
    // if (!solve(-lit1)) return false
    // // solve(1x) -> SAT
    // if (!solve(lit1)) return false
    // // solve(x0) -> SAT
    // if (!solve(-lit2)) return false
    // // solve(x1) -> SAT
    // if (!solve(lit2)) return false

    // solve(00) -> SAT
    if (!solve(-lit1, -lit2)) return false

    // solve(01) -> UNSAT
    if (solve(-lit1, lit2)) {
        return false
    } else {
        addClause(lit1, -lit2)
    }

    // solve(10) -> UNSAT
    if (solve(lit1, -lit2)) {
        return false
    } else {
        addClause(-lit1, lit2)
    }

    // solve(11) -> SAT
    if (!solve(lit1, lit2)) return false

    return true
}

internal fun Solver.`check gates equivalence using merger`(
    lit1: Lit,
    lit2: Lit,
    type: String, // EQ or XOR
): Boolean {
    val merger = newLiteral()

    // Encode the merger
    when (type) {
        "EQ" -> {
            comment("EQ merger semantics")
            iffIff(merger, lit1, lit2)
        }
        "XOR" -> {
            comment("XOR merger semantics")
            iffXor2(merger, lit1, lit2)
        }
        else -> error("Bad type '$type'")
    }

    // Freeze the merger variable
    maybeFreeze(merger)

    val isEq = when (type) {
        "EQ" -> {
            // Assume not-EQ -> expect UNSAT
            !solve(-merger)
        }
        "XOR" -> {
            // Assume XOR -> expect UNSAT
            !solve(merger)
        }
        else -> error("Bad type '$type'")
    }

    // Unfreeze the merger variable
    maybeMelt(merger)

    return isEq
}

fun searchEqGates(
    aig: Aig,
    solverProvider: () -> Solver,
    method: String, // "conj", "merge-eq", "merge-xor"
): List<Pair<Int, Int>> {
    logger.info("Searching for equivalent gates in $aig")
    val timeStart = timeNow()

    logger.info("Random sampling in order to pre-determine non-equivalent gates...")
    val nonEqGates: MutableSet<Pair<Int, Int>> = mutableSetOf()
    val random = Random(42)
    repeat(32) {
        val inputValues = List(aig.inputs.size) { random.nextBoolean() }
        val nodeValue = aig.eval(inputValues)
        for ((i, id1) in aig.andGateIds.withIndex()) {
            for ((j, id2) in aig.andGateIds.withIndex()) {
                if (i >= j) continue
                if (nodeValue[id1] != nodeValue[id2]) {
                    nonEqGates.add(Pair(i + 1, j + 1))
                }
            }
        }
        // println("[%.3fs] iter $it: nonEqGates.size=${nonEqGates.size}".format(secondsSince(timeStart)))
    }
    logger.info("Random sampling done in %.3fs".format(secondsSince(timeStart)))
    logger.info("Total non-eq gate pairs found: ${nonEqGates.size} of ${aig.andGates.size * (aig.andGates.size - 1) / 2}, pairs left to check: ${aig.andGates.size * (aig.andGates.size - 1) / 2 - nonEqGates.size}")

    solverProvider().useWith {
        logger.info("Using $this")

        declare(logger) {
            encodeAig1(aig)
        }

        val G: Int = context["G"]
        val andGateValueVar: BoolVarArray = context["andGateValue"]

        // Freeze gates (because we use them in assumptions later)
        for (g in 1..G) {
            maybeFreeze(andGateValueVar[g])
        }
        // // Mark gate values variables as non-decision
        // for (g in 1..G) {
        //     maybeSetDecision(andGateValueVar[g], false)
        // }

        val (isSat, timeSolve) = measureTimeWithResult { solve() }
        logger.info { "${if (isSat) "SAT" else "UNSAT"} for template CNF in %.3fs".format(timeSolve.seconds) }
        if (!isSat) error("Unexpected UNSAT")

        logger.info("Searching for equivalent gates using method '$method'...")
        val equivalentPairs: MutableList<Pair<Int, Int>> = mutableListOf()
        val timeEqCheckStart = timeNow()
        for (g1 in 1..G) {
            val lit1 = andGateValueVar[g1]
            for (g2 in (g1 + 1)..G) {
                if (Pair(g1, g2) !in nonEqGates) {
                    val lit2 = andGateValueVar[g2]
                    val (isEq, timeEq) = measureTimeWithResult {
                        when (method) {
                            "conj" -> {
                                `check gates equivalence using conjugated table`(lit1, lit2)
                            }
                            "merge-eq" -> {
                                `check gates equivalence using merger`(lit1, lit2, "EQ")
                            }
                            "merge-xor" -> {
                                `check gates equivalence using merger`(lit1, lit2, "XOR")
                            }
                            else -> {
                                error("Bad method '$method'")
                            }
                        }
                    }
                    if (isEq) {
                        logger.debug {
                            "[%.3fs] Found equivalent gates: $g1 and $g2 in %.3fs"
                                .format(secondsSince(timeStart), timeEq.seconds)
                        }
                        equivalentPairs.add(Pair(g1, g2))
                    }
                }
            }

            // Unfreeze the variables that we won't use anymore
            maybeMelt(lit1)
        }
        logger.info { "Total eq-check time: %.3fs".format(secondsSince(timeEqCheckStart)) }

        logger.info("Done searching for equivalent gates")
        logger.info("Total equivalent pairs: ${equivalentPairs.size} of total ${aig.andGates.size} ANDs")
        return equivalentPairs
    }
}

fun main() {
    val timeStart = timeNow()

    //region [Timings]

    // | Instance | ANDs | sample |      nonEQ |   EQ | SamTime | SolTime | TotTime |
    // |   BS_4_3 |  210 |     32 |     21`887 |   36 |    0.2s |    10ms |    0.8s |
    // |   BS_7_4 | 1155 |     32 |    665`360 |  378 |    5.1s |    0.7s |    6.9s |
    // |   BS_7_4 | 1155 |     64 |    665`925 |  378 |   11.3s |    0.2s |   12.8s |
    // |   BS_8_4 | 1540 |     32 |  1`182`771 |  504 |   12.9s |    2.2s |   16.8s |
    // |   BS_9_4 | 1980 |     32 |  1`953`408 |  648 |   27.9s |    7.3s |   37.9s |
    // |          |      |        |            |      |         |         |         |

    //endregion

    //region [samples]

    // val filename = "data/instances/examples/aag/and.aag"
    // val filename = "data/instances/examples/aag/halfadder.aag"
    // val filename = "data/instances/manual/aag/eq.aag" // 1s, 2+0
    // val filename = "data/instances/manual/aag/eq2.aag" // 1s, 3+0
    // val filename = "data/instances/BubbleSort/aag/BubbleSort_4_3.aag" // 2s, 36+0
    // val filename = "data/instances/BubbleSort/aag/BubbleSort_5_4.aag" // 17s, 180+0
    // val filename = "data/instances/BubbleSort/aag/BubbleSort_6_4.aag" // 52s, 270+0
    // val filename = "data/instances/BubbleSort/aag/BubbleSort_7_4.aag" // 130s, 378+0
    // val filename = "data/instances/BubbleSort/aag/BubbleSort_8_4.aag"
    // val filename = "data/instances/BubbleSort/aag/BubbleSort_9_4.aag"

    // FRAIGs
    val filename = "data/instances/BubbleSort/fraag/BubbleSort_4_3.aag"
    // val filename = "data/instances/BubbleSort/fraag/BubbleSort_7_4.aag"
    // val filename = "data/instances/BubbleSort/fraag/BubbleSort_8_4.aag"

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
    // val filename = "data/instances/ISCAS/aag/c3540.aag" // 116s [tot:6s,sam:4s,sol:1.3s], 4+3 [4]
    // val filename = "data/instances/ISCAS/aag/c5315.aag" // 256s [tot:25s,sam:23s,sol:0.8s], 21+0 [23]
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

    val solverProvider = {
        MiniSatSolver()
        // GlucoseSolver()
        // CryptoMiniSatSolver()
        // CadicalSolver()
    }

    // Methods: "conj", "merge-eq", "merge-xor"
    val method = "merge-eq"

    val aig = parseAig(Path(filename))
    searchEqGates(aig, solverProvider, method)

    logger.info("All done in %.3fs".format(secondsSince(timeStart)))
}
