@file:Suppress("LocalVariableName", "FunctionName", "DuplicatedCode")

package com.github.lipen.satlib.nexus.eqcheck

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.Lit
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
import com.github.lipen.satlib.nexus.utils.maybeNumberOfConflicts
import com.github.lipen.satlib.nexus.utils.maybeNumberOfDecisions
import com.github.lipen.satlib.nexus.utils.maybeNumberOfPropagations
import com.github.lipen.satlib.nexus.utils.mean
import com.github.lipen.satlib.nexus.utils.pow
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.nexus.utils.toInt
import com.github.lipen.satlib.op.iffAnd
import com.github.lipen.satlib.solver.CadicalSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.solve
import com.github.lipen.satlib.utils.useWith
import com.soywiz.klock.measureTimeWithResult
import com.soywiz.klock.milliseconds
import mu.KotlinLogging
import java.io.File
import java.util.PriorityQueue
import kotlin.math.round
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

    dumpDimacs(File("cnf_miter.cnf"))

    logger.info("Solving...")
    val (isSat, timeSolve) = measureTimeWithResult { solve() }
    logger.info { "${if (isSat) "SAT" else "UNSAT"} in %.3fs".format(timeSolve.seconds) }
    logger.debug { "Decisions: ${maybeNumberOfDecisions()}" }
    logger.debug { "Conflicts: ${maybeNumberOfConflicts()}" }
    logger.debug { "Propagations: ${maybeNumberOfPropagations()}" }

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

private fun Solver.buildDecomposition(
    aig: Aig,
    pTable: Map<Int, Double>,
    andGateValue: BoolVarArray,
    ids: List<Int>,
): Bucket {
    val ps = ids.map { id -> pTable.getValue(id) }
    val meanP = ps.mean()
    val geomeanP = ps.geomean()

    val dises = ps.map { p -> disbalance(p) }
    val meanDis = dises.mean()
    val geomeanDis = dises.geomean()

    val bucket = ids.map { id -> andGateValue[aig.andGateIds.indexOf(id) + 1] }
    val (result, timeEval) = measureTimeWithResult {
        evalAllValuations(bucket)
    }
    println("  - eval time: %.3fs".format(timeEval.seconds))
    println("  - ids ${ids.size}: $ids")
    println("  - ps: ${ps.map { round(it * 1000.0) / 1000.0 }}")
    println("  - dises: ${dises.map { round(it * 1000.0) / 1000.0 }}")
    println("  - mean/geomean p: %.3f / %.3f".format(meanP, geomeanP))
    println("  - mean/geomean dis: %.3f / %.3f".format(meanDis, geomeanDis))
    println("  - saturation: %.3f%%".format(result.saturation * 100.0))
    println("  - domain: ${result.domain.size} / ${2.pow(result.lits.size)}")

    // return result.domain.map { f -> bucketValuation(bucket, f) }
    return result
}

private fun bucketsDecomposition(
    buckets: List<Bucket>,
): List<List<Lit>> {
    return buckets
        .map { it.decomposition() }
        .cartesianProduct()
        .map { it.flatten() }
        .toList()
}

private fun Solver.mergeBuckets(
    bucket1: Bucket,
    bucket2: Bucket,
): Bucket {
    // bucket1.domain = [1,2,3]
    // bucket2.domain = [4,5]
    // listOf(d1, d2) = [ [1,2,3], [4,5] ]
    // cartesian product = [ [1,4], [1,5], [2,4], [2,5], [3,4], [3,5] ]
    val d = listOf(bucket1.domain, bucket2.domain).cartesianProduct()
        .mapNotNull { (f1, f2) ->
            val v1 = bucketValuation(bucket1.lits, f1)
            val v2 = bucketValuation(bucket2.lits, f2)
            val assumptions = v1 + v2
            if (solve(assumptions)) {
                valuationIndex(assumptions)
            } else {
                null
            }
        }
    val newDomain = d.toList()
    return Bucket(bucket1.lits + bucket2.lits, newDomain)
}

private fun Solver.mergeBucketsTree(
    buckets: Iterable<Bucket>,
): Bucket {
    val queue = PriorityQueue(compareBy<Bucket> { b -> b.saturation })
    queue.addAll(buckets)
    check(queue.isNotEmpty())

    logger.info("Trying to tree-merge buckets...")

    while (queue.size > 1) {
        val b1 = queue.remove()
        val b2 = queue.remove()
        logger.debug {
            "Merging buckets: (lits: ${b1.lits.size}, dom: ${b1.domain.size}, sat: %.3f%%) and (lits: ${b2.lits.size}, dom: ${b2.domain.size}, sat: %.3f%%)".format(
                b1.saturation * 100.0,
                b2.saturation * 100.0
            )
        }
        val b = mergeBuckets(b1, b2)
        logger.debug {
            "New bucket: (lits: ${b.lits.size}, domain: ${b.domain.size}, saturation: %.3f%%))"
                .format(b.saturation * 100.0)
        }
        queue.add(b)
    }

    val mergedBucket = queue.remove()
    logger.info {
        "Final tree-merged bucket: (lits: ${mergedBucket.lits.size}, domain: ${mergedBucket.domain.size}, saturation: %.3f%%))"
            .format(mergedBucket.saturation * 100.0)
    }

    return mergedBucket
}

internal fun Solver.`check circuits equivalence using disbalance-based decomposition`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using disbalance-based decomposition...")

    val sampleSize = 10000
    val randomSeed = 42
    val random = Random(randomSeed)
    logger.info("Computing p-tables using sampleSize=$sampleSize and randomSeed=$randomSeed...")
    val pTableLeft = aigLeft.computePTable(sampleSize, random)
    val pTableRight = aigRight.computePTable(sampleSize, random)
    val idsSortedByPLeft = aigLeft.mapping.keys.sortedBy { id -> pTableLeft.getValue(id) }
    val idsSortedByPRight = aigRight.mapping.keys.sortedBy { id -> pTableRight.getValue(id) }
    val idsSortedByDisLeft = aigLeft.mapping.keys.sortedBy { id -> -disbalance(pTableLeft.getValue(id)) }
    val idsSortedByDisRight = aigRight.mapping.keys.sortedBy { id -> -disbalance(pTableRight.getValue(id)) }

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

    val bucketSize = 14

    val bucketsLeft = run {
        idsSortedByDisLeft.windowed(bucketSize, bucketSize).take(2).mapIndexed { index, ids ->
            logger.info("(Left) Bucket #${index + 1} (size=${ids.size})")
            buildDecomposition(aigLeft, pTableLeft, andGateValueLeft, ids)
        }
    }

    val bucketsRight = run {
        idsSortedByDisRight.windowed(bucketSize, bucketSize).take(2).mapIndexed { index, ids ->
            logger.info("(Right) Bucket #${index + 1} (size=${ids.size})")
            buildDecomposition(aigRight, pTableRight, andGateValueRight, ids)
        }
    }

    logger.info("Left buckets: ${bucketsLeft.size} = ${bucketsLeft.joinToString("+") { it.domain.size.toString() }}")
    logger.info("Right buckets: ${bucketsRight.size} = ${bucketsRight.joinToString("+") { it.domain.size.toString() }}")
    logger.info(
        "Estimated decomposition size: ${
            (bucketsLeft + bucketsRight).map { it.domain.size.toLong() }.reduce(Long::times)
        }"
    )

    val decomposition = bucketsDecomposition(listOf(mergeBucketsTree(bucketsLeft), mergeBucketsTree(bucketsRight)))
    // val decomposition = bucketsDecomposition(bucketsLeft + bucketsRight)
    // val decomposition = mergeBucketsTree(bucketsLeft + bucketsRight).decomposition()
    logger.info("Total decomposition size: ${decomposition.size}")

    logger.info("Encoding miter...")
    encodeMiter()

    logger.info("Solving all ${decomposition.size} instances in the decomposition...")
    val timeStartSolveAll = timeNow()
    for ((index, assumptions) in decomposition.withIndex()) {
        // if (index + 1 == 27542) {
        //     val fileCnf = File("cnf_dec-dis_27542.cnf")
        //     dumpDimacs(fileCnf)
        //     fileCnf.appendingSink().buffer().useWith {
        //         writeln("c Assumptions")
        //         for (x in assumptions) {
        //             writeln("$x 0")
        //         }
        //     }
        // }
        val (res, timeSolve) = measureTimeWithResult {
            solve(assumptions)
            // false
        }
        if (index == 0 || (index + 1) % (if (decomposition.size < 100_000) 1_000 else if (decomposition.size < 1_000_000) 10_000 else 100_000) == 0 || timeSolve >= 500.milliseconds) {
            logger.debug {
                "${if (res) "SAT" else "UNSAT"} on ${index + 1}/${decomposition.size} in %.3fs [total: %.3fs]"
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

internal fun Solver.`check circuits equivalence using layer-wise decomposition`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using layer-wise decomposition...")

    val sampleSize = 10000
    val randomSeed = 42
    val random = Random(randomSeed)
    logger.info("Computing p-tables using sampleSize=$sampleSize and randomSeed=$randomSeed...")
    val pTableLeft = aigLeft.computePTable(sampleSize, random)
    val pTableRight = aigRight.computePTable(sampleSize, random)

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

    val bucketsLeft = run {
        val indices = listOf(81)
        indices.map { i ->
            val layer = aigLeft.layers[i]
            logger.info("(Left) Layer #$i (size=${layer.size})")
            buildDecomposition(aigLeft, pTableLeft, andGateValueLeft, layer)
        }
    }

    val bucketsRight = run {
        // val indices = listOf(141)
        // val indices = listOf(394, 373, 372, 402)
        // val indices = listOf(394, /*373,*/ 327)
        // val indices = listOf(402)
        val indices = listOf(394, 373, 372)
        indices.map { i ->
            val layer = aigRight.layers[i]
            logger.info("(Right) Layer #$i (size=${layer.size})")
            buildDecomposition(aigRight, pTableRight, andGateValueRight, layer)
        }
    }

    logger.info("Left buckets: ${bucketsLeft.size} = ${bucketsLeft.joinToString("+") { it.domain.size.toString() }}")
    logger.info("Right buckets: ${bucketsRight.size} = ${bucketsRight.joinToString("+") { it.domain.size.toString() }}")
    logger.info(
        "Estimated decomposition size: ${
            (bucketsLeft + bucketsRight).map { it.domain.size.toLong() }.reduce(Long::times)
        }"
    )

    val decomposition = bucketsDecomposition(bucketsLeft + bucketsRight)
    // val decomposition = mergeBucketsTree(bucketsLeft + bucketsRight).decomposition()

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
                "${if (res) "SAT" else "UNSAT"} on ${index + 1}/${decomposition.size} in %.3fs [total: %.3fs]"
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

internal fun Solver.`check circuits equivalence using domain-based method`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using domain-based method...")

    val sampleSize = 10000
    val randomSeed = 42
    val random = Random(randomSeed)
    logger.info("Computing p-tables using sampleSize=$sampleSize and randomSeed=$randomSeed...")
    val pTableLeft = aigLeft.computePTable(sampleSize, random)
    val pTableRight = aigRight.computePTable(sampleSize, random)
    val idsSortedByPLeft = aigLeft.andGateIds.sortedBy { id -> pTableLeft.getValue(id) }
    val idsSortedByPRight = aigRight.andGateIds.sortedBy { id -> pTableRight.getValue(id) }
    val idsSortedByDisLeft = aigLeft.andGateIds.sortedBy { id -> -disbalance(pTableLeft.getValue(id)) }
    val idsSortedByDisRight = aigRight.andGateIds.sortedBy { id -> -disbalance(pTableRight.getValue(id)) }

    declare(logger) {
        encodeAigs(aigLeft, aigRight)
    }

    val GL: Int = context["left.G"]
    val GR: Int = context["right.G"]
    val Y: Int = context["Y"]
    val andGateValueLeft: BoolVarArray = context["left.andGateValue"]
    val andGateValueRight: BoolVarArray = context["right.andGateValue"]
    val outputValueLeft: BoolVarArray = context["left.outputValue"]
    val outputValueRight: BoolVarArray = context["right.outputValue"]

    // Freeze gates
    for (g in 1..GL) {
        maybeFreeze(andGateValueLeft[g])
    }
    for (g in 1..GR) {
        maybeFreeze(andGateValueRight[g])
    }
    // Freeze outputs
    for (y in 1..Y) {
        maybeFreeze(outputValueLeft[y])
        maybeFreeze(outputValueRight[y])
    }

    logger.info("Pre-solving...")
    val (isSatMain, timeSolveMain) = measureTimeWithResult { solve() }
    logger.debug("${if (isSatMain) "SAT" else "UNSAT"} in %.3fs".format(timeSolveMain.seconds))
    if (!isSatMain) error("Unexpected UNSAT")

    val bucketSize = 14
    val numberOfBuckets = 6
    val numberOfBucketsAll = numberOfBuckets + 5

    val bucketsLeft = run {
        idsSortedByDisLeft.asSequence()
            .windowed(bucketSize, bucketSize)
            .take(numberOfBucketsAll)
            .mapIndexed { index, ids ->
                logger.info("(Left) Bucket #${index + 1} (size=${ids.size})")
                buildDecomposition(aigLeft, pTableLeft, andGateValueLeft, ids)
            }
            .sortedBy { it.saturation }
            .take(numberOfBuckets)
            .toList()
    }

    val bucketsRight = run {
        idsSortedByDisRight.asSequence()
            .windowed(bucketSize, bucketSize)
            .take(numberOfBucketsAll)
            .mapIndexed { index, ids ->
                logger.info("(Right) Bucket #${index + 1} (size=${ids.size})")
                buildDecomposition(aigRight, pTableRight, andGateValueRight, ids)
            }
            .sortedBy { it.saturation }
            .take(numberOfBuckets)
            .toList()
    }

    logger.info("Left buckets: ${bucketsLeft.size} = ${bucketsLeft.joinToString("+") { it.domain.size.toString() }}")
    logger.info("Right buckets: ${bucketsRight.size} = ${bucketsRight.joinToString("+") { it.domain.size.toString() }}")
    logger.info(
        "Estimated decomposition size: ${
            (bucketsLeft + bucketsRight).map { it.domain.size.toLong() }.reduce(Long::times)
        }"
    )

    reset()
    declare(logger) {
        encodeAigs(aigLeft, aigRight)

        logger.info("Encoding buckets...")
        val buckets = bucketsLeft + bucketsRight
        for ((index, b) in buckets.withIndex()) {
            val nthStr = when (b) {
                in bucketsLeft -> "left ${bucketsLeft.indexOf(b)}th"
                in bucketsRight -> "right ${bucketsRight.indexOf(b)}th"
                else -> error("Bad bucket $b")
            }
            logger.info(
                "Bucket #${index + 1} (${nthStr}): (lits: ${b.lits.size}, domain: ${b.domain.size}, saturation: %.3f%%))"
                    .format(b.saturation * 100.0)
            )
            val vs = b.decomposition().map { lits ->
                val aux = newLiteral()
                iffAnd(aux, lits)
                aux
            }
            addClause(vs)
        }

        logger.info("Encoding miter...")
        encodeMiter()
    }

    // for (x in 1..numberOfVariables) {
    //     maybeMelt(x)
    // }

    dumpDimacs(File("cnf_domain_${bucketSize}-${numberOfBuckets}-${numberOfBuckets}.cnf"))

    logger.info("Solving...")
    val (res, timeSolve) = measureTimeWithResult {
        solve()
    }
    logger.debug { "${if (res) "SAT" else "UNSAT"} in %.3fs".format(timeSolve.seconds) }
    logger.debug { "Decisions: ${maybeNumberOfDecisions()}" }
    logger.debug { "Conflicts: ${maybeNumberOfConflicts()}" }
    logger.debug { "Propagations: ${maybeNumberOfPropagations()}" }
    if (res) {
        logger.warn("Circuits are NOT equivalent!")
        return false
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
            "dec-dis" -> `check circuits equivalence using disbalance-based decomposition`(aigLeft, aigRight)
            "dec-layer" -> `check circuits equivalence using layer-wise decomposition`(aigLeft, aigRight)
            "domain" -> `check circuits equivalence using domain-based method`(aigLeft, aigRight)
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
    val param = "6_4"
    val aag = "fraag" // "aag" or "fraag"
    val filenameLeft = "data/instances/${left}/$aag/${left}_${param}.aag"
    val filenameRight = "data/instances/${right}/$aag/${right}_${param}.aag"

    val aigLeft = parseAig(filenameLeft)
    val aigRight = parseAig(filenameRight)
    // val solverProvider = { MiniSatSolver() }
    // val solverProvider = { GlucoseSolver() }
    // val solverProvider = { CryptoMiniSatSolver() }
    val solverProvider = { CadicalSolver() }
    // Methods: "miter", "merge-eq", "merge-xor", "conj", "dec-layer", "dec-dis", "domain"
    // val method = "dec-dis"
    val method = "miter"

    checkEquivalence(aigLeft, aigRight, solverProvider, method)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
