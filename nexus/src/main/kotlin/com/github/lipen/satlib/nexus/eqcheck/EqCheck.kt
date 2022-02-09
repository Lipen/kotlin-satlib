@file:Suppress("LocalVariableName", "FunctionName", "DuplicatedCode")

package com.github.lipen.satlib.nexus.eqcheck

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.encoding.encodeAigs
import com.github.lipen.satlib.nexus.encoding.encodeMiter
import com.github.lipen.satlib.nexus.encoding.encodeOutputMergers
import com.github.lipen.satlib.nexus.utils.cartesianProduct
import com.github.lipen.satlib.nexus.utils.declare
import com.github.lipen.satlib.nexus.utils.geomean
import com.github.lipen.satlib.nexus.utils.maybeFreeze
import com.github.lipen.satlib.nexus.utils.maybeMelt
import com.github.lipen.satlib.nexus.utils.mean
import com.github.lipen.satlib.nexus.utils.pow
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.nexus.utils.toInt
import com.github.lipen.satlib.solver.GlucoseSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.solve
import com.github.lipen.satlib.utils.useWith
import com.soywiz.klock.measureTimeWithResult
import com.soywiz.klock.milliseconds
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

internal fun Solver.`check circuits equivalence using miter`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using miter...")

    declare(logger) {
        encodeAigs(aigLeft, aigRight)
        encodeMiter()
    }

    logger.info("Solving...")
    val (isSat, timeSolve) = measureTimeWithResult { solve() }
    logger.info("${if (isSat) "SAT" else "UNSAT"} in %.3fs".format(timeSolve.seconds))

    if (!isSat) {
        logger.info("Circuits are equivalent!")
    } else {
        logger.warn("Circuits are NOT equivalent!")
    }
    return !isSat
}

internal fun Solver.`check circuits equivalence using output mergers`(
    aigLeft: Aig,
    aigRight: Aig,
    type: String, // "EQ" or "XOR"
): Boolean {
    logger.info("Checking equivalence using output $type-mergers...")

    declare(logger) {
        encodeAigs(aigLeft, aigRight)
        encodeOutputMergers(type)
    }

    val Y: Int = context["Y"]
    val mergerValue: BoolVarArray = context["mergerValue"]

    // Freeze assumptions
    for (y in 1..Y) {
        maybeFreeze(mergerValue[y])
    }

    logger.info("Pre-solving...")
    val (isSatMain, timeSolveMain) = measureTimeWithResult {
        solve()
    }
    logger.info("${if (isSatMain) "SAT" else "UNSAT"} in %.3fs".format(timeSolveMain.seconds))

    if (!isSatMain) {
        error("Unexpected UNSAT")
    } else {
        for (y in 1..Y) {
            val (isSatSub, timeSolveSub) = measureTimeWithResult {
                when (type) {
                    "EQ" -> {
                        logger.info("Solving assuming eqValue[$y]=false...")
                        solve(-mergerValue[y])
                    }
                    "XOR" -> {
                        logger.info("Solving assuming xorValue[$y]=true...")
                        solve(mergerValue[y])
                    }
                    else -> error("Bad type '$type'")
                }
            }
            logger.info("${if (isSatSub) "SAT" else "UNSAT"} in %.3fs".format(timeSolveSub.seconds))

            if (isSatSub) {
                logger.warn("Circuits are NOT equivalent!")
                return false
            }

            // Un-freeze assumptions
            maybeMelt(mergerValue[y])
        }

        logger.info("Circuits are equivalent!")
        return true
    }
}

internal fun Solver.`check circuits equivalence using conjugated tables`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using conjugated tables...")

    declare(logger) {
        encodeAigs(aigLeft, aigRight)
    }

    val Y: Int = context["Y"]
    val outputValueLeft: BoolVarArray = context["left.outputValue"]
    val outputValueRight: BoolVarArray = context["right.outputValue"]

    // Freeze assumptions
    for (y in 1..Y) {
        maybeFreeze(outputValueLeft[y])
        maybeFreeze(outputValueRight[y])
    }

    logger.info("Pre-solving...")
    val (isSatMain, timeSolveMain) = measureTimeWithResult { solve() }
    logger.info("${if (isSatMain) "SAT" else "UNSAT"} in %.3fs".format(timeSolveMain.seconds))

    if (!isSatMain) {
        error("Unexpected UNSAT")
    } else {
        logger.info("Calculating a conjugated table for each output...")
        for (y in 1..Y) {
            val left = outputValueLeft[y]
            val right = outputValueRight[y]

            logger.info("Calculating conjugated table for y = $y...")
            val timeStartConj = timeNow()
            for ((s1, s2) in listOf(
                false to false,
                false to true,
                true to false,
                true to true,
            )) {
                val (res, timeSolveSub) = measureTimeWithResult {
                    solve(left sign s1, right sign s2)
                }
                logger.debug {
                    "y=$y, solve(${s1.toInt()}${s2.toInt()})=${res.toInt()} in %.3fs".format(timeSolveSub.seconds)
                }
                if (res != (s1 == s2)) {
                    logger.warn("Circuits are NOT equivalent! solve(${s1.toInt()}${s2.toInt()}) = ${res.toInt()}")
                    return false
                }
            }
            logger.info("Determined the equality of output y=$y in %.3fs".format(secondsSince(timeStartConj)))

            // Un-freeze assumptions
            maybeMelt(left)
            maybeMelt(right)
        }

        logger.info("Circuits are equivalent!")
        return true
    }
}

internal fun Solver.`check circuits equivalence using decomposition`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using decomposition...")

    val sampleSize = 10000
    val randomSeed = 42
    val random = Random(randomSeed)
    logger.info("Computing p-tables using sampleSize=$sampleSize and randomSeed=$randomSeed...")
    val tfTableLeft = aigLeft._compute(sampleSize, random)
    val pTableLeft = tfTableLeft.mapValues { (_, tf) ->
        val (t, f) = tf
        t.toDouble() / (t + f)
    }
    val tfTableRight = aigRight._compute(sampleSize, random)
    val pTableRight = tfTableRight.mapValues { (_, tf) ->
        val (t, f) = tf
        t.toDouble() / (t + f)
    }
    val idsSortedByPLeft = aigLeft.mapping.keys.sortedBy { id -> pTableLeft.getValue(id) }
    val idsSortedByPRight = aigRight.mapping.keys.sortedBy { id -> pTableRight.getValue(id) }

    declare(logger) {
        encodeAigs(aigLeft, aigRight)
    }

    val GL: Int = context["left.G"]
    val GR: Int = context["right.G"]
    val andGateValueLeft: BoolVarArray = context["left.andGateValue"]
    val andGateValueRight: BoolVarArray = context["right.andGateValue"]

    // Freeze variables
    for (g in 1..GL) {
        maybeFreeze(andGateValueLeft[g])
    }
    for (g in 1..GR) {
        maybeFreeze(andGateValueRight[g])
    }

    logger.info("Pre-solving...")
    val (isSatMain, timeSolveMain) = measureTimeWithResult { solve() }
    logger.info("${if (isSatMain) "SAT" else "UNSAT"} in %.3fs".format(timeSolveMain.seconds))
    if (!isSatMain) error("Unexpected UNSAT")

    val decLeft = run {
        // val indices= listOf(9)
        val indices = listOf(81)
        // val indices = listOf(66)
        indices.map { i ->
            val layer = aigLeft.layers[i]
            val ps = layer.map { id -> pTableLeft.getValue(id) }
            val meanP = ps.mean()
            val geomeanP = ps.geomean()

            val dises = ps.map { p -> disbalance(p) }
            val meanDis = dises.mean()
            val geomeanDis = dises.geomean()

            val bucket = layer.map { id -> andGateValueLeft[aigLeft.andGateIds.indexOf(id) + 1] }
            val (result, timeEval) = measureTimeWithResult {
                evalBucket(bucket)
            }
            logger.info("(Left) Layer #$i (size=${layer.size}) evaluated in %.3fs".format(timeEval.seconds))
            println("  - ids ${layer.size}: $layer")
            println("  - ps: $ps")
            println("  - dises: $dises")
            println("  - mean/geomean p: %.3f / %.3f".format(meanP, geomeanP))
            println("  - mean/geomean dis: %.3f / %.3f".format(meanDis, geomeanDis))
            println("  - saturation: %.3f%%".format(result.saturation * 100.0))
            println("  - domain: ${result.domain.size} / ${2.pow(result.bucket.size)}")

            result.domain.map { f -> bucketValuation(bucket, f) }
        }
    }

    val decRight = run {
        // val indices = listOf(141)
        // val indices = listOf(394, 373, 372, 402)
        // val indices = listOf(394, /*373,*/ 327)
        // val indices = listOf(402)
        val indices = listOf(394, 373, 372)
        indices.map { i ->
            val layer = aigRight.layers[i]
            val ps = layer.map { id -> pTableRight.getValue(id) }
            val meanP = ps.mean()
            val geomeanP = ps.geomean()

            val dises = ps.map { p -> disbalance(p) }
            val meanDis = dises.mean()
            val geomeanDis = dises.geomean()

            val bucket = layer.map { id -> andGateValueRight[aigRight.andGateIds.indexOf(id) + 1] }
            val (result, timeEval) = measureTimeWithResult {
                evalBucket(bucket)
            }
            logger.info("(Right) Layer #$i (size=${layer.size}) evaluated in %.3fs".format(timeEval.seconds))
            println("  - ids ${layer.size}: $layer")
            println("  - ps: $ps")
            println("  - dises: $dises")
            println("  - mean/geomean p: %.3f / %.3f".format(meanP, geomeanP))
            println("  - mean/geomean dis: %.3f / %.3f".format(meanDis, geomeanDis))
            println("  - saturation: %.3f%%".format(result.saturation * 100.0))
            println("  - domain: ${result.domain.size} / ${2.pow(result.bucket.size)}")

            result.domain.map { f -> bucketValuation(bucket, f) }
        }
    }

    logger.info("Left decomposition size: ${decLeft.size} = ${decLeft.joinToString("+") { it.size.toString() }}")
    logger.info("Right decomposition size: ${decRight.size} = ${decRight.joinToString("+") { it.size.toString() }}")
    // val decomposition: List<List<Lit>> = listOf(decLeft, decRight).cartesianProduct().map { (x1, x2) ->
    //     x1 + x2
    // }.toList()
    val decomposition = (decLeft + decRight).cartesianProduct().map { it.flatten() }.toList() //.shuffled(random)
    // val decomposition = emptyList<List<Int>>()
    logger.info("Total decomposition size: ${decomposition.size}")

    logger.info("Encoding miter...")
    encodeMiter()

    logger.info("Solving all ${decomposition.size} instances in the decomposition...")
    val timeStartSolveAll = timeNow()
    for ((index, assumptions) in decomposition.withIndex()) {
        val (res, timeSolve) = measureTimeWithResult {
            solve(assumptions)
        }
        if (index % 1000 == 0 || timeSolve >= 500.milliseconds) {
            logger.debug {
                "${if (res) "SAT" else "UNSAT"} on $index/${decomposition.size} in %.3fs [total: %.3fs]"
                    .format(timeSolve.seconds, secondsSince(timeStartSolveAll))
            }
        }
        if (res) {
            logger.warn("Circuits are NOT equivalent!")
            return false
        }
    }

    logger.info("Circuits are equivalent!")
    return true
}

fun checkEquivalence(
    aigLeft: Aig,
    aigRight: Aig,
    solverProvider: () -> Solver,
    method: String,
): Boolean {
    logger.info("Preparing to check the equivalence using '$method' method...")
    logger.info("Left circuit: $aigLeft")
    logger.info("Right circuit: $aigRight")

    require(aigLeft.inputs.size == aigRight.inputs.size)
    require(aigLeft.outputs.size == aigRight.outputs.size)

    solverProvider().useWith {
        logger.info("Using $this")

        return when (method) {
            "miter" -> `check circuits equivalence using miter`(aigLeft, aigRight)
            "merge-eq" -> `check circuits equivalence using output mergers`(aigLeft, aigRight, "EQ")
            "merge-xor" -> `check circuits equivalence using output mergers`(aigLeft, aigRight, "XOR")
            "conj" -> `check circuits equivalence using conjugated tables`(aigLeft, aigRight)
            "dec" -> `check circuits equivalence using decomposition`(aigLeft, aigRight)
            else -> TODO("Method '$method'")
        }
    }
}

fun main() {
    val timeStart = timeNow()

    // val filenameLeft = "data/instances/manual/aag/eq.aag"
    // val filenameRight = "data/instances/manual/aag/eq-same.aag"

    val left = "BubbleSort"
    val right = "PancakeSort"
    // Params: 4_3, 5_4, 6_4, 7_4, 10_4, 10_8, 10_16, 20_8
    val param = "7_4"
    val filenameLeft = "data/instances/${left}/aag/${left}_${param}.aag"
    val filenameRight = "data/instances/${right}/aag/${right}_${param}.aag"

    val aigLeft = parseAig(filenameLeft)
    val aigRight = parseAig(filenameRight)
    // val solverProvider = { MiniSatSolver() }
    val solverProvider = { GlucoseSolver() }
    // Methods: "miter", "merge-eq", "merge-xor", "conj", "dec"
    val method = "dec"

    checkEquivalence(aigLeft, aigRight, solverProvider, method)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
