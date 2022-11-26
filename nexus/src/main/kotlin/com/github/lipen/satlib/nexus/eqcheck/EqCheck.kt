@file:Suppress("LocalVariableName", "FunctionName", "DuplicatedCode")

package com.github.lipen.satlib.nexus.eqcheck

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.cone
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.aig.shadow
import com.github.lipen.satlib.nexus.encoding.encodeAigs
import com.github.lipen.satlib.nexus.encoding.encodeMiter
import com.github.lipen.satlib.nexus.encoding.encodeOutputMergers
import com.github.lipen.satlib.nexus.utils.bit
import com.github.lipen.satlib.nexus.utils.cartesianProduct
import com.github.lipen.satlib.nexus.utils.declare
import com.github.lipen.satlib.nexus.utils.geomean
import com.github.lipen.satlib.nexus.utils.iffMaj3
import com.github.lipen.satlib.nexus.utils.iffXor2
import com.github.lipen.satlib.nexus.utils.iffXor3
import com.github.lipen.satlib.nexus.utils.implyNand
import com.github.lipen.satlib.nexus.utils.isEven
import com.github.lipen.satlib.nexus.utils.maybeFreeze
import com.github.lipen.satlib.nexus.utils.maybeMelt
import com.github.lipen.satlib.nexus.utils.maybeNumberOfConflicts
import com.github.lipen.satlib.nexus.utils.maybeNumberOfDecisions
import com.github.lipen.satlib.nexus.utils.maybeNumberOfPropagations
import com.github.lipen.satlib.nexus.utils.mean
import com.github.lipen.satlib.nexus.utils.pow
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.nexus.utils.toBinaryString
import com.github.lipen.satlib.nexus.utils.toInt
import com.github.lipen.satlib.op.iffAnd
import com.github.lipen.satlib.op.iffOr
import com.github.lipen.satlib.op.implyOr
import com.github.lipen.satlib.op.runWithTimeout2
import com.github.lipen.satlib.solver.CadicalSolver
import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.solve
import com.github.lipen.satlib.utils.lineSequence
import com.github.lipen.satlib.utils.useWith
import com.github.lipen.satlib.utils.writeln
import com.soywiz.klock.measureTimeWithResult
import com.soywiz.klock.milliseconds
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import okio.appendingSink
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.PriorityQueue
import kotlin.io.path.exists
import kotlin.math.absoluteValue
import kotlin.math.round
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

val globalOptions: MutableMap<String, String> = mutableMapOf()

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

internal fun Solver.encodeDnf(cubes: List<List<Lit>>) {
    val cubeVar = newBoolVarArray(cubes.size)
    for (i in 1..cubes.size) {
        iffOr(cubeVar[i], cubes[i - 1])
    }
    addClause((1..cubes.size).map { i -> cubeVar[i] })
}

internal fun Solver.encodeDnfTree(cubes: List<List<Lit>>) {
    val queue: PriorityQueue<List<Lit>> = PriorityQueue(cubes.size) { a, b ->
        a.size compareTo b.size
    }
    queue.addAll(cubes)

    while (queue.size > 1) {
        val c1 = queue.remove()
        val c2 = queue.remove()
        for (clause in listOf(c1, c2).cartesianProduct()) {
            var ok = true
            for (lit in clause) {
                if (clause.contains(-lit)) {
                    ok = false
                    break
                }
            }
            logger.debug("Queueing clause of size ${clause.distinct().size}")
            if (ok) queue.add(clause.distinct())
        }
    }
    val clause = queue.remove()
    logger.debug("Final clause of size ${clause.size}")
    addClause(clause)

    // for (clause in cubes.cartesianProduct()) {
    //     var ok = true
    //     for (lit in clause) {
    //         if (clause.contains(-lit)) {
    //             ok = false
    //             break
    //         }
    //     }
    //     if (ok) addClause(clause.toSet())
    // }
}

internal fun Solver.`check circuits equivalence using disbalance-based decomposition with trie`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using disbalance-based decomposition (with Trie)...")

    val sampleSize = 10000
    val randomSeed = 42
    val random = Random(randomSeed)
    logger.info("Computing p-tables using sampleSize=$sampleSize and randomSeed=$randomSeed...")
    val pTableLeft = aigLeft.computePTable(sampleSize, random)
    val pTableRight = aigRight.computePTable(sampleSize, random)
    val idsSortedByPLeft = aigLeft.mapping.keys.sortedBy { id -> pTableLeft.getValue(id) }
    val idsSortedByPRight = aigRight.mapping.keys.sortedBy { id -> pTableRight.getValue(id) }
    val idsSortedByDisLeft = aigLeft.mapping.keys.sortedBy { id -> -disbalance(pTableLeft.getValue(id), .25) }
    val idsSortedByDisRight = aigRight.mapping.keys.sortedBy { id -> -disbalance(pTableRight.getValue(id), .25) }

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

    val bucketSize = 12
    val numberOfBuckets = Pair(2, 2)
    // val numberOfBucketsCompute = (100 / bucketSize)
    val numberOfBucketsCompute = 5

    val bucketsLeft = run {
        idsSortedByDisLeft.asSequence()
            .windowed(bucketSize, bucketSize)
            .take(numberOfBucketsCompute)
            .mapIndexed { index, ids ->
                logger.info("(Left) Bucket #${index + 1} (size=${ids.size})")
                buildDecomposition(aigLeft, pTableLeft, andGateValueLeft, ids)
            }
            .sortedBy { it.saturation }
            .take(numberOfBuckets.first)
            .toList()
    }

    val bucketsRight = run {
        idsSortedByDisRight.asSequence()
            .windowed(bucketSize, bucketSize)
            .take(numberOfBucketsCompute)
            .mapIndexed { index, ids ->
                logger.info("(Right) Bucket #${index + 1} (size=${ids.size})")
                buildDecomposition(aigRight, pTableRight, andGateValueRight, ids)
            }
            .sortedBy { it.saturation }
            .take(numberOfBuckets.second)
            .toList()
    }

    logger.info("Left buckets: ${bucketsLeft.size} = ${bucketsLeft.joinToString("+") { it.domain.size.toString() }}")
    logger.info("Right buckets: ${bucketsRight.size} = ${bucketsRight.joinToString("+") { it.domain.size.toString() }}")
    logger.info(
        "Estimated decomposition size: ${
            (bucketsLeft + bucketsRight).map { it.domain.size.toLong() }.reduce(Long::times)
        }"
    )

    val megaBucketLeft = mergeBucketsTree(bucketsLeft)
    val megaBucketRight = mergeBucketsTree(bucketsRight)

    val idsLeft = megaBucketLeft.lits.map { lit -> aigLeft.andGateIds[andGateValueLeft.values.indexOf(lit)] }
    val idsRight = megaBucketRight.lits.map { lit -> aigRight.andGateIds[andGateValueRight.values.indexOf(lit)] }
    logger.debug { "Gates in left mega-bucket: $idsLeft" }
    logger.debug { "Gates in right mega-bucket: $idsRight" }

    val decomposition = bucketsDecomposition(listOf(megaBucketLeft, megaBucketRight))
        .map { lits -> lits.sortedBy { lit -> lit.absoluteValue } }
    logger.info("Total decomposition size: ${decomposition.size}")

    // // ==================
    // run {
    //     encodeMiter()
    //     val dataDir = File("data/icnf")
    //     dataDir.mkdirs()
    //     val cnfFile = dataDir.resolve("temp.cnf")
    //     dumpDimacs(cnfFile)
    //     val icnfFile = dataDir.resolve("decomposition.icnf")
    //     cnfFile.source().buffer().use { src ->
    //         icnfFile.sink().buffer().useWith {
    //             writeln("p inccnf")
    //             for (line in src.lineSequence().drop(1)) {
    //                 writeln(line)
    //             }
    //             for (lits in decomposition) {
    //                 writeln("a ${lits.joinToString(" ")} 0")
    //             }
    //         }
    //     }
    //     // cnfFile.delete()
    // }
    // return true
    // // ==================

    val n = megaBucketLeft.lits.size + megaBucketRight.lits.size
    val permutation = (0 until n).shuffled(Random(42))
    val decompositionShuffled = decomposition.map { lits ->
        permutation.map { i -> lits[i] }
    }
    val cubes = decompositionShuffled.map { lits -> lits.map { lit -> lit > 0 } }
    val varsShuffled = decompositionShuffled.first().map { lit -> lit.absoluteValue }

    logger.info("Building trie...")
    val trie = buildTrie(cubes)
    logger.info("Done building trie")

    val limit = 100
    logger.info("Performing trie.dfsLimited($limit)...")
    val partition = trie.dfsLimited(limit).toList()
    println("trie.dfsLimited($limit): (total ${partition.size})")
    for (node in partition.take(10)) {
        println("  - ${node.cube.toBinaryString().padEnd(n, '.')} (leaves: ${node.leaves})")
    }

    data class TimingInfo(
        val indexUnit: Int,
        val indexLeaf: Int,
        val time: Double,
    )

    val timeStartAll = timeNow()
    val times: MutableList<TimingInfo> = mutableListOf()
    logger.info("Solving in ${partition.size} iterations...")

    for ((indexUnit, node) in partition.withIndex()) {
        if (indexUnit != 50) {
            continue
        }

        val timeStartIter = timeNow()
        reset()
        encodeAigs(aigLeft, aigRight)
        encodeMiter()

        val units = node.cube.mapIndexed { i, b -> varsShuffled[i] sign b }
        for (lit in units) {
            addClause(lit)
        }

        val k = units.size
        val leaves = node.dfs().drop(1).filter { it.isLeaf() }.toList()

        logger.info("Iteration #${indexUnit + 1}: ${leaves.size} leaves")
        if (leaves.isEmpty()) {
            logger.debug("Solving iteration #${indexUnit + 1}/${partition.size} with ${units.size} units and without assumptions...")
            val (res, timeSolve) = measureTimeWithResult {
                solve()
            }
            times.add(TimingInfo(indexUnit, 0, timeSolve.seconds))
            if (res) {
                logger.warn("Circuits are NOT equivalent!")
                return false
            }
        } else for ((indexLeaf, leaf) in leaves.withIndex()) {
            val assumptions = (k until leaf.cube.size).map { i -> varsShuffled[i] sign leaf.cube[i] }
            logger.debug {
                "Iteration $indexUnit/$indexUnit with units=$units, assumptions=$assumptions"
            }
            // logger.debug {
            //     "indexUnit/indexLeaf=$indexUnit/$indexLeaf, Units+Assumptions: " +
            //         units.map { it > 0 }.toBinaryString() +
            //         "+" +
            //         assumptions.map { it > 0 }.toBinaryString()
            // }
            val (res, timeSolve) = measureTimeWithResult {
                val (isSat, isTimeout) = runWithTimeout2(30 * 1000) {
                    solve(assumptions)
                }
                if (isTimeout) {
                    logger.warn("Timeout on indexUnit=$indexUnit, indexLeaf=$indexLeaf")
                }
                isSat
            }
            if (timeSolve > 500.milliseconds) {
                logger.debug {
                    "Solved iteration #${indexUnit + 1}/${partition.size}, leaf ${indexLeaf + 1}/${leaves.size} with ${units.size} units and ${assumptions.size} assumptions in %.3fs"
                        .format(timeSolve.seconds)
                }
            }
            times.add(TimingInfo(indexUnit, indexLeaf, timeSolve.seconds))
            if (res) {
                logger.warn("Circuits are NOT equivalent!")
                return false
            }
        }
        logger.info {
            "Iteration #${indexUnit + 1}/${partition.size} done in %.3fs [total solve: %.3fs, total wall: %.3fs]"
                .format(secondsSince(timeStartIter), times.sumOf { it.time }, secondsSince(timeStartAll))
        }
        // if (indexUnit == 0 || (indexUnit + 1) % 100 == 0 || secondsSince(timeStartIter) > 2) {
        //     logger.info {
        //         "Iteration #${indexUnit + 1}/${partition.size} done in %.3fs [total solve: %.3fs, total wall: %.3fs]"
        //             .format(secondsSince(timeStartIter), times.sumOf { it.time }, secondsSince(timeStartAll))
        //     }
        // }
    }
    logger.info("Max times:")
    for ((indexUnit, indexLeaf, time) in times.sortedByDescending { it.time }.take(50)) {
        logger.info("  - %.3fs on $indexUnit/$indexLeaf".format(time))
    }

    logger.info("Circuits are equivalent!")
    return true
}

internal fun Solver.`check circuits equivalence using disbalance-based decomposition with dnf`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using disbalance-based decomposition (with DNF)...")

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

    val bucketSize = 12
    val numberOfBuckets = Pair(2, 2)
    // val numberOfBucketsCompute = (100 / bucketSize)
    val numberOfBucketsCompute = 5

    val bucketsLeft = run {
        idsSortedByDisLeft.asSequence()
            .windowed(bucketSize, bucketSize)
            .take(numberOfBucketsCompute)
            .mapIndexed { index, ids ->
                logger.info("(Left) Bucket #${index + 1} (size=${ids.size})")
                buildDecomposition(aigLeft, pTableLeft, andGateValueLeft, ids)
            }
            .sortedBy { it.saturation }
            .take(numberOfBuckets.first)
            .toList()
    }

    val bucketsRight = run {
        idsSortedByDisRight.asSequence()
            .windowed(bucketSize, bucketSize)
            .take(numberOfBucketsCompute)
            .mapIndexed { index, ids ->
                logger.info("(Right) Bucket #${index + 1} (size=${ids.size})")
                buildDecomposition(aigRight, pTableRight, andGateValueRight, ids)
            }
            .sortedBy { it.saturation }
            .take(numberOfBuckets.second)
            .toList()
    }

    logger.info("Left buckets: ${bucketsLeft.size} = ${bucketsLeft.joinToString("+") { it.domain.size.toString() }}")
    logger.info("Right buckets: ${bucketsRight.size} = ${bucketsRight.joinToString("+") { it.domain.size.toString() }}")
    logger.info(
        "Estimated decomposition size: ${
            (bucketsLeft + bucketsRight).map { it.domain.size.toLong() }.reduce(Long::times)
        }"
    )

    val megaBucketLeft = mergeBucketsTree(bucketsLeft)
    val megaBucketRight = mergeBucketsTree(bucketsRight)
    logger.info {
        "Left mega-bucket: (lits: ${megaBucketLeft.lits.size}, domain: ${megaBucketLeft.domain.size}, saturation: %.3f%%))"
            .format(megaBucketLeft.saturation * 100.0)
    }
    logger.info {
        "Right mega-bucket: (lits: ${megaBucketRight.lits.size}, domain: ${megaBucketRight.domain.size}, saturation: %.3f%%))"
            .format(megaBucketRight.saturation * 100.0)
    }

    val dnfLeft = megaBucketLeft.decomposition()
    val dnfRight = megaBucketRight.decomposition()
    val decomposition = megaBucketLeft.decomposition()
    logger.info("Left DNF size: ${dnfLeft.size}")
    logger.info("Right DNF size: ${dnfRight.size}")
    logger.info("Decomposition size: ${decomposition.size}")

    encodeMiter()
    encodeDnf(dnfLeft)
    encodeDnf(dnfRight)

    dumpDimacs(File("cnf_dec-dis-dnf.cnf"))

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

    // data class TimingInfo(
    //     val index: Int,
    //     val time: Double,
    // )
    //
    // val timeStartAll = timeNow()
    // val times: MutableList<TimingInfo> = mutableListOf()
    //
    // logger.info("Solving...")
    // for ((index, lits) in decomposition.withIndex()) {
    //     val timeStartIter = timeNow()
    //     // reset()
    //     // encodeAigs(aigLeft, aigRight)
    //     // encodeMiter()
    //     // encodeDnf(dnfRight)
    //
    //     // for (lit in lits) {
    //     //     addClause(lit)
    //     // }
    //
    //     val (res, timeSolve) = measureTimeWithResult {
    //         val (isSat, isTimeout) = runWithTimeout2(30 * 1000) {
    //             solve(lits)
    //         }
    //         if (isTimeout) {
    //             logger.warn("Timeout on index=$index (0-based)")
    //         }
    //         isSat
    //     }
    //     times.add(TimingInfo(index, timeSolve.seconds))
    //     logger.info {
    //         "Iteration #${index + 1}/${decomposition.size} done in %.3fs [total solve: %.3fs, total wall: %.3fs]"
    //             .format(secondsSince(timeStartIter), times.sumOf { it.time }, secondsSince(timeStartAll))
    //     }
    //     if (res) {
    //         logger.warn("Circuits are NOT equivalent!")
    //         return false
    //     }
    // }
    // logger.info("Max times:")
    // for ((index, time) in times.sortedByDescending { it.time }.take(100)) {
    //     logger.info("  - %.3fs on $index".format(time))
    // }
    //
    // logger.info("Circuits are equivalent!")
    // return true
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

internal fun Solver.`check circuits equivalence using method 10`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using method 10...")

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

    val bucketSize = 12
    val numberOfBuckets = Pair(2, 2)
    // val numberOfBucketsCompute = (100 / bucketSize)
    val numberOfBucketsCompute = 5

    val bucketsLeft = run {
        idsSortedByDisLeft.asSequence()
            .windowed(bucketSize, bucketSize)
            .take(numberOfBucketsCompute)
            .mapIndexed { index, ids ->
                logger.info("(Left) Bucket #${index + 1} (size=${ids.size})")
                buildDecomposition(aigLeft, pTableLeft, andGateValueLeft, ids)
            }
            .sortedBy { it.saturation }
            .take(numberOfBuckets.first)
            .toList()
    }

    val bucketsRight = run {
        idsSortedByDisRight.asSequence()
            .windowed(bucketSize, bucketSize)
            .take(numberOfBucketsCompute)
            .mapIndexed { index, ids ->
                logger.info("(Right) Bucket #${index + 1} (size=${ids.size})")
                buildDecomposition(aigRight, pTableRight, andGateValueRight, ids)
            }
            .sortedBy { it.saturation }
            .take(numberOfBuckets.second)
            .toList()
    }

    logger.info("Left buckets: ${bucketsLeft.size} = ${bucketsLeft.joinToString("+") { it.domain.size.toString() }}")
    logger.info("Right buckets: ${bucketsRight.size} = ${bucketsRight.joinToString("+") { it.domain.size.toString() }}")
    logger.info(
        "Estimated decomposition size: ${
            (bucketsLeft + bucketsRight).map { it.domain.size.toLong() }.reduce(Long::times)
        }"
    )

    var megaBucketLeft = mergeBucketsTree(bucketsLeft)
    var megaBucketRight = mergeBucketsTree(bucketsRight)
    logger.info {
        "Left mega-bucket: (lits: ${megaBucketLeft.lits.size}, domain: ${megaBucketLeft.domain.size}, saturation: %.3f%%))"
            .format(megaBucketLeft.saturation * 100.0)
    }
    logger.info {
        "Right mega-bucket: (lits: ${megaBucketRight.lits.size}, domain: ${megaBucketRight.domain.size}, saturation: %.3f%%))"
            .format(megaBucketRight.saturation * 100.0)
    }

    fun Aig.computeMetric(ids: List<Int>): Map<Int, Int> =
        andGateIds.associateWith { id ->
            (cone(id) + shadow(id)).intersect(ids).size
        }

    fun qwerty() {
        val idsLeft = megaBucketLeft.lits.map { lit -> aigLeft.andGateIds[andGateValueLeft.values.indexOf(lit)] }
        val idsRight = megaBucketLeft.lits.map { lit -> aigLeft.andGateIds[andGateValueLeft.values.indexOf(lit)] }
        val metricLeft: Map<Int, Int> = aigLeft.computeMetric(idsLeft)
        val metricRight: Map<Int, Int> = aigRight.computeMetric(idsRight)
        val (bestIdLeft, bestMetricLeft) = metricLeft.minByOrNull { it.value }!!
        val (bestIdRight, bestMetricRight) = metricRight.minByOrNull { it.value }!!
        val bestLitLeft = andGateValueLeft[aigLeft.andGateIds.indexOf(bestIdLeft) + 1]
        val bestLitRight = andGateValueRight[aigRight.andGateIds.indexOf(bestIdRight) + 1]

        logger.debug("Extending left bucket with gate id=${bestIdLeft}, metric=${bestMetricLeft}, lit=${bestLitLeft}")
        megaBucketLeft = mergeBuckets(megaBucketLeft, Bucket(listOf(bestLitLeft), listOf(0, 1)))
        logger.debug("Extending right bucket with gate id=${bestIdRight}, metric=${bestMetricRight}, lit=${bestLitRight}")
        megaBucketRight = mergeBuckets(megaBucketRight, Bucket(listOf(bestLitRight), listOf(0, 1)))
    }

    logger.info("Extending...")
    qwerty()

    logger.info {
        "Left mega-bucket: (lits: ${megaBucketLeft.lits.size}, domain: ${megaBucketLeft.domain.size}, saturation: %.3f%%))"
            .format(megaBucketLeft.saturation * 100.0)
    }
    logger.info {
        "Right mega-bucket: (lits: ${megaBucketRight.lits.size}, domain: ${megaBucketRight.domain.size}, saturation: %.3f%%))"
            .format(megaBucketRight.saturation * 100.0)
    }

    val decomposition = bucketsDecomposition(listOf(megaBucketLeft, megaBucketRight))
        .map { lits -> lits.sortedBy { lit -> lit.absoluteValue } }
    logger.info("Total decomposition size: ${decomposition.size}")

    val n = megaBucketLeft.lits.size + megaBucketRight.lits.size
    val permutation = (0 until n).shuffled(Random(42))
    val decompositionShuffled = decomposition.map { lits ->
        permutation.map { i -> lits[i] }
    }
    val cubes = decompositionShuffled.map { lits -> lits.map { lit -> lit > 0 } }
    val varsShuffled = decompositionShuffled.first().map { lit -> lit.absoluteValue }

    logger.info("Building trie...")
    val trie = buildTrie(cubes)
    logger.info("Done building trie")

    val limit = 200
    logger.info("Performing trie.dfsLimited($limit)...")
    val partition = trie.dfsLimited(limit).toList()
    println("trie.dfsLimited($limit): (total ${partition.size})")
    for (node in partition.take(10)) {
        println("  - ${node.cube.toBinaryString().padEnd(n, '.')} (leaves: ${node.leaves})")
    }

    data class TimingInfo(
        val indexUnit: Int,
        val indexLeaf: Int,
        val time: Double,
    )

    val timeStartAll = timeNow()
    val times: MutableList<TimingInfo> = mutableListOf()
    logger.info("Solving in ${partition.size} iterations...")
    for ((indexUnit, node) in partition.withIndex()) {
        val timeStartIter = timeNow()
        reset()
        encodeAigs(aigLeft, aigRight)
        encodeMiter()

        val units = node.cube.mapIndexed { i, b -> varsShuffled[i] sign b }
        for (lit in units) {
            addClause(lit)
        }

        val k = units.size
        val leaves = node.dfs().drop(1).filter { it.isLeaf() }.toList()

        logger.info("Iteration #${indexUnit + 1}: ${leaves.size} leaves")
        if (leaves.isEmpty()) {
            logger.debug("Solving iteration #${indexUnit + 1}/${partition.size} with ${units.size} units and without assumptions...")
            val (res, timeSolve) = measureTimeWithResult {
                solve()
            }
            times.add(TimingInfo(indexUnit, 0, timeSolve.seconds))
            if (res) {
                logger.warn("Circuits are NOT equivalent!")
                return false
            }
        } else for ((indexLeaf, leaf) in leaves.withIndex()) {
            val assumptions = (k until leaf.cube.size).map { i -> varsShuffled[i] sign leaf.cube[i] }
            val (res, timeSolve) = measureTimeWithResult {
                val (isSat, isTimeout) = runWithTimeout2(30 * 1000) {
                    solve(assumptions)
                }
                if (isTimeout) {
                    logger.warn("Timeout on indexUnit=$indexUnit, indexLeaf=$indexLeaf")
                }
                isSat
            }
            logger.debug(
                "Solved iteration #${indexUnit + 1}/${partition.size}, leaf ${indexLeaf + 1}/${leaves.size} with ${units.size} units and ${assumptions.size} assumptions in %.3fs"
                    .format(timeSolve.seconds)
            )
            times.add(TimingInfo(indexUnit, indexLeaf, timeSolve.seconds))
            if (res) {
                logger.warn("Circuits are NOT equivalent!")
                return false
            }
        }
        logger.info {
            "Iteration #${indexUnit + 1}/${partition.size} done in %.3fs [total solve: %.3fs, total wall: %.3fs]"
                .format(secondsSince(timeStartIter), times.sumOf { it.time }, secondsSince(timeStartAll))
        }
        // if (indexUnit == 0 || (indexUnit + 1) % 100 == 0 || secondsSince(timeStartIter) > 2) {
        //     logger.info {
        //         "Iteration #${indexUnit + 1}/${partition.size} done in %.3fs [total solve: %.3fs, total wall: %.3fs]"
        //             .format(secondsSince(timeStartIter), times.sumOf { it.time }, secondsSince(timeStartAll))
        //     }
        // }
    }
    logger.info("Max times:")
    for ((indexUnit, indexLeaf, time) in times.sortedByDescending { it.time }.take(100)) {
        logger.info("  - %.3fs on $indexUnit/$indexLeaf".format(time))
    }

    logger.info("Circuits are equivalent!")
    return true
}

internal fun Solver.`check circuits equivalence using method 11`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using method 11...")

    val sampleSize = 10000
    val randomSeed = 42
    val random = Random(randomSeed)
    logger.info("Computing p-tables using sampleSize=$sampleSize and randomSeed=$randomSeed...")
    val pTableLeft = aigLeft.computePTable(sampleSize, random)
    val pTableRight = aigRight.computePTable(sampleSize, random)
    val idsSortedByPLeft = aigLeft.mapping.keys.sortedBy { id -> pTableLeft.getValue(id) }
    val idsSortedByPRight = aigRight.mapping.keys.sortedBy { id -> pTableRight.getValue(id) }
    val idsSortedByDisLeft = aigLeft.mapping.keys.sortedBy { id -> -disbalance(pTableLeft.getValue(id), .25) }
    val idsSortedByDisRight = aigRight.mapping.keys.sortedBy { id -> -disbalance(pTableRight.getValue(id), .25) }

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

    fun computeLevels(aig: Aig): List<List<Int>> {
        // last level is all outputs
        // 0-th level is probably all inputs

        val levels = List(aig.layers.size) { mutableSetOf<Int>() }
        // levels.last().addAll(aig.outputIds)

        for (id in aig.mapping.keys) {
            val layerId = aig.layerIndex(id)
            levels[layerId].add(id)
            if (id in aig.outputIds) {
                check(aig.parents(id).isEmpty())
                for (i in (layerId + 1) until aig.layers.size) {
                    levels[i].add(id)
                }
            }
            for (parentId in aig.parents(id).map { it.id }) {
                val parentLayerId = aig.layerIndex(parentId)
                for (i in (layerId + 1) until parentLayerId) {
                    levels[i].add(id)
                }
            }
        }

        return levels.map { it.toList() }
    }

    fun getBest(levels: List<List<Int>>, aig: Aig, pTable: Map<Int, Double>): List<Int> {
        return levels.maxByOrNull { level ->
            val dises = level.map { id ->
                val midpoint = if (id in aig.inputIds) 0.5 else 0.25
                disbalance(pTable.getValue(id), midpoint = midpoint)
            }
            val geomeanDis = dises.geomean()
            geomeanDis / level.size
        }!!
    }

    fun pprintLevel(aig: Aig, index: Int, level: List<Int>, pTable: Map<Int, Double>) {
        val dises = level.map { id ->
            val midpoint = if (id in aig.inputIds) 0.5 else 0.25
            disbalance(pTable.getValue(id), midpoint = midpoint)
        }
        val geomeanDis = dises.geomean()
        println(
            "Level #$index of size ${level.size}: ${
                level.count { aig.inputIds.contains(it) }
            } inputs, ${
                level.count { aig.outputIds.contains(it) }
            } outputs, geomean(dis)=%.3f, geomean(dis)/size*1000=%.3f"
                .format(geomeanDis, geomeanDis / level.size * 1000.0)
        )
    }

    fun pprintLevels(aig: Aig, levels: List<List<Int>>, pTable: Map<Int, Double>) {
        for ((index, level) in levels.withIndex()) {
            pprintLevel(aig, index, level, pTable)
        }
    }

    // val bucketSize = 12
    // val numberOfBuckets = Pair(1, 1)
    // // val numberOfBuckets = Pair(2, 2)
    // // val numberOfBucketsCompute = (100 / bucketSize)
    // val numberOfBucketsCompute = 5
    //
    // val bucketsLeft = run {
    //     idsSortedByDisLeft.asSequence()
    //         .windowed(bucketSize, bucketSize)
    //         .take(numberOfBucketsCompute)
    //         .mapIndexed { index, ids ->
    //             logger.info("(Left) Bucket #${index + 1} (size=${ids.size})")
    //             buildDecomposition(aigLeft, pTableLeft, andGateValueLeft, ids)
    //         }
    //         .sortedBy { it.saturation }
    //         .take(numberOfBuckets.first)
    //         .toList()
    // }
    //
    // val bucketsRight = run {
    //     idsSortedByDisRight.asSequence()
    //         .windowed(bucketSize, bucketSize)
    //         .take(numberOfBucketsCompute)
    //         .mapIndexed { index, ids ->
    //             logger.info("(Right) Bucket #${index + 1} (size=${ids.size})")
    //             buildDecomposition(aigRight, pTableRight, andGateValueRight, ids)
    //         }
    //         .sortedBy { it.saturation }
    //         .take(numberOfBuckets.second)
    //         .toList()
    // }
    //
    // logger.info("Left buckets: ${bucketsLeft.size} = ${bucketsLeft.joinToString("+") { it.domain.size.toString() }}")
    // logger.info("Right buckets: ${bucketsRight.size} = ${bucketsRight.joinToString("+") { it.domain.size.toString() }}")
    // logger.info {
    //     "Estimated decomposition size: ${
    //         (bucketsLeft + bucketsRight).map { it.domain.size.toLong() }.reduce(Long::times)
    //     }"
    // }

    val levelsLeft = computeLevels(aigLeft)
    val levelsRight = computeLevels(aigRight)

    // logger.info("Levels (${levelsLeft.size}) in Left AIG:")
    // pprintLevels(aigLeft, levelsLeft, pTableLeft)
    // logger.info("Levels (${levelsRight.size}) in Right AIG:")
    // pprintLevels(aigLeft, levelsRight, pTableRight)

    val levelLeft = getBest(levelsLeft, aigLeft, pTableLeft)
    val levelRight = getBest(levelsRight, aigRight, pTableRight)

    logger.info { "Best Left level:" }
    pprintLevel(aigLeft, levelsLeft.indexOf(levelLeft), levelLeft, pTableLeft)
    logger.info { "Best Right level" }
    pprintLevel(aigRight, levelsRight.indexOf(levelRight), levelRight, pTableRight)

    // ==========
    check(levelLeft.size == 28)
    check(levelRight.size == 28)
    val bucketsLeft = levelLeft.windowed(14, 14).map { ids ->
        logger.info("One of left buckets:")
        buildDecomposition(aigLeft, pTableLeft, andGateValueLeft, ids)
    }
    val bucketsRight = levelRight.windowed(14, 14).map { ids ->
        logger.info("One of right buckets:")
        buildDecomposition(aigRight, pTableRight, andGateValueRight, ids)
    }
    // ==========

    var megaBucketLeft = mergeBucketsTree(bucketsLeft)
    var megaBucketRight = mergeBucketsTree(bucketsRight)

    fun Aig.computeMetric(idsChoose: List<Int>, idsTaken: List<Int>): Map<Int, Int> =
        idsChoose.associateWith { id ->
            (cone(id) + shadow(id)).intersect(idsTaken.toSet()).size
        }

    fun qwerty() {
        val idsLeft = megaBucketLeft.lits.map { lit -> aigLeft.andGateIds[andGateValueLeft.values.indexOf(lit)] }
        val idsRight = megaBucketRight.lits.map { lit -> aigRight.andGateIds[andGateValueRight.values.indexOf(lit)] }
        val metricLeft: Map<Int, Int> = aigLeft.computeMetric(levelLeft - idsLeft, idsLeft)
        val metricRight: Map<Int, Int> = aigRight.computeMetric(levelRight - idsRight, idsRight)
        val (bestIdLeft, bestMetricLeft) = metricLeft.minByOrNull { it.value }!!
        val (bestIdRight, bestMetricRight) = metricRight.minByOrNull { it.value }!!
        val bestLitLeft = andGateValueLeft[aigLeft.andGateIds.indexOf(bestIdLeft) + 1]
        val bestLitRight = andGateValueRight[aigRight.andGateIds.indexOf(bestIdRight) + 1]

        logger.debug {
            "Extending left bucket with gate id=${bestIdLeft}, metric=${bestMetricLeft}, lit=${bestLitLeft}"
        }
        megaBucketLeft = mergeBuckets(megaBucketLeft, Bucket(listOf(bestLitLeft), listOf(0, 1)))
        logger.debug {
            "New left bucket: (lits: ${megaBucketLeft.lits.size}, domain: ${megaBucketLeft.domain.size}, saturation: %.3f%%))"
                .format(megaBucketLeft.saturation * 100.0)
        }
        logger.debug {
            "Extending right bucket with gate id=${bestIdRight}, metric=${bestMetricRight}, lit=${bestLitRight}..."
        }
        megaBucketRight = mergeBuckets(megaBucketRight, Bucket(listOf(bestLitRight), listOf(0, 1)))
        logger.debug {
            "New right bucket: (lits: ${megaBucketRight.lits.size}, domain: ${megaBucketRight.domain.size}, saturation: %.3f%%))"
                .format(megaBucketRight.saturation * 100.0)
        }
    }

    logger.info("Extending...")
    repeat(10) {
        qwerty()
    }

    logger.info {
        "Left mega-bucket: (lits: ${megaBucketLeft.lits.size}, domain: ${megaBucketLeft.domain.size}, saturation: %.3f%%))"
            .format(megaBucketLeft.saturation * 100.0)
    }
    logger.info {
        "Right mega-bucket: (lits: ${megaBucketRight.lits.size}, domain: ${megaBucketRight.domain.size}, saturation: %.3f%%))"
            .format(megaBucketRight.saturation * 100.0)
    }

    val decomposition = bucketsDecomposition(listOf(megaBucketLeft, megaBucketRight)).toList()
    logger.info("Total decomposition size: ${decomposition.size}")

    val n = megaBucketLeft.lits.size + megaBucketRight.lits.size
    val permutation = (0 until n).shuffled(Random(42))
    val decompositionShuffled = decomposition.map { lits ->
        permutation.map { i -> lits[i] }
    }
    val cubes = decompositionShuffled.map { lits -> lits.map { lit -> lit > 0 } }
    val varsShuffled = decompositionShuffled.first().map { lit -> lit.absoluteValue }

    logger.info("Building trie...")
    val trie = buildTrie(cubes)
    logger.info("Done building trie")

    val limit = 500
    logger.info("Performing trie.dfsLimited($limit)...")
    val partition = trie.dfsLimited(limit).toList()
    println("trie.dfsLimited($limit): (total ${partition.size})")
    for (node in partition.take(10)) {
        println("  - ${node.cube.toBinaryString().padEnd(n, '.')} (leaves: ${node.leaves})")
    }

    data class TimingInfo(
        val indexUnit: Int,
        val indexLeaf: Int,
        val time: Double,
    )

    val timeStartAll = timeNow()
    val times: MutableList<TimingInfo> = mutableListOf()
    logger.info("Solving in ${partition.size} iterations...")
    for ((indexUnit, node) in partition.withIndex()) {
        val timeStartIter = timeNow()
        reset()
        encodeAigs(aigLeft, aigRight)
        encodeMiter()

        val units = node.cube.mapIndexed { i, b -> varsShuffled[i] sign b }
        for (lit in units) {
            addClause(lit)
        }

        val k = units.size
        val leaves = node.dfs().drop(1).filter { it.isLeaf() }.toList()

        logger.info("Iteration #${indexUnit + 1}: ${leaves.size} leaves")
        if (leaves.isEmpty()) {
            logger.debug("Solving iteration #${indexUnit + 1}/${partition.size} with ${units.size} units and without assumptions...")
            val (res, timeSolve) = measureTimeWithResult {
                solve()
            }
            times.add(TimingInfo(indexUnit, 0, timeSolve.seconds))
            if (res) {
                logger.warn("Circuits are NOT equivalent!")
                return false
            }
        } else for ((indexLeaf, leaf) in leaves.withIndex()) {
            val assumptions = (k until leaf.cube.size).map { i -> varsShuffled[i] sign leaf.cube[i] }
            val (res, timeSolve) = measureTimeWithResult {
                val (isSat, isTimeout) = runWithTimeout2(30 * 1000) {
                    solve(assumptions.sorted())
                }
                if (isTimeout) {
                    logger.warn("Timeout on indexUnit=$indexUnit, indexLeaf=$indexLeaf")
                }
                isSat
            }
            if (timeSolve.seconds > 0.5) {
                logger.debug(
                    "Solved iteration #${indexUnit + 1}/${partition.size}, leaf ${indexLeaf + 1}/${leaves.size} with ${units.size} units and ${assumptions.size} assumptions in %.3fs"
                        .format(timeSolve.seconds)
                )
            }
            times.add(TimingInfo(indexUnit, indexLeaf, timeSolve.seconds))
            if (res) {
                logger.warn("Circuits are NOT equivalent!")
                return false
            }
        }
        logger.info {
            "Iteration #${indexUnit + 1}/${partition.size} done in %.3fs [total solve: %.3fs, total wall: %.3fs]"
                .format(secondsSince(timeStartIter), times.sumOf { it.time }, secondsSince(timeStartAll))
        }
    }
    logger.info("Max times:")
    for ((indexUnit, indexLeaf, time) in times.sortedByDescending { it.time }.take(100)) {
        logger.info("  - %.3fs on $indexUnit/$indexLeaf".format(time))
    }

    logger.info("Circuits are equivalent!")
    return true
}

internal fun Solver.`check circuits equivalence using method 12`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using method 12...")

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

    fun computeBuckets(
        aig: Aig,
        pTable: Map<Int, Double>,
        andGateValue: BoolVarArray,
        bucketSize: Int,
        numberOfBuckets: Int,
        idsSortedByDis: List<Int>,
    ): List<Bucket> {
        return idsSortedByDis.asSequence()
            .windowed(bucketSize, bucketSize)
            .take(numberOfBuckets)
            .mapIndexed { index, ids ->
                logger.info("Bucket #${index + 1} (size=${ids.size})")
                buildDecomposition(aig, pTable, andGateValue, ids)
            }
            // .sortedBy { it.saturation }
            .toList()
    }

    val bucketSize = 14
    val numberOfBuckets = 5

    logger.info("Computing Left buckets...")
    val bucketsLeft = computeBuckets(
        aigLeft,
        pTableLeft,
        andGateValueLeft,
        bucketSize,
        numberOfBuckets,
        idsSortedByDisLeft
    )
    logger.info("Computing Right buckets...")
    val bucketsRight = computeBuckets(
        aigRight,
        pTableRight,
        andGateValueRight,
        bucketSize,
        numberOfBuckets,
        idsSortedByDisRight
    )

    logger.info("Left buckets: ${bucketsLeft.size} = ${bucketsLeft.map { it.domain.size }}")
    logger.info("Right buckets: ${bucketsRight.size} = ${bucketsRight.map { it.domain.size }}")
    logger.info(
        "Estimated decomposition size: ${
            (bucketsLeft + bucketsRight).map { it.domain.size.toLong() }.reduce(Long::times)
        }"
    )

    declare(logger) {
        encodeMiter()
    }

    for (bucketIndex in listOf(0, 1, 2)) {
        logger.info("Bucket index = $bucketIndex")
        val bucketLeft = bucketsLeft[bucketIndex]
        val bucketRight = bucketsRight[bucketIndex]

        logger.info {
            "Left bucket: (lits: ${bucketLeft.lits.size}, domain: ${bucketLeft.domain.size}, saturation: %.3f%%))"
                .format(bucketLeft.saturation * 100.0)
        }
        logger.info {
            "Right bucket: (lits: ${bucketRight.lits.size}, domain: ${bucketRight.domain.size}, saturation: %.3f%%))"
                .format(bucketRight.saturation * 100.0)
        }

        val decomposition = bucketsDecomposition(listOf(bucketLeft, bucketRight))
            .map { lits -> lits.sortedBy { lit -> lit.absoluteValue } }
            .toList()
        logger.info("Total decomposition size: ${decomposition.size}")

        data class TimingInfo(
            val index: Int,
            val time: Double,
        )

        val timeStartAll = timeNow()
        val times: MutableList<TimingInfo> = mutableListOf()
        logger.info("Solving...")

        for ((index, assumptions) in decomposition.withIndex()) {
            val timeStartIter = timeNow()
            val (res, timeSolve) = measureTimeWithResult {
                val (isSat, isTimeout) = runWithTimeout2(10 * 1000) {
                    solve(assumptions)
                }
                if (isTimeout) {
                    logger.warn("Timeout on index=$index")
                }
                isSat
            }
            times.add(TimingInfo(index, timeSolve.seconds))
            if (res) {
                logger.warn("Circuits are NOT equivalent!")
                return false
            }
            logger.info {
                "Iteration #${index + 1}/${decomposition.size} done in %.3fs [total solve: %.3fs, total wall: %.3fs]"
                    .format(secondsSince(timeStartIter), times.sumOf { it.time }, secondsSince(timeStartAll))
            }
        }
        logger.info("Max times:")
        for ((index, time) in times.sortedByDescending { it.time }.take(50)) {
            logger.info("  - %.3fs on index=$index".format(time))
        }
    }

    logger.info("Circuits are equivalent!")
    return true
}

internal fun Solver.`check circuits equivalence using method 13`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using method 13...")

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

    declare(logger) {
        encodeMiter()
    }

    dumpDimacs(File("cnf-miter.cnf"))

    val Y: Int = context["Y"]
    check(isEven(Y))
    val xorValue: BoolVarArray = context["xorValue"]

    val F = Y / 2
    val mergedOutputsXorValue = newBoolVarArray(F)
    for (f in 1..F) {
        iffXor2(
            mergedOutputsXorValue[f],
            xorValue[2 * (f - 1) + 1],
            xorValue[2 * (f - 1) + 1 + 1],
        )
    }
    logger.debug { "mergedOutputsXorValue = ${mergedOutputsXorValue.values}" }

    // Freeze variables
    for (f in 1..F) {
        maybeFreeze(mergedOutputsXorValue[f])
    }

    data class TimingInfo(
        val t: Long,
        val time: Double,
    )

    val timeStartAll = timeNow()
    val times: MutableList<TimingInfo> = mutableListOf()
    logger.info("Solving...")
    val useAssumptions = false

    val fileIcnf = File("cubes.icnf")
    dumpDimacs(fileIcnf)
    fileIcnf.appendingSink().buffer().useWith {
        for (t in 0 until 2L.pow(F)) {
            val assumptions = (1..F).map { f ->
                mergedOutputsXorValue[f] sign t.bit(F - (f - 1) - 1)
            }
            writeln("a ${assumptions.joinToString(" ")} 0")
        }
    }
    // return true

    for (t in 0 until 2L.pow(F)) {
        if (!useAssumptions) {
            reset()
            encodeAigs(aigLeft, aigRight)
            encodeMiter()
            newBoolVarArray(F)
            for (f in 1..F) {
                iffXor2(
                    mergedOutputsXorValue[f],
                    xorValue[2 * (f - 1) + 1],
                    xorValue[2 * (f - 1) + 1 + 1],
                )
            }
        }

        val assumptions = (1..F).map { f ->
            mergedOutputsXorValue[f] sign t.bit(F - (f - 1) - 1)
        }

        val (res, timeSolve) = measureTimeWithResult {
            val (isSat, isTimeout) = runWithTimeout2(500 * 1000) {
                if (useAssumptions) {
                    solve(assumptions)
                } else {
                    for (lit in assumptions) {
                        addClause(lit)
                    }
                    solve()
                }
            }
            if (isTimeout) {
                logger.warn("Timeout on t=$t)")
            }
            isSat
        }
        times.add(TimingInfo(t, timeSolve.seconds))
        if (res) {
            logger.warn("Circuits are NOT equivalent!")
            return false
        }
        if (timeSolve.seconds > 1.0) {
            logger.info {
                "Iteration t=${t + 1}/${2L.pow(F)} done in %.3fs (total solve: %.3fs, total wall: %.3fs)"
                    .format(timeSolve.seconds, times.sumOf { it.time }, secondsSince(timeStartAll))
            }
            logger.debug { "t = $t, cube = $assumptions" }
        }
    }

    logger.info("Circuits are equivalent!")
    return true
}

internal fun Solver.`check circuits equivalence using method 14`(
    name: String,
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence for $name using method 14...")

    check("type" in globalOptions) { "Global option 'type' must be set for m14" }
    check("funs" in globalOptions) { "Global option 'funs' must be set for m14" }

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

    declare(logger) {
        encodeMiter()
    }

    // dumpDimacs(File("cnf-miter.cnf"))

    val X: Int = context["X"]
    check(isEven(X))
    val inputValue: BoolVarArray = context["inputValue"]

    // type: "pairs", "triples"
    // val type = globalOptions.getOrDefault("type", "pairs")
    val type = globalOptions.getValue("type")

    // funs: "xor", "and", "nand-or", "maj"
    // val funs = globalOptions.getOrDefault("funs", "xor")
    val funs = globalOptions.getValue("funs")

    val F = when (type) {
        "pairs" -> X / 2
        "triples" -> X / 3
        else -> error("Bad type '$type'")
    }
    logger.info("X = $X, F = $F, 2^F = ${2L.pow(F)}")
    val mergedInputValue = newBoolVarArray(F)
    for (f in 1..F) {
        when (type) {
            "pairs" -> {
                when (funs) {
                    "xor" -> {
                        iffXor2(
                            mergedInputValue[f],
                            inputValue[2 * (f - 1) + 1 + 0],
                            inputValue[2 * (f - 1) + 1 + 1],
                        )
                    }
                    "and" -> {
                        iffAnd(
                            mergedInputValue[f],
                            inputValue[2 * (f - 1) + 1 + 0],
                            inputValue[2 * (f - 1) + 1 + 1],
                        )
                    }
                    "nand-or" -> {
                        implyNand(
                            mergedInputValue[f],
                            inputValue[2 * (f - 1) + 1 + 0],
                            inputValue[2 * (f - 1) + 1 + 1],
                        )
                        implyOr(
                            -mergedInputValue[f],
                            inputValue[2 * (f - 1) + 1 + 0],
                            inputValue[2 * (f - 1) + 1 + 1],
                        )
                    }
                    else -> error("Bad functions '$funs' for type '$type'")
                }
            }
            "triples" -> {
                when (funs) {
                    "xor" -> {
                        iffXor3(
                            mergedInputValue[f],
                            inputValue[3 * (f - 1) + 1 + 0],
                            inputValue[3 * (f - 1) + 1 + 1],
                            inputValue[3 * (f - 1) + 1 + 2],
                        )
                    }
                    "and" -> {
                        iffAnd(
                            mergedInputValue[f],
                            inputValue[3 * (f - 1) + 1 + 0],
                            inputValue[3 * (f - 1) + 1 + 1],
                            inputValue[3 * (f - 1) + 1 + 2],
                        )
                    }
                    "nand-or" -> {
                        implyNand(
                            mergedInputValue[f],
                            inputValue[3 * (f - 1) + 1 + 0],
                            inputValue[3 * (f - 1) + 1 + 1],
                            inputValue[3 * (f - 1) + 1 + 2],
                        )
                        implyOr(
                            -mergedInputValue[f],
                            inputValue[3 * (f - 1) + 1 + 0],
                            inputValue[3 * (f - 1) + 1 + 1],
                            inputValue[3 * (f - 1) + 1 + 2],
                        )
                    }
                    "maj" -> {
                        iffMaj3(
                            mergedInputValue[f],
                            inputValue[3 * (f - 1) + 1 + 0],
                            inputValue[3 * (f - 1) + 1 + 1],
                            inputValue[3 * (f - 1) + 1 + 2],
                        )
                    }
                    else -> error("Bad functions '$funs' for type '$type'")
                }
            }
            else -> error("Bad type '$type'")
        }
    }
    logger.debug { "mergedInputValue = ${mergedInputValue.values}" }

    // val fileIcnf = File("dec_${name}_merge-in_${type}-${funs}.icnf")
    // dumpDimacs(fileIcnf)
    // fileIcnf.appendingSink().buffer().useWith {
    //     for (t in 0 until 2L.pow(F)) {
    //         val assumptions = (1..F).map { f ->
    //             mergedInputValue[f] sign t.bit(F - (f - 1) - 1)
    //         }
    //         writeln("a ${assumptions.joinToString(" ")} 0")
    //     }
    // }
    // return true

    val fileCnf = File("cnf_${name}_merge-in_${type}-${funs}.cnf")
    dumpDimacs(fileCnf)
    fileCnf.appendingSink().buffer().useWith {
        writeln("c mergedInputValue: ${mergedInputValue.shape}")
        writeln("c mergedInputValue = ${mergedInputValue.values}")
    }
    return true

    // Freeze variables
    for (f in 1..F) {
        maybeFreeze(mergedInputValue[f])
    }

    data class TimingInfo(
        val t: Long,
        val time: Double,
    )

    val timeStartAll = timeNow()
    val times: MutableList<TimingInfo> = mutableListOf()
    logger.info("Solving...")

    for (t in 0 until 2L.pow(F)) {
        val assumptions = (1..F).map { f ->
            mergedInputValue[f] sign t.bit(F - (f - 1) - 1)
        }

        val (res, timeSolve) = measureTimeWithResult {
            val (isSat, isTimeout) = runWithTimeout2(500 * 1000) {
                solve(assumptions)
            }
            if (isTimeout) {
                logger.warn("Timeout on t=$t)")
            }
            isSat
        }
        times.add(TimingInfo(t, timeSolve.seconds))
        if (res) {
            logger.warn("Circuits are NOT equivalent!")
            return false
        }
        if (timeSolve.seconds > 1.0) {
            logger.info {
                "Iteration t=${t + 1}/${2L.pow(F)} done in %.3fs (total solve: %.3fs, total wall: %.3fs)"
                    .format(timeSolve.seconds, times.sumOf { it.time }, secondsSince(timeStartAll))
            }
            logger.debug { "t = $t, cube = $assumptions" }
        }
    }
    logger.info("Max times:")
    for ((t, time) in times.sortedByDescending { it.time }.take(50)) {
        logger.info("  - %.3fs on index=$t".format(time))
    }

    logger.info("Circuits are equivalent!")
    return true
}

internal fun Solver.`check circuits equivalence using method 15`(
    name: String,
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence for $name using method 15...")

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

    declare(logger) {
        encodeMiter()
    }

    // dumpDimacs(File("cnf-miter.cnf"))

    val X: Int = context["X"]
    check(isEven(X))
    val inputValue: BoolVarArray = context["inputValue"]

    // type: "pairs", "triples"
    val type = "triples"

    // funs1: "xor", "and", "nand-or", "maj"
    // Note: "maj" is only for "triples"
    val funs1 = "maj"

    val F = when (type) {
        "pairs" -> X / 2
        "triples" -> X / 3
        else -> error("Bad type '$type'")
    }
    logger.info("X = $X, F = $F, 2^F = ${2L.pow(F)}")
    val mergedInputValue = newBoolVarArray(F)
    for (f in 1..F) {
        when (type) {
            "pairs" -> {
                when (funs1) {
                    "xor" -> {
                        iffXor2(
                            mergedInputValue[f],
                            inputValue[2 * (f - 1) + 1 + 0],
                            inputValue[2 * (f - 1) + 1 + 1],
                        )
                    }
                    "and" -> {
                        iffAnd(
                            mergedInputValue[f],
                            inputValue[2 * (f - 1) + 1 + 0],
                            inputValue[2 * (f - 1) + 1 + 1],
                        )
                    }
                    "nand-or" -> {
                        implyNand(
                            mergedInputValue[f],
                            inputValue[2 * (f - 1) + 1 + 0],
                            inputValue[2 * (f - 1) + 1 + 1],
                        )
                        implyOr(
                            -mergedInputValue[f],
                            inputValue[2 * (f - 1) + 1 + 0],
                            inputValue[2 * (f - 1) + 1 + 1],
                        )
                    }
                    else -> error("Bad functions '$funs1' for type '$type'")
                }
            }
            "triples" -> {
                when (funs1) {
                    "xor" -> {
                        iffXor3(
                            mergedInputValue[f],
                            inputValue[3 * (f - 1) + 1 + 0],
                            inputValue[3 * (f - 1) + 1 + 1],
                            inputValue[3 * (f - 1) + 1 + 2],
                        )
                    }
                    "and" -> {
                        iffAnd(
                            mergedInputValue[f],
                            inputValue[3 * (f - 1) + 1 + 0],
                            inputValue[3 * (f - 1) + 1 + 1],
                            inputValue[3 * (f - 1) + 1 + 2],
                        )
                    }
                    "nand-or" -> {
                        implyNand(
                            mergedInputValue[f],
                            inputValue[3 * (f - 1) + 1 + 0],
                            inputValue[3 * (f - 1) + 1 + 1],
                            inputValue[3 * (f - 1) + 1 + 2],
                        )
                        implyOr(
                            -mergedInputValue[f],
                            inputValue[3 * (f - 1) + 1 + 0],
                            inputValue[3 * (f - 1) + 1 + 1],
                            inputValue[3 * (f - 1) + 1 + 2],
                        )
                    }
                    "maj" -> {
                        iffMaj3(
                            mergedInputValue[f],
                            inputValue[3 * (f - 1) + 1 + 0],
                            inputValue[3 * (f - 1) + 1 + 1],
                            inputValue[3 * (f - 1) + 1 + 2],
                        )
                    }
                    else -> error("Bad functions '$funs1' for type '$type'")
                }
            }
            else -> error("Bad type '$type'")
        }
    }
    logger.debug { "mergedInputValue = ${mergedInputValue.values}" }

    // funs2: "xor", "and", "nand-or"
    val funs2 = "and"

    val G = F / 2
    logger.info("X = $X, G = $G, 2^G = ${2L.pow(G)}")
    val mergedMergersValue = newBoolVarArray(G)
    for (g in 1..G) {
        when (funs2) {
            "xor" -> {
                iffXor2(
                    mergedMergersValue[g],
                    mergedInputValue[2 * (g - 1) + 1 + 0],
                    mergedInputValue[2 * (g - 1) + 1 + 1],
                )
            }
            "and" -> {
                iffAnd(
                    mergedMergersValue[g],
                    mergedInputValue[2 * (g - 1) + 1 + 0],
                    mergedInputValue[2 * (g - 1) + 1 + 1],
                )
            }
            "nand-or" -> {
                implyNand(
                    mergedMergersValue[g],
                    mergedInputValue[2 * (g - 1) + 1 + 0],
                    mergedInputValue[2 * (g - 1) + 1 + 1],
                )
                implyOr(
                    -mergedMergersValue[g],
                    mergedInputValue[2 * (g - 1) + 1 + 0],
                    mergedInputValue[2 * (g - 1) + 1 + 1],
                )
            }
            else -> error("Bad functions '$funs2'")
        }
    }

    val fileIcnf = File("decomposition_merged-inputs_${type}-${funs1}_pairs-${funs2}.icnf")
    dumpDimacs(fileIcnf)
    fileIcnf.appendingSink().buffer().useWith {
        for (t in 0 until 2L.pow(G)) {
            val assumptions = (1..G).map { g ->
                mergedMergersValue[g] sign t.bit(G - (g - 1) - 1)
            }
            writeln("a ${assumptions.joinToString(" ")} 0")
        }
    }
    return true

    // Freeze variables
    for (f in 1..F) {
        maybeFreeze(mergedInputValue[f])
    }

    data class TimingInfo(
        val t: Long,
        val time: Double,
    )

    val timeStartAll = timeNow()
    val times: MutableList<TimingInfo> = mutableListOf()
    logger.info("Solving...")

    for (t in 0 until 2L.pow(F)) {
        val assumptions = (1..F).map { f ->
            mergedInputValue[f] sign t.bit(F - (f - 1) - 1)
        }

        val (res, timeSolve) = measureTimeWithResult {
            val (isSat, isTimeout) = runWithTimeout2(500 * 1000) {
                solve(assumptions)
            }
            if (isTimeout) {
                logger.warn("Timeout on t=$t)")
            }
            isSat
        }
        times.add(TimingInfo(t, timeSolve.seconds))
        if (res) {
            logger.warn("Circuits are NOT equivalent!")
            return false
        }
        if (timeSolve.seconds > 1.0) {
            logger.info {
                "Iteration t=${t + 1}/${2L.pow(F)} done in %.3fs (total solve: %.3fs, total wall: %.3fs)"
                    .format(timeSolve.seconds, times.sumOf { it.time }, secondsSince(timeStartAll))
            }
            logger.debug { "t = $t, cube = $assumptions" }
        }
    }
    logger.info("Max times:")
    for ((t, time) in times.sortedByDescending { it.time }.take(50)) {
        logger.info("  - %.3fs on index=$t".format(time))
    }

    logger.info("Circuits are equivalent!")
    return true
}

internal fun Solver.`check circuits equivalence using bucket-decomposition with balanced gates`(
    name:String,
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    logger.info("Checking equivalence using disbalance-based decomposition (with Trie)...")

    val sampleSize = 10000
    val randomSeed = 42
    val random = Random(randomSeed)
    logger.info("Computing p-tables using sampleSize=$sampleSize and randomSeed=$randomSeed...")
    val pTableLeft = aigLeft.computePTable(sampleSize, random)
    val pTableRight = aigRight.computePTable(sampleSize, random)
    // val idsSortedByPLeft = aigLeft.mapping.keys.sortedBy { id -> pTableLeft.getValue(id) }
    // val idsSortedByPRight = aigRight.mapping.keys.sortedBy { id -> pTableRight.getValue(id) }
    val idsSortedByDisLeft = aigLeft.mapping.keys.sortedBy { id ->
        val mid = when {
            id in aigLeft.inputIds -> 0.5
            id in aigLeft.andGateIds -> 0.5
            else -> error("Bad gate with id=$id")
        }
        disbalance(pTableLeft.getValue(id), midpoint = mid)
    }
    val idsSortedByDisRight = aigRight.mapping.keys.sortedBy { id ->
        val mid = when {
            id in aigRight.inputIds -> 0.5
            id in aigRight.andGateIds -> 0.5
            else -> error("Bad gate with id=$id")
        }
        disbalance(pTableRight.getValue(id), midpoint = mid)
    }

    declare(logger) {
        encodeAigs(aigLeft, aigRight)
    }

    val GL: Int = context["left.G"]
    val GR: Int = context["right.G"]
    val inputValue: BoolVarArray = context["inputValue"]
    val andGateValueLeft: BoolVarArray = context["left.andGateValue"]
    val andGateValueRight: BoolVarArray = context["right.andGateValue"]

    // Freeze variables
    for (g in 1..GL) {
        maybeFreeze(andGateValueLeft[g])
    }
    for (g in 1..GR) {
        maybeFreeze(andGateValueRight[g])
    }

    logger.info("Variables in Left AIG sorted by disbalance:")
    for (i in 0 until 50) {
        val id = idsSortedByDisLeft[i]
        val lit = if (id in aigLeft.inputIds) {
            inputValue[aigLeft.inputIds.indexOf(id) + 1]
        } else {
            andGateValueLeft[aigLeft.andGateIds.indexOf(id) + 1]
        }
        val mid = when {
            id in aigLeft.inputIds -> 0.5
            // id in aigLeft.andGateIds -> 0.25
            id in aigLeft.andGateIds -> 0.5
            else -> error("Bad gate with id=$id")
        }
        val dis = disbalance(pTableLeft.getValue(id), midpoint = mid)
        println("[$i] (${if (id in aigLeft.inputIds) "input" else "gate"}) id: $id, lit: $lit, dis: %.3f".format(dis))
    }
    logger.info("Variables in Right AIG sorted by disbalance:")
    for (i in 0 until 50) {
        val id = idsSortedByDisRight[i]
        val lit = if (id in aigRight.inputIds) {
            inputValue[aigRight.inputIds.indexOf(id) + 1]
        } else {
            andGateValueRight[aigRight.andGateIds.indexOf(id) + 1]
        }
        val mid = when {
            id in aigRight.inputIds -> 0.5
            // id in aigRight.andGateIds -> 0.25
            id in aigRight.andGateIds -> 0.5
            else -> error("Bad gate with id=$id")
        }
        val dis = disbalance(pTableRight.getValue(id), midpoint = mid)
        println("[$i] (${if (id in aigRight.inputIds) "input" else "gate"}) id: $id, lit: $lit, dis: %.3f".format(dis))
    }

    logger.info("Pre-solving...")
    val (isSatMain, timeSolveMain) = measureTimeWithResult { solve() }
    logger.info("${if (isSatMain) "SAT" else "UNSAT"} in %.3fs".format(timeSolveMain.seconds))
    if (!isSatMain) error("Unexpected UNSAT")

    val bucketSize = 5
    val numberOfBuckets = Pair(1, 1)
    // val numberOfBucketsCompute = (100 / bucketSize)
    val numberOfBucketsCompute = 5

    val bucketsLeft = run {
        idsSortedByDisLeft.asSequence()
            .filter { it in aigLeft.andGateIds }
            .windowed(bucketSize, bucketSize)
            .take(numberOfBucketsCompute)
            .mapIndexed { index, ids ->
                logger.info("(Left) Bucket #${index + 1} (size=${ids.size})")
                buildDecomposition(aigLeft, pTableLeft, andGateValueLeft, ids)
            }
            .sortedBy { -it.saturation }
            // .drop(numberOfBuckets.first)
            .take(numberOfBuckets.first)
            .toList()
    }

    val bucketsRight = run {
        idsSortedByDisRight.asSequence()
            .filter { it in aigRight.andGateIds }
            .windowed(bucketSize, bucketSize)
            .take(numberOfBucketsCompute)
            .mapIndexed { index, ids ->
                logger.info("(Right) Bucket #${index + 1} (size=${ids.size})")
                buildDecomposition(aigRight, pTableRight, andGateValueRight, ids)
            }
            .sortedBy { -it.saturation }
            // .drop(numberOfBuckets.second)
            .take(numberOfBuckets.second)
            .toList()
    }

    logger.info("Left buckets: ${bucketsLeft.size} = ${
        bucketsLeft.joinToString("+") { it.domain.size.toString() }
    } (${
        bucketsLeft.joinToString("+") { "%.3f%%".format(it.saturation) }
    })")
    logger.info("Right buckets: ${bucketsRight.size} = ${
        bucketsRight.joinToString("+") { it.domain.size.toString() }
    } (${
        bucketsRight.joinToString("+") { "%.3f%%".format(it.saturation) }
    })")
    logger.info(
        "Estimated decomposition size: ${
            (bucketsLeft + bucketsRight).map { it.domain.size.toLong() }.reduce(Long::times)
        }"
    )

    val megaBucketLeft = mergeBucketsTree(bucketsLeft)
    val megaBucketRight = mergeBucketsTree(bucketsRight)

    logger.info("Building decomposition of size ${megaBucketLeft.domain.size.toLong() * megaBucketRight.domain.size}...")
    val decomposition = bucketsDecomposition(listOf(megaBucketLeft, megaBucketRight))
        .map { it.sortedBy { it.absoluteValue } }
    // logger.info("Total decomposition size: ${decomposition.size}")

    // ============
    // val fileIcnf = File("cnf-with-cubes.icnf")
    // CadicalSolver().useWith {
    //     encodeAigs(aigLeft, aigRight)
    //     encodeMiter()
    //     dumpDimacs(fileIcnf)
    //     fileIcnf.appendingSink().buffer().useWith {
    //         for (cube in decomposition) {
    //             writeln("a ${cube.joinToString(" ")} 0")
    //         }
    //     }
    // }
    val fileCnf = File("cnf_${name}_miter.cnf")
    val variables = (megaBucketLeft.lits + megaBucketRight.lits).toSet().sorted()
    CadicalSolver().useWith {
        encodeAigs(aigLeft, aigRight)
        encodeMiter()
        dumpDimacs(fileCnf)
        fileCnf.appendingSink().buffer().useWith {
            writeln("c vars = $variables")
        }
    }
    val fileCubes = File("cubes_${name}_balanced_${bucketSize}_${bucketSize}.txt")
    fileCubes.sink().buffer().useWith {
        for (cube in decomposition) {
            writeln(cube.joinToString(" "))
        }
    }
    return true
    // ============

    logger.info("Shuffling...")
    val n = megaBucketLeft.lits.size + megaBucketRight.lits.size
    val permutation = (0 until n).shuffled(Random(42))
    val decompositionShuffled = decomposition.map { lits ->
        permutation.map { i -> lits[i] }
    }
    var varsShuffled: List<Lit> = emptyList()
    val cubes = decompositionShuffled.map { lits ->
        varsShuffled = lits.map { lit -> lit.absoluteValue }
        lits.map { lit -> lit > 0 }
    }
    // val varsShuffled = decompositionShuffled.first().map { lit -> lit.absoluteValue }

    logger.info("Building trie...")
    val trie = buildTrie(cubes.asIterable())
    logger.info("Done building trie")

    val limit = 1
    logger.info("Performing trie.dfsLimited($limit)...")
    val partition = trie.dfsLimited(limit).toList()
    println("trie.dfsLimited($limit): (total ${partition.size})")
    for (node in partition.take(10)) {
        println("  - ${node.cube.toBinaryString().padEnd(n, '.')} (leaves: ${node.leaves})")
    }

    // // =====
    // encodeMiter()
    // logger.info("Dumping hard instances...")
    // // val hard = listOf(
    // //     50 to 0,
    // //     51 to 6,
    // //     51 to 21,
    // //     51 to 32,
    // //     51 to 52,
    // //     51 to 74,
    // //     53 to 6,
    // //     385 to 20,
    // // )
    // val hard = listOf(
    //     542 to 120,
    //     542 to 525,
    //     1057 to 151,
    //     542 to 430,
    //     560 to 201,
    //     542 to 481,
    //     542 to 520,
    //     1057 to 250,
    //     2080 to 320,
    //     1375 to 520,
    // )
    // val hardCubes: MutableList<List<Lit>> = mutableListOf()
    // val hardDir = File("data/hard2")
    // hardDir.mkdirs()
    // for ((indexUnit, indexLeaf) in hard) {
    //     val node = partition[indexUnit]
    //     val units = node.cube.mapIndexed { i, b -> varsShuffled[i] sign b }
    //     val leaves = node.dfs().drop(1).filter { it.isLeaf() }.toList()
    //     val leaf = leaves[indexLeaf]
    //     val assumptions = (units.size until leaf.cube.size).map { i -> varsShuffled[i] sign leaf.cube[i] }
    //     val fileCnf = hardDir.resolve("cnf_hard_${indexUnit}_${indexLeaf}.cnf")
    //     logger.info("Dumping hard instance $indexUnit/$indexLeaf to '$fileCnf'...")
    //     dumpDimacs(fileCnf)
    //     fileCnf.appendingSink().buffer().useWith {
    //         writeln("c Units (${units.size})")
    //         for (lit in units) {
    //             writeln("$lit 0")
    //         }
    //         writeln("c Assumptions (${assumptions.size})")
    //         for (lit in assumptions) {
    //             writeln("$lit 0")
    //         }
    //     }
    //     hardCubes.add(units + assumptions)
    // }
    //
    // logger.info("Hard cubes:")
    // for (cube in hardCubes) {
    //     logger.info(cube.map { it > 0 }.toBinaryString())
    // }
    // // return true
    //
    // // logger.info("Encoding DNF...")
    // // encodeDnf(hardCubes)
    // // val fileCnfMerged = hardDir.resolve("cnf_hard_merged.cnf")
    // // dumpDimacs(fileCnfMerged)
    // // // =====
    // return true

    data class TimingInfo(
        val indexUnit: Int,
        val indexLeaf: Int,
        val time: Double,
    )

    val timeStartAll = timeNow()
    val times: MutableList<TimingInfo> = mutableListOf()
    logger.info("Solving in ${partition.size} iterations...")

    for ((indexUnit, node) in partition.withIndex()) {
        val timeStartIter = timeNow()
        reset()
        encodeAigs(aigLeft, aigRight)
        encodeMiter()

        val numberOfConflictsStart = maybeNumberOfConflicts()

        val units = node.cube.mapIndexed { i, b -> varsShuffled[i] sign b }
        for (lit in units) {
            addClause(lit)
        }

        val k = units.size
        val leaves = node.dfs().drop(1).filter { it.isLeaf() }.toList()

        logger.info("Iteration #${indexUnit + 1}: ${leaves.size} leaves")
        if (leaves.isEmpty()) {
            logger.debug("Solving iteration #${indexUnit + 1}/${partition.size} with ${units.size} units and without assumptions...")
            val (res, timeSolve) = measureTimeWithResult {
                solve()
            }
            times.add(TimingInfo(indexUnit, 0, timeSolve.seconds))
            if (res) {
                logger.warn("Circuits are NOT equivalent!")
                return false
            }
        } else for ((indexLeaf, leaf) in leaves.withIndex()) {
            val assumptions = (k until leaf.cube.size).map { i -> varsShuffled[i] sign leaf.cube[i] }
            val (res, timeSolve) = measureTimeWithResult {
                val (isSat, isTimeout) = runWithTimeout2(30 * 1000) {
                    solve(assumptions.sorted())
                }
                if (isTimeout) {
                    logger.warn("Timeout on indexUnit=$indexUnit, indexLeaf=$indexLeaf")
                }
                isSat
            }
            if (timeSolve.seconds > 0.5) {
                logger.debug(
                    "Solved iteration #${indexUnit + 1}/${partition.size}, leaf ${indexLeaf + 1}/${leaves.size} with ${units.size} units and ${assumptions.size} assumptions in %.3fs"
                        .format(timeSolve.seconds)
                )
            }
            times.add(TimingInfo(indexUnit, indexLeaf, timeSolve.seconds))
            if (res) {
                logger.warn("Circuits are NOT equivalent!")
                return false
            }
        }
        val confls = maybeNumberOfConflicts() - numberOfConflictsStart
        logger.info {
            "Iteration #${indexUnit + 1}/${partition.size} done in %.3fs [total solve: %.3fs, total wall: %.3fs] confls=${confls}"
                .format(secondsSince(timeStartIter), times.sumOf { it.time }, secondsSince(timeStartAll))
        }
        // if (indexUnit == 0 || (indexUnit + 1) % 100 == 0 || secondsSince(timeStartIter) > 2) {
        //     logger.info {
        //         "Iteration #${indexUnit + 1}/${partition.size} done in %.3fs [total solve: %.3fs, total wall: %.3fs]"
        //             .format(secondsSince(timeStartIter), times.sumOf { it.time }, secondsSince(timeStartAll))
        //     }
        // }
    }
    logger.info("Max times:")
    for ((indexUnit, indexLeaf, time) in times.sortedByDescending { it.time }.take(100)) {
        logger.info("  - %.3fs on $indexUnit/$indexLeaf".format(time))
    }

    logger.info("Circuits are equivalent!")
    return true
}

fun checkEquivalence(
    name: String,
    aigLeft: Aig,
    aigRight: Aig,
    solverProvider: () -> Solver,
    method: String,
): Boolean {
    logger.info("Preparing to check the equivalence for $name using '$method' method...")
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
            "dec-dis-trie" -> `check circuits equivalence using disbalance-based decomposition with trie`(
                aigLeft,
                aigRight
            )
            "dec-dis-dnf" -> `check circuits equivalence using disbalance-based decomposition with dnf`(
                aigLeft,
                aigRight
            )
            "dec-layer" -> `check circuits equivalence using layer-wise decomposition`(aigLeft, aigRight)
            "m10" -> `check circuits equivalence using method 10`(aigLeft, aigRight)
            "m11" -> `check circuits equivalence using method 11`(aigLeft, aigRight)
            "m12" -> `check circuits equivalence using method 12`(aigLeft, aigRight)
            "m13" -> `check circuits equivalence using method 13`(aigLeft, aigRight)
            "m14" -> `check circuits equivalence using method 14`(name, aigLeft, aigRight)
            "m15" -> `check circuits equivalence using method 15`(name, aigLeft, aigRight)
            "domain" -> `check circuits equivalence using domain-based method`(aigLeft, aigRight)
            "dec-bal" -> `check circuits equivalence using bucket-decomposition with balanced gates`(name,aigLeft, aigRight)
            else -> TODO("Method '$method'")
        }
    }
}

internal fun loadPTable(aig: Aig, path: String) {
    loadPTable(aig, Paths.get(path))
}

internal fun loadPTable(aig: Aig, path: Path) {
    if (path.exists()) {
        logger.debug { "Loading p-table from '$path'..." }
        val pTable = path.source().buffer().inputStream().use { input ->
            Json.decodeFromStream<Map<Int, Double>>(input)
        }
        aig.precomputedPTable = pTable
    } else {
        logger.debug { "Not found p-table at '$path'..." }
    }
}

fun main() {
    val timeStart = timeNow()

    val left = "BubbleSort"
    val right = "PancakeSort"
    val param = "8_4"
    val name = "BvP_$param"
    val aag = "fraag" // "aag" or "fraag"

    val nameLeft = "${left}_${param}"
    val nameRight = "${right}_${param}"
    val filenameLeft = "data/instances/$left/$aag/$nameLeft.aag"
    val filenameRight = "data/instances/$right/$aag/$nameRight.aag"

    val aigLeft = parseAig(filenameLeft)
    val aigRight = parseAig(filenameRight)
    // val solverProvider = { MiniSatSolver() }
    // val solverProvider = { GlucoseSolver() }
    // val solverProvider = { CryptoMiniSatSolver() }
    val solverProvider = { CadicalSolver() }
    // val method = "m14"
    // val method = "dec-bal"
    val method = "dec-dis-trie"

    globalOptions["type"] = "pairs"
    globalOptions["funs"] = "xor"

    loadPTable(aigLeft, "data/instances/$left/ptable/${nameLeft}_$aag.json")
    loadPTable(aigRight, "data/instances/$right/ptable/${nameRight}_$aag.json")

    checkEquivalence(name, aigLeft, aigRight, solverProvider, method)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
