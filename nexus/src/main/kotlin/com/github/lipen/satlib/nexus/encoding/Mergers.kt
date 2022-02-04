@file:Suppress("LocalVariableName")

package com.github.lipen.satlib.nexus.encoding

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.newBoolVarArray
import com.github.lipen.satlib.nexus.aig.Aig
import com.github.lipen.satlib.nexus.utils.iffXor2
import com.github.lipen.satlib.op.iffIff
import com.github.lipen.satlib.solver.Solver
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal fun Solver.encodeOutputMergers(type: String) {
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
