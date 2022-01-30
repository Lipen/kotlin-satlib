@file:Suppress("LocalVariableName", "FunctionName")

package com.github.lipen.satlib.nexus.eqcheck

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.utils.declare
import com.github.lipen.satlib.nexus.utils.iffXor2
import com.github.lipen.satlib.nexus.utils.maybeFreeze
import com.github.lipen.satlib.nexus.utils.maybeMelt
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.nexus.utils.toInt
import com.github.lipen.satlib.op.iffAnd
import com.github.lipen.satlib.op.iffIff
import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.solve
import com.github.lipen.satlib.utils.useWith
import com.soywiz.klock.measureTimeWithResult
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private fun Solver.encodeAig(
    name: String,
    aig: Aig,
    reuse: String? = null,
) {
    /* Constants */

    context["$name.aig"] = aig
    // val nodes = context("$name.nodes") { aig.mapping.values.map { it.id } }
    val nodes = context("$name.nodes") { aig.layers().flatten().toList() }
    fun index2id(i: Int) = nodes[i - 1]
    fun id2index(id: Int) = nodes.indexOf(id) + 1
    val V = context("$name.V") { nodes.size }
    val X = context("$name.X") { aig.inputs.size }
    val Y = context("$name.Y") { aig.outputs.size }

    logger.info("$name: V = $V, X = $Y, Y = $Y")

    // TODO: remove
    check(nodes.take(X).toSet() == aig.inputs.take(X).map { it.id }.toSet())

    /* Variables */

    val nodeValue = context("$name.nodeValue") {
        if (reuse != null) {
            val nodesReuse: List<Int> = context["$reuse.nodes"]
            val nodeValueReuse: BoolVarArray = context["$reuse.nodeValue"]
            newBoolVarArray(V) { (v) ->
                if (v > X) newLiteral()
                else nodeValueReuse[nodesReuse.indexOf(index2id(v)) + 1]
            }
        } else {
            newBoolVarArray(V)
        }
    }

    /* Constraints */

    comment("AND gate semantics")
    for (v in (X + 1)..V) {
        val gate = aig.andGate(index2id(v))
        check(id2index(gate.id) == v)
        iffAnd(
            nodeValue[v],
            nodeValue[id2index(gate.left.id)] sign !gate.left.negated,
            nodeValue[id2index(gate.right.id)] sign !gate.right.negated,
        )
    }
}

private fun Solver.encodeAigs(
    aigLeft: Aig,
    aigRight: Aig,
) {
    require(aigLeft.inputs.size == aigRight.inputs.size)
    require(aigLeft.outputs.size == aigRight.outputs.size)

    encodeAig("left", aigLeft)
    encodeAig("right", aigRight, reuse = "left")
    context["X"] = context["left.X"]
    context["Y"] = context["left.Y"]
}

private fun Solver.encodeMiter() {
    /* Constants */

    val aigLeft: Aig = context["left.aig"]
    val nodesLeft: List<Int> = context["left.nodes"]
    fun idLeft2index(id: Int) = nodesLeft.indexOf(id) + 1

    val aigRight: Aig = context["right.aig"]
    val nodesRight: List<Int> = context["right.nodes"]
    fun idRight2index(id: Int) = nodesRight.indexOf(id) + 1

    val Y: Int = context["Y"]

    /* Variables */

    val nodeValueLeft: BoolVarArray = context["left.nodeValue"]
    val nodeValueRight: BoolVarArray = context["right.nodeValue"]
    val xorValue = context("xorValue") {
        newBoolVarArray(Y)
    }

    /* Constraints */

    comment("Miter XORs")
    for (y in 1..Y) {
        val outputLeft = aigLeft.outputs[y - 1]
        val outputRight = aigRight.outputs[y - 1]
        iffXor2(
            xorValue[y],
            nodeValueLeft[idLeft2index(outputLeft.id)] sign !outputLeft.negated,
            nodeValueRight[idRight2index(outputRight.id)] sign !outputRight.negated,
        )
    }

    comment("Miter OR")
    addClause((1..Y).map { y -> xorValue[y] })
}

private fun Solver.encodeOutputMergers(type: String) {
    require(type in listOf("EQ", "XOR"))

    /* Constants */

    val aigLeft: Aig = context["left.aig"]
    val nodesLeft: List<Int> = context["left.nodes"]
    fun idLeft2index(id: Int) = nodesLeft.indexOf(id) + 1

    val aigRight: Aig = context["right.aig"]
    val nodesRight: List<Int> = context["right.nodes"]
    fun idRight2index(id: Int) = nodesRight.indexOf(id) + 1

    val Y: Int = context["Y"]

    /* Variables */

    val nodeValueLeft: BoolVarArray = context["left.nodeValue"]
    val nodeValueRight: BoolVarArray = context["right.nodeValue"]
    val mergerValue = context("mergerValue") {
        newBoolVarArray(Y)
    }

    /* Constraints */

    comment("Merge outputs using $type")
    for (y in 1..Y) {
        val outputLeft = aigLeft.outputs[y - 1]
        val outputRight = aigRight.outputs[y - 1]
        val left = nodeValueLeft[idLeft2index(outputLeft.id)] sign !outputLeft.negated
        val right = nodeValueRight[idRight2index(outputRight.id)] sign !outputRight.negated
        when (type) {
            "EQ" -> iffIff(mergerValue[y], left, right)
            "XOR" -> iffXor2(mergerValue[y], left, right)
            else -> error("Bad type '$type'")
        }
    }
}

private fun Solver.`check equivalence using naive miter`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
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

private fun Solver.`check equivalence using output mergers`(
    aigLeft: Aig,
    aigRight: Aig,
    type: String, // "EQ" or "XOR"
): Boolean {
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

private fun Solver.`check equivalence using conjugated tables`(
    aigLeft: Aig,
    aigRight: Aig,
): Boolean {
    declare(logger) {
        encodeAigs(aigLeft, aigRight)
    }

    /* Constants */

    val nodesLeft: List<Int> = context["left.nodes"]
    fun idLeft2index(id: Int) = nodesLeft.indexOf(id) + 1
    val nodeValueLeft: BoolVarArray = context["left.nodeValue"]

    val nodesRight: List<Int> = context["right.nodes"]
    fun idRight2index(id: Int) = nodesRight.indexOf(id) + 1
    val nodeValueRight: BoolVarArray = context["right.nodeValue"]

    val Y: Int = context["Y"]

    // Freeze assumptions
    for (y in 1..Y) {
        maybeFreeze(nodeValueLeft[idLeft2index(aigLeft.outputs[y - 1].id)])
        maybeFreeze(nodeValueRight[idRight2index(aigRight.outputs[y - 1].id)])
    }

    logger.info("Pre-solving...")
    val (isSatMain, timeSolveMain) = measureTimeWithResult {
        solve()
    }
    logger.info("${if (isSatMain) "SAT" else "UNSAT"} in %.3fs".format(timeSolveMain.seconds))

    if (!isSatMain) {
        error("Unexpected UNSAT")
    } else {
        logger.info("Calculating a conjugated table for each output...")
        for (y in 1..Y) {
            val outputLeft = aigLeft.outputs[y - 1]
            val outputRight = aigRight.outputs[y - 1]
            val left = nodeValueLeft[idLeft2index(outputLeft.id)] sign !outputLeft.negated
            val right = nodeValueRight[idRight2index(outputRight.id)] sign !outputRight.negated

            // // logger.info("Calculating conjugated table for y = $y...")
            // val (table, timeCalc) = measureTimeWithResult {
            //     calculateConjugatedTable(left, right).toBinaryString()
            // }
            // logger.info("Calculated conjugated table $table for y=$y in %.3fs".format(timeCalc.seconds))
            //
            // if (table != "1001") {
            //     logger.warn("Circuits are NOT equivalent!")
            //     return false
            // }

            // Conjugated table for equal outputs: '1001'

            // logger.info("Calculating conjugated table for y = $y...")
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
                    // logger.debug { "s1 = $s1, s2 = $s2, res = $res" }
                    logger.warn("Circuits are NOT equivalent! solve(${s1.toInt()}${s2.toInt()}) = ${res.toInt()}")
                    return false
                }
            }
            logger.info("Determined the equality of outputs y=$y in %.3fs".format(secondsSince(timeStartConj)))

            // Un-freeze assumptions
            maybeMelt(left)
            maybeMelt(right)
        }

        logger.info("Circuits are equivalent!")
        return true
    }
}

fun checkEquivalence(
    aigLeft: Aig,
    aigRight: Aig,
    solverProvider: () -> Solver,
    method: String,
): Boolean {
    logger.info("Checking for equivalence using '$method' method")
    logger.info("Left circuit: $aigLeft")
    logger.info("Right circuit: $aigRight")

    require(aigLeft.inputs.size == aigRight.inputs.size)
    require(aigLeft.outputs.size == aigRight.outputs.size)

    solverProvider().useWith {
        logger.info("Using $this")

        return when (method) {
            "miter" -> `check equivalence using naive miter`(aigLeft, aigRight)
            "merge-eq" -> `check equivalence using output mergers`(aigLeft, aigRight, "EQ")
            "merge-xor" -> `check equivalence using output mergers`(aigLeft, aigRight, "XOR")
            "conj" -> `check equivalence using conjugated tables`(aigLeft, aigRight)
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
    val solverProvider = { MiniSatSolver() }
    // Methods: "miter", "merge-eq", "merge-xor", "conj"
    val method = "conj"

    checkEquivalence(aigLeft, aigRight, solverProvider, method)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
