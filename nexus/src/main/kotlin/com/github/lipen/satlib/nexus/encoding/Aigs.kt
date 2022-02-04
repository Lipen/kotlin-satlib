@file:Suppress("DuplicatedCode", "LocalVariableName")

package com.github.lipen.satlib.nexus.encoding

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.aig.AigAndGate
import com.github.lipen.satlib.nexus.aig.AigInput
import com.github.lipen.satlib.nexus.aig.Ref
import com.github.lipen.satlib.op.iffAnd
import com.github.lipen.satlib.solver.Solver
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal fun Solver.encodeAig1(aig: Aig) {
    /* Constants */

    context["aig"] = aig
    val X = context("X") { aig.inputs.size }
    val Y = context("Y") { aig.outputs.size }
    val G = context("G") { aig.andGates.size }
    logger.info("X = $Y, Y = $Y, G = $G")

    fun inputByIndex(x: Int): AigInput = aig.inputs[x - 1]
    fun outputByIndex(y: Int): Ref = aig.outputs[y - 1]
    fun andGateByIndex(g: Int): AigAndGate = aig.andGates[g - 1] // FIXME: order?

    /* Variables */

    val inputValue = context("inputValue") {
        newBoolVarArray(X)
    }
    val andGateValue = context("andGateValue") {
        newBoolVarArray(G)
    }

    fun nodeValueById(id: Int): Lit {
        return when (val node = aig.node(id)) {
            is AigInput -> inputValue[aig.inputs.indexOf(node) + 1]
            is AigAndGate -> andGateValue[aig.andGates.indexOf(node) + 1]
        }
    }

    fun nodeValueByRef(ref: Ref): Lit = nodeValueById(ref.id) sign !ref.negated

    val outputValue = context("outputValue") {
        newBoolVarArray(Y) { (y) ->
            val output = outputByIndex(y)
            nodeValueByRef(output)
        }
    }

    /* Constraints */

    comment("AND gate semantics")
    for (g in 1..G) {
        val gate = andGateByIndex(g)
        iffAnd(
            andGateValue[g],
            nodeValueByRef(gate.left),
            nodeValueByRef(gate.right),
        )
    }
}

internal fun Solver.encodeAig2(
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
        return when (val node = aig.node(id)) {
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

internal fun Solver.encodeAigs(
    aigLeft: Aig,
    aigRight: Aig,
) {
    require(aigLeft.inputs.size == aigRight.inputs.size)
    require(aigLeft.outputs.size == aigRight.outputs.size)

    encodeAig2(aigLeft, "left")
    encodeAig2(aigRight, "right", reuse = "left")
    context["X"] = context["left.X"]
    context["Y"] = context["left.Y"]
    context["inputValue"] = context["left.inputValue"]
}
