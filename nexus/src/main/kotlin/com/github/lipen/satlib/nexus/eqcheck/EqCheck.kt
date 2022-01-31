@file:Suppress("LocalVariableName", "FunctionName")

package com.github.lipen.satlib.nexus.eqcheck

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.AigAndGate
import com.github.lipen.satlib.nexus.aig.AigInput
import com.github.lipen.satlib.nexus.aig.Ref
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
    aig: Aig,
    name: String,
    reuse: String? = null,
) {
    /* Constants */

    context["$name.aig"] = aig
    val X = context("$name.X") { aig.inputs.size }
    val Y = context("$name.Y") { aig.outputs.size }
    val G = context("$name.V") { aig.andGates.size }
    logger.info("$name: X = $Y, Y = $Y, G = $G")

    fun input(x: Int): AigInput = aig.inputs[x - 1]
    fun output(y: Int): Ref = aig.outputs[y - 1]
    fun andGate(g: Int): AigAndGate = aig.andGates[g - 1] // FIXME: order?

    /* Variables */

    val inputValue = context("$name.inputValue") {
        if (reuse == null) {
            newBoolVarArray(X)
        } else {
            context["$reuse.inputValue"]
        }
    }
    val andGateValue = context("$name.andGateValue") {
        newBoolVarArray(G)
    }

    fun nodeValue(id: Int): Lit {
        val node = aig.node(id)
        return when (node) {
            is AigInput -> inputValue[aig.inputs.indexOf(node) + 1]
            is AigAndGate -> andGateValue[aig.andGates.indexOf(node) + 1]
        }
    }

    fun nodeValue(ref: Ref): Lit = nodeValue(ref.id) sign !ref.negated

    val outputValue = context("$name.outputValue") {
        newBoolVarArray(Y) { (y) ->
            val output = output(y)
            nodeValue(output)
        }
    }

    /* Constraints */

    comment("AND gate semantics")
    for (g in 1..G) {
        val gate = andGate(g)
        iffAnd(
            andGateValue[g],
            nodeValue(gate.left),
            nodeValue(gate.right),
        )
    }
}

private fun Solver.encodeAigs(
    aigLeft: Aig,
    aigRight: Aig,
) {
    require(aigLeft.inputs.size == aigRight.inputs.size)
    require(aigLeft.outputs.size == aigRight.outputs.size)

    encodeAig(aigLeft, "left")
    encodeAig(aigRight, "right", reuse = "left")
    context["X"] = context["left.X"]
    context["Y"] = context["left.Y"]
    context["inputValue"] = context["left.inputValue"]
}

private fun Solver.encodeMiter() {
    /* Constants */

    val aigLeft: Aig = context["left.aig"]
    val aigRight: Aig = context["right.aig"]

    val Y: Int = context["Y"]

    /* Variables */

    val outputValueLeft: BoolVarArray = context["left.outputValue"]
    val outputValueRight: BoolVarArray = context["right.outputValue"]
    val xorValue = context("xorValue") {
        newBoolVarArray(Y)
    }

    /* Constraints */

    comment("Miter XORs")
    for (y in 1..Y) {
        iffXor2(
            xorValue[y],
            outputValueLeft[y],
            outputValueRight[y],
        )
    }

    comment("Miter OR")
    addClause((1..Y).map { y -> xorValue[y] })
    // addClause(xorValue.values)
}

private fun Solver.encodeOutputMergers(type: String) {
    require(type in listOf("EQ", "XOR"))

    /* Constants */

    val aigLeft: Aig = context["left.aig"]
    val aigRight: Aig = context["right.aig"]

    val Y: Int = context["Y"]

    /* Variables */

    val outputValueLeft: BoolVarArray = context["left.outputValue"]
    val outputValueRight: BoolVarArray = context["right.outputValue"]
    val mergerValue = context("mergerValue") {
        newBoolVarArray(Y)
    }

    /* Constraints */

    comment("Merge outputs using $type")
    for (y in 1..Y) {
        val merger = mergerValue[y]
        val left = outputValueLeft[y]
        val right = outputValueRight[y]
        when (type) {
            "EQ" -> iffIff(merger, left, right)
            "XOR" -> iffXor2(merger, left, right)
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

    val outputValueLeft: BoolVarArray = context["left.outputValue"]
    val outputValueRight: BoolVarArray = context["right.outputValue"]

    val Y: Int = context["Y"]

    // Freeze assumptions
    for (y in 1..Y) {
        maybeFreeze(outputValueLeft[y])
        maybeFreeze(outputValueRight[y])
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
    val param = "6_4"
    val filenameLeft = "data/instances/${left}/aag/${left}_${param}.aag"
    val filenameRight = "data/instances/${right}/aag/${right}_${param}.aag"

    val aigLeft = parseAig(filenameLeft)
    val aigRight = parseAig(filenameRight)
    val solverProvider = { MiniSatSolver() }
    // val solverProvider = { GlucoseSolver() }
    // Methods: "miter", "merge-eq", "merge-xor", "conj"
    val method = "merge-xor"

    checkEquivalence(aigLeft, aigRight, solverProvider, method)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
