@file:Suppress("LocalVariableName", "DuplicatedCode")

package com.github.lipen.satlib.nexus.atpg

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.AigAndGate
import com.github.lipen.satlib.nexus.aig.AigInput
import com.github.lipen.satlib.nexus.aig.Ref
import com.github.lipen.satlib.nexus.aig.parseAig
import com.github.lipen.satlib.nexus.aig.shadow
import com.github.lipen.satlib.nexus.eqgates.`check gates equivalence using conjugated table`
import com.github.lipen.satlib.nexus.utils.declare
import com.github.lipen.satlib.nexus.utils.maybeFreeze
import com.github.lipen.satlib.nexus.utils.maybeMelt
import com.github.lipen.satlib.nexus.utils.secondsSince
import com.github.lipen.satlib.nexus.utils.timeNow
import com.github.lipen.satlib.op.iffAnd
import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.useWith
import mu.KotlinLogging
import kotlin.math.absoluteValue

private val logger = KotlinLogging.logger {}

private fun Solver.encodeAigATPG(
    aig: Aig,
    name: String,
    reuse: String? = null,
    brokenGateId: Int = 0,
) {
    if (reuse != null) {
        require(brokenGateId != 0)
    } else {
        require(brokenGateId == 0)
    }

    /* Constants */

    context["$name.aig"] = aig
    val X = context("$name.X") { aig.inputs.size }
    val Y = context("$name.Y") { aig.outputs.size }
    val G = context("$name.G") { aig.andGates.size }
    logger.info("$name: X = $Y, Y = $Y, G = $G")

    fun inputByIndex(x: Int): AigInput = aig.inputs[x - 1]
    fun outputByIndex(y: Int): Ref = aig.outputs[y - 1]
    fun andGateByIndex(g: Int): AigAndGate = aig.andGates[g - 1] // FIXME: order?

    /* Variables */

    val inputValue = context("$name.inputValue") {
        if (reuse == null) {
            newBoolVarArray(X)
        } else {
            context["$reuse.inputValue"]
        }
    }
    val andGateValue = context("$name.andGateValue") {
        if (reuse == null) {
            newBoolVarArray(G)
        } else {
            val shadow = context("shadow") { aig.shadow(brokenGateId.absoluteValue) }
            println("shadow of $brokenGateId (size=${shadow.size}): $shadow")
            val andGateValueReuse: BoolVarArray = context["$reuse.andGateValue"]
            newBoolVarArray(G) { (g) ->
                val gate = andGateByIndex(g)
                if (gate.id in shadow) {
                    newLiteral()
                } else {
                    andGateValueReuse[g]
                }
            }
        }
    }

    fun nodeValueById(id: Int): Lit {
        return when (val node = aig.node(id)) {
            is AigInput -> inputValue[aig.inputs.indexOf(node) + 1]
            is AigAndGate -> andGateValue[aig.andGates.indexOf(node) + 1]
        }
    }

    fun nodeValueByRef(ref: Ref): Lit = nodeValueById(ref.id) sign !ref.negated

    val outputValue = context("$name.outputValue") {
        newBoolVarArray(Y) { (y) ->
            val output = outputByIndex(y)
            nodeValueByRef(output)
        }
    }

    /* Constraints */

    if (reuse == null) {
        comment("AND gate semantics")
        for (g in 1..G) {
            val gate = andGateByIndex(g)
            iffAnd(
                andGateValue[g],
                nodeValueByRef(gate.left),
                nodeValueByRef(gate.right),
            )
        }
    } else {
        val shadow: List<Int> = context["shadow"]

        comment("AND gate semantics for shadow (excluding broken gate)")
        for (id in shadow - brokenGateId.absoluteValue) {
            val gate = aig.andGate(id)
            iffAnd(
                nodeValueById(id),
                nodeValueByRef(gate.left),
                nodeValueByRef(gate.right),
            )
        }

        comment("Broken gate value")
        addClause(nodeValueById(brokenGateId.absoluteValue) sign (brokenGateId > 0))
    }
}

private fun Solver.encodeStuckAtFault(
    aig: Aig,
    brokenGateId: Int,
) {
    encodeAigATPG(aig, name = "original")
    encodeAigATPG(aig, name = "faulty", reuse = "original", brokenGateId = brokenGateId)
    context["X"] = context["original.X"]
    context["Y"] = context["original.Y"]
    context["inputValue"] = context["original.inputValue"]
}

private fun atpg(aig: Aig) {
    logger.info("Performing ATPG for $aig...")

    MiniSatSolver().useWith {
        // val brokenGateId = aig.layers[1][0]
        val brokenGateId = 30 // 25

        declare(logger) {
            encodeStuckAtFault(aig, brokenGateId)
        }

        val Y: Int = context["Y"]
        val outputValueLeft: BoolVarArray = context["original.outputValue"]
        val outputValueRight: BoolVarArray = context["faulty.outputValue"]

        // Freeze assumptions
        for (y in 1..Y) {
            maybeFreeze(outputValueLeft[y])
            maybeFreeze(outputValueRight[y])
        }

        for (y in 1..Y) {
            val left = outputValueLeft[y]
            val right = outputValueRight[y]

            logger.info("Calculating conjugated table for y = $y...")
            val timeStartConj = timeNow()
            // var ok = true
            // for ((s1, s2) in listOf(
            //     false to false,
            //     false to true,
            //     true to false,
            //     true to true,
            // )) {
            //     val (res, timeSolveSub) = measureTimeWithResult {
            //         solve(left sign s1, right sign s2)
            //     }
            //     logger.debug {
            //         "y=$y, solve(${s1.toInt()}${s2.toInt()})=${res.toInt()} in %.3fs".format(timeSolveSub.seconds)
            //     }
            //     if (res != (s1 == s2)) {
            //         logger.warn {
            //             "Circuits are NOT equivalent in output y=$y: solve(${s1.toInt()}${s2.toInt()}) = ${res.toInt()}"
            //         }
            //         ok = false
            //         // return false
            //     }
            // }
            val ok = `check gates equivalence using conjugated table`(left, right)
            if (ok) {
                logger.info {
                    "Circuits are equivalent in output y=$y. Done in %.3fs".format(secondsSince(timeStartConj))
                }
            } else {
                logger.info {
                    "Circuits are NOT equivalent in output y=$y. Done in %.3fs".format(secondsSince(timeStartConj))
                }
            }

            // Un-freeze assumptions
            maybeMelt(left)
            maybeMelt(right)
        }
    }
}

fun main() {
    val timeStart = timeNow()

    // val filename = "data/instances/manual/aag/eq.aag"
    // val filename = "data/instances/manual/aag/eq2.aag"
    // val filename = "data/instances/ISCAS/aag/c17.aag"
    // val filename = "data/instances/ISCAS/aag/c432.aag"
    // val filename = "data/instances/BubbleSort/aag/BubbleSort_3_3.aag"
    // val filename = "data/instances/BubbleSort/aag/BubbleSort_4_3.aag"
    val filename = "data/instances/BubbleSort/aag/BubbleSort_7_4.aag"

    val aig = parseAig(filename)
    logger.info { aig }

    atpg(aig)

    logger.info("All done in %.3f s".format(secondsSince(timeStart)))
}
