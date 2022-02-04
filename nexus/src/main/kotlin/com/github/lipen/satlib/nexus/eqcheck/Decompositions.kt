@file:Suppress("LocalVariableName", "DuplicatedCode")

package com.github.lipen.satlib.nexus.eqcheck

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.encoding.encodeAig1
import com.github.lipen.satlib.nexus.utils.declare
import com.github.lipen.satlib.nexus.utils.geomean
import com.github.lipen.satlib.nexus.utils.maybeFreeze
import com.github.lipen.satlib.nexus.utils.mean
import com.github.lipen.satlib.nexus.utils.pow
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.solver.CadicalSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.useWith
import com.soywiz.klock.measureTimeWithResult
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

internal fun disbalance(p: Double, midpoint: Double = 0.25): Double {
    return if (p <= midpoint) (midpoint - p) / midpoint
    else (p - midpoint) / (1 - midpoint)
}

internal fun Solver.determineDecomposition1(
    aig: Aig,
): List<List<Lit>> {
    declare(logger) {
        encodeAig1(aig)
    }

    val G: Int = context["G"]
    val andGateValue: BoolVarArray = context["andGateValue"]

    val sampleSize = 10000
    val randomSeed = 42
    val random = Random(randomSeed)
    logger.info("Computing p-table using sampleSize=$sampleSize and randomSeed=$randomSeed...")
    val tfTable = aig._compute(sampleSize, random)
    val pTable = tfTable.mapValues { (_, tf) ->
        val (t, f) = tf
        t.toDouble() / (t + f)
    }
    // println("Sorted p-table: ${pTable.toSortedMap { a, b -> pTable.getValue(a).compareTo(pTable.getValue(b)) }}")
    val idsSortedByDisbalance = aig.mapping.keys.sortedBy { id -> pTable.getValue(id) }

    // Freeze gates (because we use them in assumptions later)
    for (g in 1..G) {
        maybeFreeze(andGateValue[g])
    }

    val (isSat, timeSolve) = measureTimeWithResult { solve() }
    logger.info { "${if (isSat) "SAT" else "UNSAT"} for template CNF in %.3fs".format(timeSolve.seconds) }
    if (!isSat) error("Unexpected UNSAT")

    for ((i, layer) in aig.layers.withIndex()) {
        // 0-th layer contains only inputs
        if (i == 0) continue

        if (layer.size > 20) {
            logger.info("Layer #$i (size=${layer.size}) is too big")
        } else if (layer.size < 6) {
            logger.info("Layer #$i (size=${layer.size}) is too small")
        } else {
            val ps = layer.map { id -> pTable.getValue(id) }
            val meanP = ps.mean()
            val geomeanP = ps.geomean()

            val dises = ps.map { p -> disbalance(p) }
            val meanDis = dises.mean()
            val geomeanDis = dises.geomean()

            val bucket = layer.map { id -> andGateValue[aig.andGateIds.indexOf(id) + 1] }
            val (result, timeEval) = measureTimeWithResult {
                evalBucket(bucket)
            }
            logger.info("Layer #$i (size=${layer.size}) evaluated in %.3fs".format(timeEval.seconds))
            println("  - ids ${layer.size}: $layer")
            println("  - ps: $ps")
            println("  - dises: $dises")
            println("  - mean/geomean p: %.3f / %.3f".format(meanP, geomeanP))
            println("  - mean/geomean dis: %.3f / %.3f".format(meanDis, geomeanDis))
            println("  - saturation: %.3f%%".format(result.saturation * 100.0))
            println("  - domain: ${result.domain.size} / ${2.pow(result.bucket.size)}")
        }

        // for (id in layer) {
        //     maybeMelt(andGateValueVar[aig.andGateIds.indexOf(id) + 1])
        // }
    }

    run {
        val k = 14
        val lowAsymm = idsSortedByDisbalance.take(k)

        val ps = lowAsymm.map { id -> pTable.getValue(id) }
        val meanP = ps.mean()
        val geomeanP = ps.geomean()

        val dises = ps.map { p -> disbalance(p) }
        val meanDis = dises.mean()
        val geomeanDis = dises.geomean()

        val bucket = lowAsymm.map { id -> andGateValue[aig.andGateIds.indexOf(id) + 1] }
        val (result, timeEval) = measureTimeWithResult {
            evalBucket(bucket)
        }
        logger.info("Lowest k=$k asymm gates evaluated in %.3fs".format(timeEval.seconds))
        println("  - ids (${lowAsymm.size}): $lowAsymm")
        println("  - ps: $ps")
        println("  - dises: $dises")
        println("  - mean/geomean p: %.3f / %.3f".format(meanP, geomeanP))
        println("  - mean/geomean dis: %.3f / %.3f".format(meanDis, geomeanDis))
        println("  - saturation: %.3f%%".format(result.saturation * 100.0))
        println("  - domain: ${result.domain.size} / ${2.pow(result.bucket.size)}")
    }

    run {
        val k = 14
        val highAsymm = idsSortedByDisbalance.takeLast(k)

        val ps = highAsymm.map { id -> pTable.getValue(id) }
        val meanP = ps.mean()
        val geomeanP = ps.geomean()

        val dises = ps.map { p -> disbalance(p) }
        val meanDis = dises.mean()
        val geomeanDis = dises.geomean()

        val bucket = highAsymm.map { id -> andGateValue[aig.andGateIds.indexOf(id) + 1] }
        val (result, timeEval) = measureTimeWithResult {
            evalBucket(bucket)
        }
        logger.info("Highest k=$k asymm gates evaluated in %.3fs".format(timeEval.seconds))
        println("  - ids (${highAsymm.size}): $highAsymm")
        println("  - ps: $ps")
        println("  - dises: $dises")
        println("  - mean/geomean p: %.3f / %.3f".format(meanP, geomeanP))
        println("  - mean/geomean dis: %.3f / %.3f".format(meanDis, geomeanDis))
        println("  - saturation: %.3f%%".format(result.saturation * 100.0))
        println("  - domain: ${result.domain.size} / ${2.pow(result.bucket.size)}")
    }

    run {
        val k = 14
        val topAsymm = aig.mapping.keys.sortedBy { id ->
            disbalance(pTable.getValue(id))
        }.takeLast(k)

        val ps = topAsymm.map { id -> pTable.getValue(id) }
        val meanP = ps.mean()
        val geomeanP = ps.geomean()

        val dises = ps.map { p -> disbalance(p) }
        val meanDis = dises.mean()
        val geomeanDis = dises.geomean()

        val bucket = topAsymm.map { id -> andGateValue[aig.andGateIds.indexOf(id) + 1] }
        val (result, timeEval) = measureTimeWithResult {
            evalBucket(bucket)
        }
        logger.info("Top k=$k asymm gates evaluated in %.3fs".format(timeEval.seconds))
        println("  - ids (${topAsymm.size}): $topAsymm")
        println("  - ps: $ps")
        println("  - dises: $dises")
        println("  - mean/geomean p: %.3f / %.3f".format(meanP, geomeanP))
        println("  - mean/geomean dis: %.3f / %.3f".format(meanDis, geomeanDis))
        println("  - saturation: %.3f%%".format(result.saturation * 100.0))
        println("  - domain: ${result.domain.size} / ${2.pow(result.bucket.size)}")
    }

    return emptyList()
}

fun main() {
    val timeStart = timeNow()

    val left = "BubbleSort"
    val right = "PancakeSort"
    // Params: 3_3, 4_3, 5_4, 6_4, ...
    val param = "7_4"
    val aag = "aag" // "aag" or "fraag"
    val filenameLeft = "data/instances/${left}/$aag/${left}_${param}.aag"
    val filenameRight = "data/instances/${right}/$aag/${right}_${param}.aag"

    val aigLeft = parseAig(filenameLeft)
    val aigRight = parseAig(filenameRight)
    // val solverProvider = { MiniSatSolver() }
    // val solverProvider = { GlucoseSolver() }
    val solverProvider = { CadicalSolver() }

    solverProvider().useWith {
        logger.info("Using $this")
        val decomposition = determineDecomposition1(aigRight)
        logger.info("Decomposition size: ${decomposition.size}")
        for (item in decomposition) {
            println("  - $item")
        }
    }

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
