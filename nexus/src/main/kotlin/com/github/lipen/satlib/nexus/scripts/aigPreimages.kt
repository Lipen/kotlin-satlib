package com.github.lipen.satlib.nexus.scripts

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.encoding.encodeAigs
import com.github.lipen.satlib.nexus.eqcheck.disbalance
import com.github.lipen.satlib.nexus.eqcheck.loadPTable
import com.github.lipen.satlib.nexus.utils.declare
import com.github.lipen.satlib.nexus.utils.maybeFreeze
import com.github.lipen.satlib.nexus.utils.pow
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.solver.CadicalSolver
import com.github.lipen.satlib.utils.useWith
import com.soywiz.klock.measureTimeWithResult
import mu.KotlinLogging
import kotlin.math.absoluteValue
import kotlin.math.roundToLong
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * # Results
 *
 * ## BvP_8_4
 *
 * `N=10000`
 *
 * ```
 *   5+5: 98.49% = 9849 / 10000 ~=~ 4230113290 / 2^32
 * 10+10: 94.74% = 9474 / 10000 ~=~ 4069052016 / 2^32
 * 15+15: 88.45% = 8845 / 10000 ~=~ 3798898573 / 2^32
 * 20+20: 83.20% = 8320 / 10000 ~=~ 3573412790 / 2^32
 * 30+30: 69.60% = 6960 / 10000 ~=~ 2989297238 / 2^32
 * 50+50: 38.81% = 3881 / 10000 ~=~ 1666876808 / 2^32
 * 70+70: 19.16% = 1916 / 10000 ~=~ 822915734 / 2^32
 * ```
 *
 * ## BvP_9_4
 *
 * `eps=0.01`, `delta=0.01`, then `N=211933`
 *
 * ```
 *   5+5: 99.19% = 210217 / 211933 ~=~ 68163062105 / 2^36
 * 10+10: 97.29% = 206188 / 211933 ~=~ 66856655024 / 2^36
 * 15+15: 94.39% = 200038 / 211933 ~=~ 64862511677 / 2^36
 * 20+20: 90.76% = 192351 / 211933 ~=~ 62369994619 / 2^36
 * 30+30: 83.02% = 175937 / 211933 ~=~ 57047739514 / 2^36
 * 50+50: 61.49% = 130311 / 211933 ~=~ 42253465637 / 2^36
 * ```
 *
 */

fun main() {
    val timeStartGlobal = timeNow()

    val left = "BubbleSort"
    val right = "PancakeSort"
    val param = "9_4"
    val name = "BvP_$param"

    // val left = "PancakeSort"
    // val right = "SelectionSort"
    // val param = "8_4"
    // val name = "PvS_$param"

    val aag = "fraag" // "aag" or "fraag"
    val nameLeft = "${left}_${param}"
    val nameRight = "${right}_${param}"
    val filenameLeft = "data/instances/$left/$aag/$nameLeft.aag"
    val filenameRight = "data/instances/$right/$aag/$nameRight.aag"

    val aigLeft = parseAig(filenameLeft)
    val aigRight = parseAig(filenameRight)
    val solverProvider = { CadicalSolver() }

    loadPTable(aigLeft, "data/instances/$left/ptable/${nameLeft}_$aag.json")
    loadPTable(aigRight, "data/instances/$right/ptable/${nameRight}_$aag.json")

    val sampleSize = 10000
    val randomSeed = 42
    logger.info("Computing p-tables using sampleSize=$sampleSize and randomSeed=$randomSeed...")
    val pTableLeft = aigLeft.computePTable(sampleSize, Random(randomSeed))
    val pTableRight = aigRight.computePTable(sampleSize, Random(randomSeed + 1_000_000))
    val idsSortedByPLeft = aigLeft.mapping.keys.sortedBy { id -> pTableLeft.getValue(id) }
    val idsSortedByPRight = aigRight.mapping.keys.sortedBy { id -> pTableRight.getValue(id) }
    val idsSortedByDisLeft = aigLeft.mapping.keys.sortedBy { id -> -disbalance(pTableLeft.getValue(id)) }
    val idsSortedByDisRight = aigRight.mapping.keys.sortedBy { id -> -disbalance(pTableRight.getValue(id)) }

    val mostDisbalancedGatesLeft = idsSortedByDisLeft.take(50)
    val mostDisbalancedGatesRight = idsSortedByDisRight.take(50)

    logger.info(
        "Most disbalanced gates in left: ${mostDisbalancedGatesLeft.associateWith { id -> pTableLeft.getValue(id) }}"
    )
    logger.info(
        "Most disbalanced gates in right: ${mostDisbalancedGatesRight.associateWith { id -> pTableRight.getValue(id) }}"
    )

    solverProvider().useWith {
        declare(logger) {
            encodeAigs(aigLeft, aigRight)
        }

        val X: Int = context["X"]
        val GL: Int = context["left.G"]
        val GR: Int = context["right.G"]
        val andGateValueLeft: BoolVarArray = context["left.andGateValue"]
        val andGateValueRight: BoolVarArray = context["right.andGateValue"]
        val inputValue: BoolVarArray = context["inputValue"]

        // Freeze variables
        for (x in 1..X) {
            maybeFreeze(inputValue[x])
        }
        for (g in 1..GL) {
            maybeFreeze(andGateValueLeft[g])
        }
        for (g in 1..GR) {
            maybeFreeze(andGateValueRight[g])
        }

        val cubeLeft = mostDisbalancedGatesLeft.map { id ->
            andGateValueLeft[aigLeft.andGateIds.indexOf(id) + 1] sign (pTableLeft.getValue(id) > 0.5)
        }
        val cubeRight = mostDisbalancedGatesRight.map { id ->
            andGateValueRight[aigRight.andGateIds.indexOf(id) + 1] sign (pTableRight.getValue(id) > 0.5)
        }
        val cube: List<Lit> = cubeLeft + cubeRight
        val cubeValues = cube.map { it > 0 }

        logger.info("Cube left: $cubeLeft")
        logger.info("Cube right: $cubeRight")
        logger.info("Cube: $cube")

        val totalIterations = 211933
        val random = Random(42)
        var gotCube = 0
        val timeStartSolve = timeNow()

        for (i in 1..totalIterations) {
            val assumptions = (1..X).map { x ->
                inputValue[x] sign random.nextBoolean()
            }
            val (res, timeSolve) = measureTimeWithResult {
                solve(assumptions)
            }
            check(res)
            if (i % 1000 == 0) {
                logger.debug(
                    "Iteration $i/$totalIterations done in %.3fs [total: %.3fs]  Trials: $gotCube / $totalIterations"
                        .format(timeSolve.seconds, secondsSince(timeStartSolve))
                )
            }
            val cubeModel = cube.map { it.absoluteValue }.map { getValue(it) }
            var ok = true
            for (j in cube.indices) {
                if (cubeValues[j] != cubeModel[j]) {
                    // logger.debug {
                    //     "Mismatch on j=$j: ${cubeModel.toBinaryString()} != ${cubeValues.toBinaryString()}"
                    // }
                    ok = false
                    break
                }
            }
            if (ok) {
                gotCube += 1
            }
        }
        val p = gotCube.toDouble() / totalIterations.toDouble()
        logger.info("Trials: %.2f%% = $gotCube / $totalIterations ~=~ ${(p * 2L.pow(X)).roundToLong()} / 2^$X".format(p * 100.0))

        // logger.info("Encoding miter")
        // encodeMiter()
        // // logger.info("Adding cube $cube")
        // // for (lit in cube) {
        // //     addClause(lit)
        // // }
        // // logger.info("Solving miter with cube...")
        // val decisionsStart = maybeNumberOfDecisions()
        // val propagationsStart = maybeNumberOfPropagations()
        // val conflictsStart = maybeNumberOfConflicts()
        // val (res, timeSolve) = measureTimeWithResult {
        //     solve()
        // }
        // val decisions = maybeNumberOfDecisions() - decisionsStart
        // val propagations = maybeNumberOfPropagations() - propagationsStart
        // val conflicts = maybeNumberOfConflicts() - conflictsStart
        // logger.info("${if (res) "SAT" else "UNSAT"} in %.3fs".format(timeSolve.seconds))
        // logger.debug { "decisions: $decisions" }
        // logger.debug { "propagations: $propagations" }
        // logger.debug { "conflicts: $conflicts" }
    }

    println("All done in %.3fs".format(secondsSince(timeStartGlobal)))
}
